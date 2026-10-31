# GSM Sourcer

The **GSM Sourcer** is a SIE component that sources GSM (Generative System Model) definitions from external artifacts — code repositories, published standards, regulatory texts, or any other definitional artifact repository.

External artifacts are **definitions in their native form**. Code defines structure and behavior. Legislation defines principles, constraints, and obligations. Standards define quality requirements and compliance boundaries. The GSM Sourcer translates these native definitions into GSM primitives, closing the gap between the world's definitions and SIE's generative model.

## Design Directives

### "Source artifacts ARE definitions"

The GSM Sourcer *translates* external artifacts. Java service class IS a Component with Mechanisms. An ISO norm article IS a Directive. A GDPR obligation IS a Policy. The sourcer performs a faithful structural translation from the source's native definitional language into GSM's universal definitional language.

### Generic core, domain-specific source adapters

The GSM Sourcer is **source-agnostic at its core**. The pipeline (inventory → extract → verify → merge → validate → output) is universal. What varies is the **Source Adapter** — a domain-specific module that:

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
- Technical qualifier archetype schemas relevant to the domain
- LLM prompt specializations for the domain

Examples:

- **IT domain preset**: Git Repository Adapter (code → Infrastructure + Organization), Helm Chart Adapter, OpenAPI Adapter, etc.
- **Legal/Regulatory domain preset**: Legislation Adapter (legal texts → Constitution + Organization), Standards Adapter (ISO/IEC norms → DPR)
- Future presets: Financial, Healthcare, Industrial, etc.

### SaaS multi-tenant model

Legal/regulatory sources (GDPR, NIS2, ISO 27001, etc.) produce GSM definitions for **Systems representing legal entities** — the regulation itself modeled as a System with Purpose, Directives, Policies, and Rules. These definitions live in **SaaS SIE instances** and are consumed by tenants.

A tenant's Systems are **governed subjects** of these legal entity Systems. The governance relationship is:

```text
LegalEntitySystem (GDPR)  ←→  TenantSystem (CustomerApp)
  Role: LEGISLATOR              Role: GOVERNED_SUBJECT
  Relationship: governance mandate
```

The tenant's SIE instance sources the legal entity System's DPR through Relationship-based governance inheritance. The GSM Sourcer populates the upstream legal entity Systems; governance processes propagate constraints downstream.

## Architecture

### Generic Pipeline (source-agnostic)

```text
┌─────────────────────────────────────────────────────────┐
│  GSM SOURCER — Generic Pipeline                         │
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
│  │ GSM JSON │  │ GSM rules│                              │
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
| --- | --- |
| Connect to source (clone, fetch, download) | Source Adapter |
| Walk artifact tree | Source Adapter |
| Classify each artifact by structural role | Source Adapter (domain-specific classification table) |
| Build structural skeleton (Component hierarchy, etc.) | Generic pipeline (from adapter classification) |

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
- Are qualifiers accurately captured?

Maximum 3 verification iterations. On convergence, primitives are marked `coherenceVerified: true`.

#### Phase 4: MERGE + DEDUPLICATE (deterministic)

Combines deterministic and LLM extractions:

- Parser-derived primitives win for schemas (higher fidelity)
- LLM-derived primitives win for behavioral logic (not parseable)
- Name normalization (adapter-specific naming conventions)
- Duplicate detection (exact match → merge; near-duplicate → finding)
- Cross-reference resolution (dangling references → ERROR finding)
- Structural assembly (assign Mechanisms to Components, derive Interfaces, etc.)

#### Phase 5: SUPERSESSION DETECTION (deterministic, incremental only)

When prior GSM definitions exist for the same System:

- Match extracted primitives to existing by deterministic ID, provenance path, or name
- Diff content (schema, body, qualifiers)
- Changed → mark new as `supersedes` existing
- Unchanged → skip
- Removed from source → finding: "source removed"

#### Phase 6: VALIDATE (deterministic)

GSM consistency rules:

- Every Mechanism has ≥ 1 Receptor and ≥ 1 Effector
- Every Receptor/Effector bound to exactly 1 EventArchetype
- Every Interaction has compatible effector/receptor EventArchetypes
- Every Rule body begins with `on("EventArchetypeName")`
- Every `on()` argument resolves to a defined EventArchetype
- Name uniqueness per primitive type
- Name length ≤ 80 chars
- Schema.uri uniqueness
- Component containment: no circular parent-child
- Schemas are valid JSON Schemas
- Qualifier values conform to Qualifier Schemas
- Provenance completeness (every primitive has provenance)

Output: findings array (ERROR, WARNING, INFO).

#### Phase 7: OUTPUT

A single JSON document conforming to the GSM Sourcer output schema (see Output Schema section), ready for persistence via the Definition Manager API.

All primitives carry `definitionStatus: DRAFT`. Nothing is activated without governance review.

## Source Adapter Interface

Every Source Adapter implements the following contract:

```text
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
│   - Qualifier Schemas
│
├── getQualifierSchemas(): Schema[]
│   Returns Qualifier Schemas relevant
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

#### Infrastructure layer

- Component (composite: repo → modules → packages)
- Mechanism, Receptor, Effector
- Interface, Interaction, Channel
- EventArchetype + Schema
- StateObjectArchetype + Schema

#### Organization layer

- Rule (Starlark body translated from method logic — priority target)
- Function (from public methods/operations)

#### Auto-derived by Definition Manager (NOT by Sourcer)

- Controller, Actuator, Sensor (from Rule bodies)
- Actuation, Sensing

#### Technical qualifier archetypes extracted from code

- ErrorHandling, RetryPolicy, CachingPolicy, LoggingPolicy
- TimeoutPolicy, CircuitBreaker, RateLimiting
- TransactionScope, SecurityContext, Observability

#### Class-to-GSM classification table (IT/Git)

| Code pattern | Discriminant | GSM primitive |
| --- | --- | --- |
| Service/handler class | Methods that receive input AND mutate state or produce events | **sub-Component** + methods → **Rules** |
| Controller/endpoint class | Annotated endpoints (@RestController, @GrpcService, etc.) | **sub-Component** + endpoints → **Receptors** + methods → **Rules** |
| Entity/model class | Persistent fields (@Entity, ORM model, Prisma model) | **StateObjectArchetype** + fields → **Schema** |
| DTO/event/message class | Transferable payload, no business logic | **EventArchetype** + fields → **Schema** |
| Repository/DAO class | CRUD access to entities | Methods → **Functions** (no event trigger, not Rules) |
| Utility/helper class | Stateless, no I/O, no state mutation | Methods → **Functions** |
| Configuration class | Application config, properties, feature flags | Properties → **Qualifier** values on owning System |
| Client/adapter class | Calls external services (HTTP client, SDK wrapper) | **Effectors** + **Interactions** (outbound) |
| Listener/consumer class | Consumes events/messages (Kafka, RabbitMQ, SQS) | **Receptors** + methods → **Rules** |

**Method-to-Rule filter** (only behaviorally meaningful methods):

- Receives an event/request/message (trigger)
- Mutates state OR produces an event/response (effect)
- Skip: getters, setters, toString, equals, hashCode, builders, pure formatters

#### Starlark translation contract

- Method input (HTTP request, event payload) → `event = on("EventArchetypeName")`
- DB/cache reads → `var = sys.acquire("ArchetypeName", filter)`
- DB writes → `sys.create/modify/delete("ArchetypeName", data)`
- Downstream calls, message publishing → `sys.emit("EventArchetypeName", data)`
- Assigned return values (code checks results) → closed-loop: `result = sys.create(...)`
- DO NOT translate: logging, metrics, error handling, retries, caching, transaction management → these go into Qualifiers
- Available globals: `on`, `sys`, `now`, `uuid4`, `fullmatch`, `search`

### Legal/Regulatory Domain (Legislation Adapter)

Source: published legislation and regulatory texts (GDPR, NIS2, DORA, etc.).

The sourced System represents the **legal entity itself** (the regulation as a System). Its DPR encodes the regulatory requirements that governed Systems must satisfy.

#### Constitution layer

- Purpose (the regulation's objectives — e.g., "Protection of personal data", "Cybersecurity resilience")
- Directive (normative requirements — e.g., "System MUST ENSURE lawfulness OF personal-data-processing")

#### Organization layer

- Policy (measurable obligations derived from articles — e.g., data breach notification within 72 hours)
- Rule (operational procedures mandated by articles — e.g., data subject access request handling)
- Function (capabilities the regulation requires — e.g., "Data breach notification", "Impact assessment")
- Operationalization (Directive → Policy → Rule chains tracing regulatory lineage)

#### SaaS deployment model

- Regulatory Systems are defined once in SaaS SIE instances
- Tenant Systems establish Relationships (`GOVERNED_SUBJECT ↔ LEGISLATOR`) to regulatory Systems
- Governance processes propagate regulatory DPR as constraints on tenant Systems
- Tenant compliance is evaluated by the Supervision plane against regulatory DPR

### Standards Domain (Standards Adapter)

Source: published standards and norms (ISO 27001, ISO 9001, NIST CSF, CIS Benchmarks, etc.).

Similar to legal/regulatory, but with:

- Purpose (the standard's objectives)
- Directive (normative requirements from SHALL/SHOULD clauses)
- Policy (measurable controls — e.g., "Access control review frequency ≤ 90 days")
- Function (control activities — e.g., "Access review", "Vulnerability scanning")
- Operationalization (requirement → control → procedure chains)

Standards Systems use Relationship roles: `STANDARDIZATION_BODY ↔ GOVERNED_SUBJECT` or `CERTIFYING_AUTHORITY ↔ GOVERNED_SUBJECT`.

## Idempotency & Incremental Sync

| Concern | Design |
| --- | --- |
| **Deterministic ID** | `UUID5(systemId, primitiveType, primitiveName)` — survives re-extraction if name is stable |
| **Idempotency** | Same source + same version → same output. LLM calls: `temperature: 0`, `seed: 42`. Parser phases: deterministic |
| **Incremental** | Only re-extract changed artifacts (git diff, document version diff). Unchanged artifacts: skip. Changed: full re-extract → supersession detection |
| **Rename detection** | Name disappears + new name appears in same artifact scope with similar structure → candidate rename finding (not auto-resolved) |
| **Deletion detection** | Existing primitive's provenance artifact no longer exists in source → finding: "source removed" |

## Risk Mitigation

| Risk | Mitigation |
| --- | --- |
| **Translation fidelity** | LLM self-verification loop (Phase 3): verify each primitive against source, max 3 iterations |
| **Hallucinated primitives** | Phase 6 validates: every archetype referenced in Rules MUST be defined in output |
| **Naming drift across runs** | Deterministic naming rules per adapter + deterministic IDs (UUID5) |
| **Confidence visibility** | Every LLM-extracted primitive has `confidence` score + `coherenceVerified` flag |
| **Human trust boundary** | All output has `definitionStatus: DRAFT` — nothing is ACTIVE without governance review |
| **Traceability** | Every primitive links back to exact artifact, section, and identifier via provenance |
| **Source fidelity differentiation** | `extractionMethod` + `schemaSourceType` enable reviewers to filter by trust level |
| **Cross-domain coherence** | Generic pipeline enforces GSM consistency rules regardless of source domain |

## GSM Prerequisites

Two GSM changes are prerequisites for the GSM Sourcer:

### 1. Component self-containment

```plantuml
Component "1" *--> "0..*" Component : contains
```

Structural hierarchy within Components (repo → module → package). This is first-order structural containment — not governance composition (which remains a System-level decision). Contained sub-Components inherit their parent's System ownership and all four composition conditions.

### 2. Definition supersession

```plantuml
SystemicPrimitive "0..1" o--> "0..1" SystemicPrimitive : supersedes
```

Optional reference from a new definition to the one it replaces. Constraints:

- Same primitive type (Rule can only supersede Rule)
- Same owning System
- No circular chains
- When superseding primitive reaches ACTIVE, superseded automatically transitions to DEPRECATED

This handles incremental sync (code changed → new DRAFT supersedes existing ACTIVE) and applies universally beyond the Sourcer (any definition evolution scenario).

## Technical Qualifier Schemas (IT Domain Preset)

Cross-cutting technical concerns extracted from code annotations, configuration, and framework conventions:

| Qualifier Schema | Applicable to | Schema captures |
| --- | --- | --- |
| `ErrorHandling` | Rule, Function, Mechanism | strategy, maxRetries, backoffPolicy, fallbackAction |
| `RetryPolicy` | Rule, Mechanism, Interaction | maxAttempts, backoffType, initialDelay, maxDelay |
| `CachingPolicy` | Function, Mechanism | strategy, ttl, evictionPolicy, keyPattern |
| `LoggingPolicy` | Rule, Function, Mechanism | level, structured, sensitiveFields, auditRequired |
| `TimeoutPolicy` | Interaction, Channel, Mechanism | connectTimeout, readTimeout, writeTimeout |
| `CircuitBreaker` | Interaction, Channel | failureThreshold, resetTimeout, halfOpenRequests |
| `RateLimiting` | Receptor, Interface | requestsPerSecond, burstSize, strategy |
| `TransactionScope` | Rule, Function | isolation, propagation, readOnly |
| `SecurityContext` | Interface, Channel, Receptor | authRequired, authMechanism, encryption, mTLS |
| `Observability` | Mechanism, Function | tracingEnabled, metricsEnabled, spanName, customAttributes |

## UI Integration

The GSM Sourcer is triggerable from the OIP UI (and future domain app UIs):

| UI Element | Behavior |
| --- | --- |
| **Trigger location** | System detail view → "Source Definitions" action |
| **Source selection** | Choose source type (domain preset) → adapter-specific form |
| **Configuration** | Adapter-specific: repo URL + branch (git), document reference (legislation), etc. |
| **Progress** | Real-time phase progress (7 phases), artifact count, extraction count |
| **Review** | Definition diff view: new primitives, supersessions, findings |
| **Accept/reject** | Per-primitive or bulk: accept → persisted as DRAFT in Definition Manager |
| **Findings panel** | Filterable by severity (ERROR/WARNING/INFO), primitive type, phase |
| **Re-run** | Idempotent: same source + same version → same output (or "no changes") |

## Repository Structure

```text
gsm-sourcer/
├── README.md                  # This file
├── LICENSE
└── definition/                # Design-time artifacts
    ├── architecture/          # Component and sequence diagrams
    ├── adapters/              # Source adapter specifications
    │   ├── git-repository/    # IT domain: Git Repository Adapter
    │   ├── legislation/       # Legal domain: Legislation Adapter
    │   └── standards/         # Standards domain: Standards Adapter
    ├── schemas/               # JSON schemas
    │   ├── output/            # Output document schema
    │   ├── provenance/        # Provenance model schema
    │   └── qualifiers/        # Technical qualifier archetype schemas
    └── pipeline/              # Generic pipeline specifications
```
