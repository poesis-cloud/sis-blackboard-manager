# GSM Sourcer

The **GSM Sourcer** is a SIE component that translates external definitional artifacts (code repositories, regulatory texts, published standards, …) into GSM (Generative System Model) primitives so that they can enter the Definition Manager, become first-class governable Ascriptions, and participate in the SIE governance loop.

This README captures the **decisions converged during the design phase**. Sections marked **[PROPOSAL — pending confirmation]** are recommendations not yet committed to design.

---

## 1. Naming and scope

> **Software Behavior & Architecture Sourcing (SBAS)** — the first sourcing scenario delivered by GSM Sourcer.

SBAS sources the **dynamic** structure of a software system (Structures, Mechanisms, Effectors, Receptors, Interactions) from a code repository, leaving its **static** structure (the codebase itself) as Archetype content. This honours GSM §1: Structure is dynamic; Archetype is static; the codebase **defines** the software but **is not** the software.

SBAS is the first concrete *sourcing scenario* GSM Sourcer addresses. Other scenarios (regulatory-text sourcing, standards sourcing, OpenAPI sourcing) are within scope of the same engine.

### Use cases / usage profiles addressed

1. **Architecture discovery** — derive a TOGAF/C4-style Structure map from the actual code, not a stale documentation page.
2. **Compliance baselining** — produce the inventory of Mechanisms × data Archetypes that Norms then constrain.
3. **Drift detection** — re-source after code changes; surface deltas as governance-relevant findings.
4. **Definition seeding for greenfield governance** — bootstrap a DRAFT Ascription set when adopting GSM/ITIP on an existing codebase.
5. **Cross-system coupling analysis** — when multiple repos source into the same tenant, Interactions across Structures become first-class.
6. **Refactor impact analysis** *(future)* — given a Norm targeting a property, enumerate every Mechanism whose ports carry it.

### Market positioning

SBAS occupies the slot **static-source → governable-typed-definition with a closed-loop governance pipeline downstream**. Adjacent tools stop earlier:

| Adjacent tool | Stops at | SBAS adds |
|---|---|---|
| Backstage | Author-asserted YAML catalog | Code-as-source-of-truth + Ascription lifecycle |
| Sourcegraph | Code search & dep graph | Typed governance ontology (Mechanisms, Effectors, Norms) |
| Structurizr | Manual C4 DSL | Auto-derivation; governance, not diagrams |
| CAST / Sonargraph / Structure101 | Architecture metrics & rule violations | Materialised governable subjects, not just findings |
| ArchUnit / Spectral | Constraint execution | Produces the subject the constraints later govern |
| OpenTelemetry / eBPF service maps | Runtime topology (DSM) | Definition plane (GSM); complementary, not overlapping |

---

## 2. Foundational rules (committed)

These rules are immutable for the SBAS scenario and must be honoured by every plug-in.

1. **GSM models dynamic structure only.** No "Codebase Archetype" is created. Software behaviour = Structure + Mechanisms + Effectors + Receptors + Interactions + data Archetypes.
2. **Use only existing archetype catalogues.** Sourcing emits Ascriptions typed by pre-existing ITIP / sie-operator Archetypes (TOGAF, ISO 25000, application protocols, …). No language- or framework-specific archetypes are invented at sourcing time (no `JacksonDtoArchetype`, no `SpringRestEndpointArchetype`).
3. **Effectors and Receptors are user-typed.** They are emitted as DRAFT Ascriptions whose port type references an existing protocol Archetype (e.g. `HttpRequestEffector`). DefMan AST auto-derivation is **deferred** (see deferred ideas).
4. **Mechanism granularity is configurable** along two orthogonal axes (see §4).
5. **Interactions are emitted and proposed immediately**, even when the matching counterpart Mechanism does not yet exist (anticipatory governance via `Directive.purpose` is the GSM-native pattern for this).
6. **Out of scope** for sourcing: logging, metrics, tracing, configuration management, framework wiring. These are not modelled as GSM primitives.
7. **Application protocol archetypes live in ITIP**, not in sie-operator. Only Relay and Governance protocols remain operator-internal (see §6).

---

## 3. Source → GSM mapping (committed)

The mapping below is normative for the SBAS scenario. Detailed per-cell rules and edge cases will live in `def/code-to-gsm-mapping.md` (to be authored as the next step).

### Table B — code element → GSM primitive

| Code element                                    | GSM primitive            | Notes |
|---|---|---|
| Repository / module / deployable unit           | **Structure**            | One Structure per deployable unit; `purpose` derived from artifact name. |
| Class / service / function group (per profile)  | **Mechanism**            | Granularity per Axis 2 (§4). |
| Outbound call site (HTTP client, Kafka producer, gRPC client, JDBC, intra-process call, …) | **Effector** | Typed by a protocol Effector Archetype from `itip/def/frameworks/{family}/`. |
| Inbound endpoint (HTTP route, Kafka consumer, gRPC service method, message handler, …) | **Receptor** | Typed by a protocol Receptor Archetype. |
| Effector → Receptor pairing (intra-Structure or inter-Structure) | **Interaction** | Emitted even when the counterpart is absent (anticipatory). |
| DTO / record / payload class                    | **Archetype** (data, rootless) | Title = class name; schema derived from fields. |

### Table C — code element → port-typing Archetype family

| Source signal | Effector / Receptor port type |
|---|---|
| `WebClient.get/post/...`, `RestTemplate.exchange`, `HttpClient.send` | `gsmarc://itip/frameworks/http/HttpRequestEffector/v1` |
| Spring `@GetMapping`, `@PostMapping`, JAX-RS, servlet | `gsmarc://itip/frameworks/http/HttpRequestReceptor/v1` |
| Kafka `KafkaTemplate.send` | `gsmarc://itip/frameworks/kafka/...` *(quad pending)* |
| Kafka `@KafkaListener` | `gsmarc://itip/frameworks/kafka/...` *(quad pending)* |
| gRPC client stub / generated server | `gsmarc://itip/frameworks/grpc/...` *(quad pending)* |
| JDBC / JPA repository | `gsmarc://itip/frameworks/jdbc/...` *(quad pending)* |
| Intra-process method call (Mechanism A → Mechanism B in same Structure) | **Code-API** family *(see §7; pending term confirmation)* |

### Table D — what is NOT mapped

| Code element | Why excluded |
|---|---|
| Logging statements (`log.info(...)`) | Cross-cutting concern; not a GSM primitive. |
| Metrics, traces, profiling instrumentation | DSM (description plane), not GSM. |
| Configuration loading (`@Value`, `application.yml`) | Not behaviour; not modelled. |
| DI wiring (`@Autowired`, `@Bean`) | Mechanism wiring is a runtime concern derived from Interactions. |
| Framework boilerplate (Spring lifecycle hooks, etc.) | Not behaviour-bearing. |
| Helper utilities with no I/O effect | Filtered by Axis 2 granularity. |

---

## 4. Granularity (committed defaults; extensible)

Two orthogonal axes drive what gets emitted. The architecture **must leave room** for additional profiles per axis (see deferred ideas in `/memories/repo/gsm-sourcer-deferred-ideas.md`).

| Axis | What it controls | Default profile (committed) | Future profiles (deferred) |
|---|---|---|---|
| **Axis 1 — depth** | What code is in scope for extraction | **Functional** — only functionally meaningful behaviour-bearing code | Heuristic-driven: reachability, test-corroborated, hotspot-weighted, coverage-driven |
| **Axis 2 — mechanism granularity** | How code is decomposed into Mechanisms | **Standard** — one Mechanism per cohesive functional unit (typically one per service/handler class) | Coarse/Capability, Fine/Per-statement, Boundary-only |

The Sourcing Run config selects a profile per axis.

---

## 5. Architecture — Option A (committed) + structural pattern (proposal)

### Option A (committed)

> **gsm-sourcer is a generic engine. ITIP owns the IT-specific source plug-ins (per repo type / language / build system) and the IT-specific target catalogue plug-ins (TOGAF, SAFe, ISO frameworks, …).**

Invariants:

- Modularization — every source family and every target catalogue is a discrete plug-in.
- Extensibility — new plug-ins can be added without touching the engine.
- Owning politics — IT-specific concerns live in ITIP, not in gsm-sourcer.

### [PROPOSAL — pending confirmation] Structural pattern: A2 + A3 hybrid

After exploring five candidate patterns (Mapper, Maven-style lifecycle, Compiler frontend/middle/backend, Hexagonal core+adapters, LSP-style server), the recommendation is:

> **Maven-style lifecycle (A2) layered on top of a compiler IR (A3).**

Concretely:

- gsm-sourcer defines a **fixed Sourcing Lifecycle** with named phases: `inventory → extract → verify → merge → supersede → validate → emit`.
- Plug-ins register **goals** bound to phases; multiple goals per phase execute in declared order.
- Phases communicate via a **Sourcing IR** (typed AST of candidate GSM primitives + provenance + diagnostics).
- The `emit` phase runs **per target catalogue** so a single source pass can be projected onto multiple catalogues without re-extraction.

**Pending confirmation before this is committed to design.** Other patterns considered are recorded in conversation notes and can be revisited.

---

## 6. Plug-in registry — R3 (committed for now)

> **Each Sourcing Run lists explicit plug-ins (sources + emitters) by URI/version in its run-config.**

This is the smallest credible step and remains compatible with future evolutions. Alternatives considered:

- **R1 — DM-governed** plug-in registry (full Ascription lifecycle): right end-state for SaaS multi-tenant. Deferred.
- **R2 — Filesystem discovery**: rejected (no audit, no approval gate).
- **R4 — OCI registry**: rejected as premature.
- **R5 — Java SPI / classpath**: rejected (antithetical to the extensibility invariant).
- **R6 — Hybrid (R3 over R1)**: target end-state once R1 lands.

The Sourcing Run config schema **must be designed so it can later reference DM-governed plug-in Definitions by URI** when R1 lands without breaking.

---

## 7. Intra-Structure couplings — Code-API family [PROPOSAL]

The classical RPC/LPC distinction (remote procedure call vs. local *inter-process* call) does **not** apply cleanly to in-process function/method calls between Mechanisms in the same software Structure: those calls are language-level invocations within one address space, not LPC.

> **Proposal**: introduce a **Code-API** protocol family in `itip/def/frameworks/code-api/` for typing intra-Structure causal couplings whose carrier is a language-level invocation (method/function call). Identity-bound by callee `target` + `signature`. Distinct from Relay (which is an SIE Operator runtime primitive between Mechanisms during one operation).

To be authored when the first SBAS scenario needs it. Pending confirmation of the term.

---

## 8. Protocol archetype ownership (committed; just executed)

| Protocol family | Owner | Location | `$id` scheme |
|---|---|---|---|
| Relay (in-memory causal propagation) | sie-operator | `sie/sie-operator/def/statement/protocol/relay/` | `gsmarc://ops/protocols/relay/{Title}/v1` |
| Governance (DM ↔ Operator events) | sie-operator | `sie/sie-operator/def/statement/protocol/governance/` | `gsmarc://ops/protocols/governance/{Title}/v1` |
| HTTP | **ITIP** | `itip/def/frameworks/http/` | `gsmarc://itip/frameworks/http/{Title}/v1` |
| Kafka, AMQP, gRPC, GraphQL, JDBC, WebSocket | **ITIP** *(quads pending)* | `itip/def/frameworks/{family}/` | `gsmarc://itip/frameworks/{family}/{Title}/v1` |

The HTTP archetype quad is **vendored** to the operator classpath via a Maven `<resource>` directive in `sie/sie-operator/pom.xml` so `StructureSeedRunner` can register it at startup without a network round-trip.

---

## 9. Deferred work

Recorded in `/memories/repo/gsm-sourcer-deferred-ideas.md`. To be sequenced via the product-manager workflow into `def/features/gsm-sourcer-safe-backlog.puml`. Highlights:

- Additional granularity profiles for both axes (heuristic depth, alternative mechanism granularities, reachability/test-corroboration filters).
- A2+A3 architecture confirmation and formal Sourcing IR / phase contract.
- R6 hybrid plug-in registry.
- Code-API archetype quad authoring.
- DefMan auto-derivation reactivation under a flag.

---

## 10. Next steps (in order)

1. Confirm or revise §5 (A2+A3 hybrid), §7 (Code-API name), §6 (R3 stance) — these are the only un-committed items.
2. Author `def/code-to-gsm-mapping.md` — normative spec from Tables B/C/D.
3. Author `def/sourcing-run-config-schema.json` — Run config (depth axis, granularity axis, plug-in selection, target catalogue selection) with extensibility hooks.
4. Author `.github/skills/gsm-sourcing-from-code/SKILL.md` — workflow skill for SBAS runs.
5. When a sourcing scenario requires it: generate the Code-API protocol family and any missing application-protocol quads under `itip/def/frameworks/{family}/`.

---

## See also

- `README.legacy.md` — superseded design exploration kept for traceability.
- `/memories/repo/gsm-sourcer-deferred-ideas.md` — backlog candidates.
- `/memories/repo/protocol-sourcing-patterns.md` — earlier per-verb-effector design (preserved as a future option for HTTP).
- `/memories/repo/gsm-extension-points.md`.
- `itip/def/frameworks/README.md` — framework catalogue and inter-framework articulation.
- `sie/sie-operator/def/statement/README.md` — operator-internal statement definitions.

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

The GSM Sourcer is triggerable from the ITIP UI (and future domain app UIs):

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
