# SIE Definition Sourcer

The **Definition Sourcer** is a SIE component that sources GSM (Generative System Model) definitions from external artifacts — code repositories, published standards, regulatory texts, or any other definitional artifact repository.

External artifacts are **definitions in their native form**. Code defines structure and behavior. Legislation defines principles, constraints, and obligations. Standards define quality requirements and compliance boundaries. The Definition Sourcer translates these native definitions into GSM primitives, closing the gap between the world's definitions and SIE's generative model.

## Design Directives

### "Source artifacts ARE definitions"

The Definition Sourcer does not *interpret* or *describe* external artifacts — it **translates** them. A Java service class IS a Structure with Mechanisms. An ISO norm article IS a Directive. A GDPR obligation IS a Norm. The sourcer performs a faithful structural translation from the source's native definitional language into GSM's universal definitional language.

This is distinct from Description-plane observation (DSM). DSM observes runtime state and behavior. The Definition Sourcer operates entirely within the Definition plane: it takes definitional artifacts as input and produces definitional primitives as output.

### Generic core, domain-specific source adapters

The Definition Sourcer is **source-agnostic at its core**. The pipeline (inventory → extract → verify → merge → validate → output) is universal. What varies is the **Source Adapter** — a domain-specific module that:

- Connects to a specific artifact repository type (git, legislation database, standards registry, artifact repository)
- Classifies artifacts within that source
- Maps source patterns to GSM primitive types
- Provides deterministic parsers for structured artifacts
- Provides LLM prompt contracts for unstructured/semi-structured artifacts

Source Adapters are pluggable. The generic pipeline orchestrates them.

### Domain presets

Source Adapters are organized into **domain presets** — curated collections of adapters for a specific domain. A domain preset bundles:

- Adapter implementations for relevant source types
- Class-to-GSM classification tables specific to the domain
- Technical Archetype schemas relevant to the domain
- LLM prompt specializations for the domain

Examples:

- **IT domain preset**: Git Repository Adapter (code → GSM primitives), Helm Chart Adapter, OpenAPI Adapter, etc.
- **Legal/Regulatory domain preset**: Legislation Adapter (legal texts → GSM primitives), Standards Adapter (ISO/IEC norms → DNA)
- Future presets: Financial, Healthcare, Industrial, etc.

### SaaS multi-tenant model

Legal/regulatory sources (GDPR, NIS2, ISO 27001, etc.) produce GSM definitions for **Systems representing legal entities** — the regulation itself modeled as a System with Purpose, Directives, and Norms. These definitions live in **SaaS SIE instances** and are consumed by tenants.

A tenant's Systems are **governed subjects** of these legal entity Systems. The governance relationship is:

```
LegalEntitySystem (GDPR)  ──governs──▶  TenantSystem (CustomerApp)
  (Governance expressed via Directives targeting tenant Structure purposes)
```

The tenant's SIE instance sources the legal entity System's DNA through governance Directives. The Definition Sourcer populates the upstream legal entity Systems; governance processes propagate constraints downstream.

## Architecture

### Generic Pipeline (source-agnostic)

```
┌─────────────────────────────────────────────────────────┐
│  DEFINITION SOURCER — Generic Pipeline                  │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐  │
│  │ INVENTORY│→ │ EXTRACT  │→ │ VERIFY   │→ │ MERGE  │  │
│  │          │  │          │  │          │  │        │  │
│  │ Classify │  │ Parse +  │  │ LLM self-│  │ Dedup  │  │
│  │ artifacts│  │ LLM      │  │ coherence│  │ Resolve│  │
│  └──────────┘  └──────────┘  └──────────┘  └────┬───┘  │
│                                                  │      │
│  ┌──────────┐  ┌──────────┐                      │      │
│  │ OUTPUT   │← │ VALIDATE │←─────────────────────┘      │
│  │          │  │          │                              │
│  │ GSM JSON │  │ GSM valid│                              │
│  │ + prove- │  │ + schema │                              │
│  │   nance  │  │   checks │                              │
│  └──────────┘  └──────────┘                              │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │  SOURCE ADAPTER (pluggable, domain-specific)    │    │
│  │  ┌──────────┐ ┌──────────┐ ┌─────────────────┐ │    │
│  │  │ Connector│ │Classifier│ │ GSM Mapper      │ │    │
│  │  │ (fetch)  │ │ (types)  │ │ (pattern→prim.) │ │    │
│  │  └──────────┘ └──────────┘ └─────────────────┘ │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### Pipeline Phases

#### Phase 1: INVENTORY (deterministic + adapter-specific)

The adapter connects to the source and produces a **classified artifact manifest**.

| Responsibility | Owner |
|---|---|
| Connect to source (clone, fetch, download) | Source Adapter |
| Walk artifact tree | Source Adapter |
| Classify each artifact by structural role | Source Adapter (domain-specific classification table) |
| Build structural skeleton (Structure hierarchy, etc.) | Generic pipeline (from adapter classification) |

Output: classified artifact manifest + structural skeleton.

#### Phase 2: EXTRACT (deterministic parsers + LLM)

Two extraction modes, applied per artifact:

**Deterministic extraction** (parsers — preferred when available):

- Structured artifacts with known schemas (OpenAPI, protobuf, SQL DDL, JSON Schema, AsyncAPI, legal article databases with structured fields)
- Source Adapter provides the parser
- Output tagged with `extractionMethod: "parser-deterministic"`

**LLM extraction** (when no deterministic parser exists):

- Semi-structured or unstructured artifacts (source code, legal prose, standard narrative text)
- Generic pipeline manages the LLM call with structured JSON output
- Source Adapter provides the **prompt specialization** (classification table, naming rules, domain-specific extraction instructions)
- Output tagged with `extractionMethod: "llm"`

LLM calls use `temperature: 0` and structured JSON output mode.

#### Phase 3: VERIFY (LLM self-coherence loop)

For LLM-extracted primitives only. The pipeline feeds extracted primitives back to the LLM alongside the original source, prompting verification:

- Does each extracted primitive faithfully represent the source artifact?
- Are cross-references consistent (names resolve, archetypes exist)?
- Are definitions accurately captured?

Maximum 3 verification iterations. On convergence, primitives are marked `coherenceVerified: true`.

#### Phase 4: MERGE + DEDUPLICATE (deterministic)

Combines deterministic and LLM extractions:

- Parser-derived primitives win for schemas (higher fidelity)
- LLM-derived primitives win for behavioral logic (not parseable)
- Name normalization (adapter-specific naming conventions)
- Duplicate detection (exact match → merge; near-duplicate → finding)
- Cross-reference resolution (dangling references → ERROR finding)
- Structural assembly (assign Mechanisms to Structures, derive Interfaces, etc.)

#### Phase 5: SUPERSESSION DETECTION (deterministic, incremental only)

When prior GSM definitions exist for the same System:

- Match extracted primitives to existing by deterministic ID, provenance path, or name
- Diff content (schema, rule, definition)
- Changed → mark new as `supersedes` existing
- Unchanged → skip
- Removed from source → finding: "source removed"

#### Phase 6: VALIDATE (deterministic)

GSM consistency rules:

- Every Mechanism has ≥ 1 Receptor and ≥ 1 Effector
- Every Receptor/Effector bound to exactly 1 Archetype
- Every Interaction has compatible effector/receptor Archetypes
- Every Mechanism rule begins with `on("ArchetypeName")`
- Every `on()` argument resolves to a defined Archetype
- Name uniqueness per primitive type
- Name length ≤ 80 chars
- Schema.uri uniqueness
- Structure definitions: no circular references
- Schemas are valid JSON Schemas
- Ascription definitions conform to Archetype schemas
- Provenance completeness (every primitive has provenance)

Output: findings array (ERROR, WARNING, INFO).

#### Phase 7: OUTPUT

A single JSON document conforming to the Definition Sourcer output schema (see Output Schema section), ready for persistence via the Definition Manager API.

All primitives carry `definitionStatus: DRAFT`. Nothing is activated without governance review.

## Source Adapter Interface

Every Source Adapter implements the following contract:

```
SourceAdapter
├── getSourceType(): string
│   Returns a unique identifier for this source type
│   (e.g., "git-repository", "legislation-database", "iso-standards")
│
├── connect(config: SourceConfig): Connection
│   Establishes connection to the artifact source
│   (clone, authenticate, download, etc.)
│
├── inventory(connection: Connection): ArtifactManifest
│   Walks the artifact tree and classifies each artifact
│   Returns: classified manifest with paths + structural roles
│
├── getClassificationTable(): ClassificationTable
│   Returns the domain-specific mapping from artifact patterns
│   to GSM primitive types
│
├── getDeterministicParsers(): Parser[]
│   Returns available parsers for structured artifacts
│   (OpenAPI, protobuf, SQL DDL, etc.)
│
├── getPromptSpecialization(): PromptSpec
│   Returns domain-specific LLM prompt additions:
│   - Classification rules
│   - Naming conventions
│   - Extraction instructions
│   - Archetype schemas
│
├── getArchetypeSchemas(): Schema[]
│   Returns Archetype schemas relevant
│   to this source type
│
└── getNamingConventions(): NamingRules
    Returns naming rules for extracted primitives
    (suffixes, casing, length constraints)
```

## Provenance Model

Every extracted primitive carries a `provenance` object, regardless of source type:

```json
{
  "provenance": {
    "sourceType": "git-repository | legislation-database | iso-standard | ...",
    "sourceRef": {
      "uri": "https://github.com/org/repo.git | https://eur-lex.europa.eu/... | ...",
      "version": "abc123def | 2024-04-15 | ISO/IEC 27001:2022",
      "accessTimestamp": "2026-02-22T10:30:00Z"
    },
    "artifacts": [
      {
        "path": "src/main/java/com/example/OrderService.java | Article 5(1)(a) | Section 6.1.2",
        "section": { "start": 45, "end": 78 },
        "identifier": "processOrder | Art.5.1.a | 6.1.2",
        "language": "java | fr | en"
      }
    ],
    "extractionMethod": "llm | parser-deterministic | hybrid",
    "extractionModel": "claude-opus-4-2025-05-14",
    "extractionTimestamp": "2026-02-22T10:30:00Z",
    "schemaSourceType": "openapi | protobuf | sql-ddl | jpa-entity | legal-article | iso-clause | llm-inferred",
    "confidence": 0.92,
    "coherenceVerified": true,
    "coherenceIterations": 2
  }
}
```

The `sourceType` and `schemaSourceType` fields enable filtering and trust differentiation: parser-derived schemas from an OpenAPI spec carry higher structural fidelity than LLM-inferred schemas from source code — which in turn carry higher fidelity than LLM-inferred definitions from legal prose.

## Target GSM Primitives by Source Domain

### IT Domain (Git Repository Adapter)

Source: code repositories.

> **⚠️ STALE SECTION.** The layer-based organization (Infrastructure/Organization) and several primitives listed below (Component, Channel, EventArchetype, StateObjectArchetype, Qualifier, Controller, Actuator, Sensor, Actuation, Sensing) are eliminated in current GSM. Current GSM uses: Structure (not Component), Archetype (single kind, replacing EventArchetype + StateObjectArchetype), Mechanism with auto-derived Effectors/Receptors (replacing Controller/Actuator/Sensor), DNA (replacing DPR). This section needs conceptual redesign.

**Infrastructure layer:**

- ~~Component~~ → Structure (composite: repo → modules → packages)
- Mechanism, Receptor, Effector
- Interface, Interaction (Channel eliminated — expressed as Norms on Interactions)
- Archetype (single kind, with JSON Schema)

**Organization layer:**

- ~~Rule~~ → Mechanism rule (Starlark translated from method logic — priority target)
- Function (from public methods/operations)

**Auto-derived by Definition Manager (NOT by Sourcer):**

- Effectors, Receptors (auto-derived from Mechanism rules by Definition Manager)

**Technical Archetype schemas extracted from code:**

- ErrorHandling, RetryPolicy, CachingPolicy, LoggingPolicy
- TimeoutPolicy, CircuitBreaker, RateLimiting
- TransactionScope, SecurityContext, Observability
  (expressed as domain Archetype schemas)

**Class-to-GSM classification table (IT/Git):**

| Code pattern | Discriminant | GSM primitive |
|---|---|---|
| Service/handler class | Methods that receive input AND mutate state or produce events | methods → **Mechanism rules** |
| Controller/endpoint class | Annotated endpoints (@RestController, @GrpcService, etc.) | endpoints → **Receptors** + methods → **Mechanism rules** |
| Entity/model class | Persistent fields (@Entity, ORM model, Prisma model) | **Archetype** + fields → JSON Schema |
| DTO/event/message class | Transferable payload, no business logic | **Archetype** + fields → JSON Schema |
| Repository/DAO class | CRUD access to entities | Methods → **Functions** (no event trigger, not Mechanism rules) |
| Utility/helper class | Stateless, no I/O, no state mutation | Methods → **Functions** |
| Configuration class | Application config, properties, feature flags | Properties → **Ascription** definitions on owning Structure |
| Client/adapter class | Calls external services (HTTP client, SDK wrapper) | **Effectors** + **Interactions** (outbound) |
| Listener/consumer class | Consumes events/messages (Kafka, RabbitMQ, SQS) | **Receptors** + methods → **Mechanism rules** |

**Method-to-Rule filter** (only behaviorally meaningful methods):

- Receives an event/request/message (trigger)
- Mutates state OR produces an event/response (effect)
- Skip: getters, setters, toString, equals, hashCode, builders, pure formatters

**Starlark translation contract:**

- Method input (HTTP request, event payload) → `event = on("ArchetypeName")`
- DB/cache reads → `var = sys.acquire("ArchetypeName", filter)`
- DB writes → `sys.create/modify/delete("ArchetypeName", data)`
- Downstream calls, message publishing → `sys.emit("ArchetypeName", data)`
- Assigned return values (code checks results) → closed-loop: `result = sys.create(...)`
- DO NOT translate: logging, metrics, error handling, retries, caching, transaction management → these go into Archetype-typed Ascriptions
- Available globals: `on`, `sys`, `now`, `uuid7`, `fullmatch`, `search`

### Legal/Regulatory Domain (Legislation Adapter)

Source: published legislation and regulatory texts (GDPR, NIS2, DORA, etc.).

The sourced System represents the **legal entity itself** (the regulation as a System). Its DNA encodes the regulatory requirements that governed Systems must satisfy.

> **⚠️ STALE**: Constitution/Organization layers are eliminated in current GSM. Concepts map to: Purpose → Structure.purpose, Directive → Directive, Policy → Norm, Rule → Mechanism rule, Operationalization → eliminated.

**Former Constitution layer:**

- Purpose (the regulation's objectives — e.g., "Protection of personal data", "Cybersecurity resilience")
- Directive (normative requirements — e.g., "System MUST ENSURE lawfulness OF personal-data-processing")

**Organization layer:**

- Norm (measurable obligations derived from articles — e.g., data breach notification within 72 hours)
- Mechanism rule (operational procedures mandated by articles — e.g., data subject access request handling)
- Function (capabilities the regulation requires — e.g., "Data breach notification", "Impact assessment")
- DNA chain (Directive → Norm → Mechanism rule chains tracing regulatory lineage)

**SaaS deployment model:**

- Regulatory Systems are defined once in SaaS SIE instances
- Tenant Systems are governed by regulatory Systems via Directives targeting tenant Structure purposes
- Governance processes propagate regulatory DNA as constraints on tenant Systems
- Tenant compliance is evaluated by the Supervision plane against regulatory DNA

### Standards Domain (Standards Adapter)

Source: published standards and norms (ISO 27001, ISO 9001, NIST CSF, CIS Benchmarks, etc.).

Similar to legal/regulatory, but with:

- Purpose (the standard's objectives)
- Directive (normative requirements from SHALL/SHOULD clauses)
- Norm (measurable controls — e.g., "Access control review frequency ≤ 90 days")
- Function (control activities — e.g., "Access review", "Vulnerability scanning")
- DNA chain (requirement → control → procedure chains)

Standards Systems govern tenant Systems via Directives (same pattern as Legal/Regulatory governance).

## Idempotency & Incremental Sync

| Concern | Design |
|---|---|
| **Deterministic ID** | `UUID5(systemId, primitiveType, primitiveName)` — survives re-extraction if name is stable |
| **Idempotency** | Same source + same version → same output. LLM calls: `temperature: 0`, `seed: 42`. Parser phases: deterministic |
| **Incremental** | Only re-extract changed artifacts (git diff, document version diff). Unchanged artifacts: skip. Changed: full re-extract → supersession detection |
| **Rename detection** | Name disappears + new name appears in same artifact scope with similar structure → candidate rename finding (not auto-resolved) |
| **Deletion detection** | Existing primitive's provenance artifact no longer exists in source → finding: "source removed" |

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| **Translation fidelity** | LLM self-verification loop (Phase 3): verify each primitive against source, max 3 iterations |
| **Hallucinated primitives** | Phase 6 validates: every Archetype referenced in Mechanism rules MUST be defined in output |
| **Naming drift across runs** | Deterministic naming rules per adapter + deterministic IDs (UUID5) |
| **Confidence visibility** | Every LLM-extracted primitive has `confidence` score + `coherenceVerified` flag |
| **Human trust boundary** | All output has `definitionStatus: DRAFT` — nothing is ACTIVE without governance review |
| **Traceability** | Every primitive links back to exact artifact, section, and identifier via provenance |
| **Source fidelity differentiation** | `extractionMethod` + `schemaSourceType` enable reviewers to filter by trust level |
| **Cross-domain coherence** | Generic pipeline enforces GSM consistency rules regardless of source domain |

## GSM Prerequisites

Two GSM changes are prerequisites for the Definition Sourcer:

### 1. Structure self-containment (STALE)

```plantuml
Structure "1" *--> "0..*" Structure : contains
```

Structural hierarchy within Structures (repo → module → package). This is first-order structural containment — not governance composition (which remains a System-level decision). Contained sub-Structures inherit their parent Structure's governance scope.

### 2. Definition supersession

```plantuml
Ascription "0..1" o--> "0..1" Ascription : supersedes
```

Optional reference from a new definition to the one it replaces. Constraints:

- Same Ascription subtype (Mechanism can only supersede Mechanism)
- Same owning System
- No circular chains
- When superseding primitive reaches ACTIVE, superseded automatically transitions to DEPRECATED

This handles incremental sync (code changed → new DRAFT supersedes existing ACTIVE) and applies universally beyond the Sourcer (any definition evolution scenario).

## Technical Archetype Schemas (IT Domain Preset)

Cross-cutting technical concerns extracted from code annotations, configuration, and framework conventions (expressed as domain Archetype schemas extending GSM base archetypes via allOf):

| Archetype Schema | Applicable to | Schema captures |
|---|---|---|
| `ErrorHandling` | Mechanism | strategy, maxRetries, backoffPolicy, fallbackAction |
| `RetryPolicy` | Mechanism, Interaction | maxAttempts, backoffType, initialDelay, maxDelay |
| `CachingPolicy` | Mechanism | strategy, ttl, evictionPolicy, keyPattern |
| `LoggingPolicy` | Mechanism | level, structured, sensitiveFields, auditRequired |
| `TimeoutPolicy` | Interaction, Mechanism | connectTimeout, readTimeout, writeTimeout |
| `CircuitBreaker` | Interaction | failureThreshold, resetTimeout, halfOpenRequests |
| `RateLimiting` | Receptor, Interface | requestsPerSecond, burstSize, strategy |
| `TransactionScope` | Mechanism | isolation, propagation, readOnly |
| `SecurityContext` | Interface, Receptor | authRequired, authMechanism, encryption, mTLS |
| `Observability` | Mechanism | tracingEnabled, metricsEnabled, spanName, customAttributes |

## UI Integration

The Definition Sourcer is triggerable from the ITIP UI (and future domain app UIs):

| UI Element | Behavior |
|---|---|
| **Trigger location** | System detail view → "Source Definitions" action |
| **Source selection** | Choose source type (domain preset) → adapter-specific form |
| **Configuration** | Adapter-specific: repo URL + branch (git), document reference (legislation), etc. |
| **Progress** | Real-time phase progress (7 phases), artifact count, extraction count |
| **Review** | Definition diff view: new primitives, supersessions, findings |
| **Accept/reject** | Per-primitive or bulk: accept → persisted as DRAFT in Definition Manager |
| **Findings panel** | Filterable by severity (ERROR/WARNING/INFO), primitive type, phase |
| **Re-run** | Idempotent: same source + same version → same output (or "no changes") |

## Repository Structure

```
sie/sie-definition-sourcer/
├── README.md                  # This file
├── LICENSE
└── definition/                # Design-time artifacts
    ├── architecture/          # Architecture and sequence diagrams
    ├── adapters/              # Source adapter specifications
    │   ├── git-repository/    # IT domain: Git Repository Adapter
    │   ├── legislation/       # Legal domain: Legislation Adapter
    │   └── standards/         # Standards domain: Standards Adapter
    ├── schemas/               # JSON schemas
    │   ├── output/            # Output document schema
    │   ├── provenance/        # Provenance model schema
    │   └── qualifiers/        # Technical Archetype schemas
    └── pipeline/              # Generic pipeline specifications
```
