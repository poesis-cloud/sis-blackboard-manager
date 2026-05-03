# SIE Definition Blackboard Manager

A **generic, domain-agnostic REST blackboard service** for collaborative
definition sourcing.

This service is part of the **SIE engine**. It owns the persisted blackboard
data and the seal lifecycle that turns client-contributed hypotheses into a
sealed, byte-stable, auditable contribution stream. It does not interpret any
domain ontology and it does not reduce contributions: client sourcers (e.g.
ITIP's repository definition sourcer) declare panels and slot schemas
entirely as data via REST; client controllers reduce the sealed stream
client-side.

**Minimality**: the substrate is intentionally minimal -- it stores facts,
validates per-slot, and seals. Concepts beyond that (e.g. dedupe, identity
keys, evidence, contributor metadata) are client responsibilities and will
be added to the substrate only if a concrete ITIP client need surfaces.

> Repository was previously named `gsm-sourcer`. The engine-loaded "Pack"
> model is gone: client sourcers now run in their own processes and speak
> HTTP to this service.

---

## 1. Role in SIE

```
┌───────────────────┐    HTTP/REST    ┌──────────────────────────────┐
│ Client sourcer    │ ──────────────► │  Definition Blackboard       │
│ (e.g. ITIP        │                 │  Manager  (this service)     │
│  repository       │ ◄────────────── │                              │
│  definition       │   sealed        │  - Blackboard resource       │
│  sourcer)         │   contribution  │  - Panel / Slot registry     │
└─────────┬─────────┘   stream        │  - Contribution validation   │
          │                           │  - Blackboard-seal state     │
          │ client controller +       │    machine (server-owned)    │
          │ pipeline (reduction,      │  - Audit ledger              │
          │ then domain consumers)    │                              │
          ▼                           └──────────────────────────────┘
   Definition Manager
   (downstream consumer)
```

- **Client sourcer** owns: domain semantics, Knowledge Source machinery,
  any orchestration concept (e.g. a client-side "Run"), the
  **reducer/controller** (selecting/merging contributions per slot), and
  the post-reduction pipeline (e.g. registration to Definition Manager).
- **This service** owns: blackboard data and seal lifecycle. It has **no
  Run, no Phase, no chaining concept, no correlation, no resolver, no
  reduction concept**. Each `Blackboard` is fully isolated -- the
  substrate exposes no inter-Blackboard relation.

Multiple client sourcers (different domains, different teams, different
languages) can share one instance of this service.

---

## 2. Why a REST blackboard service

The classic Blackboard pattern (Hearsay-II, BB1, GBB) co-locates the
blackboard, knowledge sources, and controller in one process. That model
forces every client to plug into the same JVM, share trust, and obey the
same SPI.

We split along the **trust + ownership** boundary:

| Concern                              | Where it lives    | Why                                          |
| ------------------------------------ | ----------------- | -------------------------------------------- |
| Blackboard data                      | This service      | Single arbiter; authoritative audit ledger   |
| Blackboard-seal lifecycle            | This service      | Server-owned state machine; can't be forged  |
| Knowledge Source machinery           | Client sourcer    | Any language; client-private code            |
| Run loop (which KS fires when)       | Client sourcer    | Domain-specific; not the service's concern   |
| Domain ontology (panel meaning)      | Client sourcer    | Service treats panels as opaque names        |
| **Reduction** (winner / merge / compose) | Client controller | Logged, replayable process over sealed stream |
| Post-reduction pipeline              | Client sourcer    | Validation/registration is domain-specific   |

Result: the service is **domain-agnostic, reduction-free, and reusable**;
clients are free to choose their own technology stack and their own reduction
algorithm; audit and replay remain anchored on the sealed contribution stream.

---

## 3. REST surface (overview)

| Resource                                            | Verbs                | Notes                                              |
| --------------------------------------------------- | -------------------- | -------------------------------------------------- |
| `/blackboards`                                      | POST, GET            | Create / list Blackboards; body is `BlackboardConfig` |
| `/blackboards/{id}`                                 | GET                  | Blackboard state + config                          |
| `/blackboards/{id}/abort`                           | POST                 | Abort an OPEN or SEALING Blackboard                |
| `/blackboards/{id}/panels`                          | POST, GET            | Declare panel topology (frozen at first contribution) |
| `/blackboards/{id}/contributions`                   | POST, GET            | On SEALED Bb, GET returns the byte-stable sealed stream. |
| `/blackboards/{id}/contributions/{cid}`             | GET                  | Single-contribution trace                          |
| `/blackboards/{id}/state`                           | GET                  | Live (pre-seal) view of POSTED contributions       |
| `/blackboards/{id}/seal`                            | POST                 | Server-owned `OPEN → SEALING → SEALED` transition. Freezes the stream; runs no reduction. |
| `/blackboards/{id}/audit`                           | GET                  | Append-only ledger (POSTED, REJECTED, seal events) |

There is no `/resolution` endpoint. Reading the sealed contribution stream
(`GET /blackboards/{id}/contributions` once the Bb is `SEALED`) is the
deterministic input for client-side reducers.

Auth scopes: `bb:write` (contributions), `bb:seal` (Blackboard seal),
`bb:bb-admin` (create / abort Blackboards, declare panels), `bb:read`
(everything else).

Concurrency:

- `If-Match` ETag CAS on panel declarations and seal calls.

See [def/blackboard/schemas/](def/blackboard/schemas/) for the JSON schemas
of `BlackboardConfig`, `PanelDeclaration`, `Contribution`. (OpenAPI spec is
a follow-up artefact.)

---

## 4. Server-side data model

See [def/blackboard/blackboard.puml](def/blackboard/blackboard.puml) for
the full class diagram. Core entities:

- **`Blackboard`** -- the single top-level resource; fully isolated, with
  no inter-Blackboard relation (no correlation, no chaining).
- **`BlackboardStatus`** -- `OPEN -> SEALING -> SEALED` (or `ABORTED`).
- **`Panel`** -- client-namespaced; topology frozen at first contribution.
  Description is mandatory.
- **`ContributionSlot`** -- `name + schemaUri`. Multiple contributions per
  slot are accepted; reducing them is a client-controller concern.
- **`Contribution`** -- immutable after POST; carries
  `(panel, slot, post, derivedContribution, timestamp)`. `post` is
  validated against `slot.schemaUri` at POST time. `derivedContribution[]`
  URIs MUST reference contributions in the **same** Blackboard; cross-Bb
  URIs are rejected (422).

There is no `BlackboardResolution` entity. Reduction outputs, if a client
wants them as Bb citizens, are posted as ordinary contributions on a
`<client>:Resolved.*` panel before seal.

---

## 5. Blackboard-seal lifecycle (server-owned)

See [def/blackboard/blackboard-lifecycle.puml](def/blackboard/blackboard-lifecycle.puml).

```
OPEN ──POST seal──► SEALING ──success──► SEALED
                       │
                       └── irrecoverable error ──► ABORTED
```

Why server-side:

- Single arbiter for "are we done with this Blackboard".
- The seal moment is the audit anchor for deterministic replay: it freezes
  the contribution set and gives the stream a byte-stable order.
- The service does **not** chain Blackboards. **Each Blackboard is fully
  isolated**: the substrate exposes no inter-Blackboard relation (no
  correlation, no chaining, no shared lineage). Any client flow that
  spans multiple Blackboards is responsible for its own bookkeeping
  out-of-band.

The service performs no reduction during `SEALING`; it only freezes the
stream and closes the audit ledger.

---

## 6. Reduction is controller-side

Reducing N>1 contributions per slot -- choosing a winner, merging values,
composing per-key views, etc. -- is **not** a substrate concern. It is a
client-controller concern executed over the sealed contribution stream.

Pattern:

1. Client seals the Blackboard (`POST /blackboards/{id}/seal`).
2. Client controller `GET`s the sealed contribution stream with the desired
   filter (`?panel=&slot=`). The stream is byte-stable: same call returns
   the same bytes forever.
3. Client applies its reducer (any algorithm: highest-confidence, first-writer,
   collect-all, compose, weighted ensemble, LLM-mediated arbitration, ...).
4. Client logs the reduction process to its own process log -- this is the
   auditable, replayable "reduction process".
5. (Optional) Client publishes the reduced view as terminal contributions on
   a `<client>:Resolved.*` panel **inside the same Bb, before seal**, so the
   reduction output is itself a first-class auditable Bb citizen with the
   same query/audit surface as any other contribution.

Why this is right:

- Substrate stores **facts**, not **decisions**. A REST service shared across
  clients should not bake one specific control strategy.
- The same sealed Bb can be reduced differently by different consumers
  (different reducers, different policies) without re-running the producers.
- Reduction is a process; processes belong in a controller, where they can
  be logged, versioned, retried, A/B-tested, and replayed.

The substrate guarantees byte-stable sealed inputs (replay anchor).
Everything else is client controller territory.

---

## 7. Diagrams

| File                                                                | Kind      | Purpose                                  |
| ------------------------------------------------------------------- | --------- | ---------------------------------------- |
| [component.puml](def/blackboard/component.puml)                     | PD        | Service-internal components + actors     |
| [usecases.puml](def/blackboard/usecases.puml)                       | UC        | REST API consumer use cases              |
| [blackboard.puml](def/blackboard/blackboard.puml)                   | CD        | Server-side data model                   |
| [blackboard-lifecycle.puml](def/blackboard/blackboard-lifecycle.puml) | State   | Per-Blackboard lifecycle (OPEN -> SEALING -> SEALED \| ABORTED) |

The end-to-end REST sequence diagram (client-driven) lives in the
first client sourcer that exercises this service:
[itip-definition-blackboard-repository-sourcer/def/blackboard-workflow.puml](../../itip/itip-definition-blackboard-repository-sourcer/def/blackboard-workflow.puml).

---

## 8. What is NOT in this service

Deliberately out of scope (client-side concerns):

- Knowledge Source execution, sandboxing, classloaders.
- Run loop (deciding which KS to fire when).
- Any orchestration concept ("Run", "Phase", chaining). Each
  `Blackboard` is fully isolated: the substrate exposes no
  inter-Blackboard relation (no correlation, no shared lineage).
- Domain ontology (what a panel means, what a slot value represents).
- **Reduction / resolution of contributions** per slot -- choosing a winner,
  merging values, composing per-key views. This is a client controller
  concern, executed against the sealed stream.
- **Dedupe / idempotency / per-identity grouping** of contributions. The
  substrate accepts every well-formed contribution; clients that need
  deduplication apply it controller-side over the sealed stream.
- Post-reduction domain pipeline (e.g. validating reduced values against a
  domain spine, registering into Definition Manager).
- LLM/inference services, external-tool brokering, source-bundle storage.
- Identity reconciliation across reductions (client-side, using
  same-Blackboard `derivedContribution` lineage).

These were all part of the previous engine-loaded "Pack" model. They now
live in client sourcer repositories (the first one being ITIP's
[itip-definition-blackboard-repository-sourcer](../../itip/itip-definition-blackboard-repository-sourcer/)).

---

## 9. Reserved for later: KS-runner callback

A future centralization mode (a central controller driving multiple
client sourcers from one place) will require a server-initiated callback
into client KS runners. The REST surface reserves the
`/blackboards/{id}/seal` endpoint shape and a future
`/blackboards/{id}/events` SSE stream as the controller-facing surface;
no implementation in this phase.

---

## 10. Skill

The corresponding Copilot skill is at
[.github/skills/definition-blackboard-manager/SKILL.md](../../.github/skills/definition-blackboard-manager/SKILL.md).
It is the authoritative reference for both the service and the
client sourcer authoring contract.
