# Copilot Instructions — sie-definition-blackboard-manager

This is the **server-side REST blackboard service** for collaborative GSM definition sourcing.
Read `PROJECT_BRIEF.md` at the repo root for full context before starting any task.

---

## Team

### Remy (Producer)
Sprint planning, GitHub Issues, PR coordination. **Never writes application code.**
Skills: `product-manager`, `create-pr`, `update-pr`, `sync`, `commit`, `chronicle`

### Nova / Sage / Milo (Dev)
Implement the server REST API, Blackboard lifecycle, contribution validation, and audit ledger.
Skills: `definition-blackboard-manager`, `gsm-knowledge`, `agent-customization`, `update-skills`,
`commit`, `create-pr`, `context-map`, `refactor-plan`

### Ivy (QA)
Blackboard seal lifecycle tests, contribution validation correctness, audit ledger integrity,
JaCoCo >= 95%.
Skills: `code-review`, `commit`

---

## Domain Rules (mandatory — enforce on every PR and every write path)

### Blackboard isolation invariant
Sealed blackboards are **immutable**. Every write-path handler (contributions, panel updates, state
changes) MUST check the Blackboard state before accepting a mutation. Reject with HTTP 409 if the
blackboard is SEALED or BYTE_STABLE.

### Blackboard-seal state machine
```
OPEN -> SEALED -> BYTE_STABLE
```
No backwards transitions. No state skipping. Enforce as a validated state machine in
`BlackboardService`. Any attempt to transition backwards is a hard error (HTTP 422).

### Contribution validation
Reject invalid contributions **before** persistence. Validate the contribution payload against the
referenced Panel schema. Return HTTP 422 with a structured error body on schema violations.
Never persist an invalid contribution.

### Audit ledger
Every mutation (blackboard create/seal, contribution accept/reject, panel update) MUST produce an
immutable `AuditEntry`. The audit ledger is append-only — no update/delete endpoints for audit
entries. Audit writes must be transactional with the mutation they record.

### Repository-Service exclusivity rule
Each Repository is consumed by exactly one Service. If a service needs data owned by another service,
call that service — never access another service's repository directly.
Exception: `AbstractAscriptionService` may use `ArchetypeRepository` directly for tenant-schema
resolution (documented exception — see PROJECT_BRIEF.md).

---

## Engineering Rules

### Root-cause-first
Fix the actual cause of warnings/errors before using `@SuppressWarnings`, `// noinspection`, or
similar suppression. Suppression is last resort; scope it as narrowly as possible (single field/
statement, never class-level) with a comment explaining why.

### JaCoCo >= 95% instruction coverage
Every `src/main` class must have corresponding tests. After adding or modifying `src/main` code, run
`mvn test jacoco:report` and verify `target/site/jacoco/index.html` shows >= 95% at module level.

### Never pipe mvn
Run Maven commands directly — no `| cat`, `| tee`, `| grep`, `2>&1 | ...`. Piping disrupts WSL
stability and hides real-time test progress.

### Git history preservation
Use `git mv` / `git rm` for tracked file moves and deletes. Never use plain OS-level `mv`/`rm`.

### Archive folders are read-only
`archives/` and `archive/` folders contain superseded historical content. Never edit, update, or
delete files inside them.

### ops/ structure (SIE Repository Structure Consistency Rule)
When `ops/` is created it must follow exactly:
```
ops/
  README.md
  helm/
    Chart.yaml
    environments/
      dev/values.yaml
      preprod/values.yaml
      prod/values.yaml
    templates/
```
No `ops/helm/values.yaml` at chart root — values live only under `environments/`.
Makefile targets: `dev-up`, `dev-down`, `dev-check`, `prod-deploy`, `package-helm`.
Namespace: `sie`.

### YAML naming
Use `.yaml` for all YAML files. Do not add new `.yml` files.

---

## Tech Stack Quick Reference

- Java 21, Spring Boot 3.5.x, Maven
- GroupId: `cloud.poesis.sie`, ArtifactId: `sie-definition-blackboard-manager`
- PostgreSQL via companion repo `sie-definition-blackboard-database`
- Kubernetes / AKS, Helm; Testcontainers for integration tests
- SpotBugs (`spotbugs-exclude.xml`)

---

## Known Gaps (Sprint 0)

- WARNING: `ops/` directory missing — Helm chart not yet created (Sprint 4)
- WARNING: `.github/workflows/` missing — CI/CD not yet created (Sprint 4)
- WARNING: No domain entities or REST controllers yet — Sprint 1 target

---

## SE Plugin Agents (global — invoke by name)

These agents are installed globally via the `software-engineering-team` plugin. Invoke them by name in any chat.

| When | Invoke |
|---|---|
| Security review before any merge | `SE: Security` |
| Architecture decision or structurant PR | `SE: Architect` |
| CI/CD pipeline, Helm, deployment debug | `SE: DevOps/CI` |
| Writing/updating API docs, ADRs, README | `SE: Technical Writer` |
| Authoring GitHub Issues or backlog items | `SE: Product Manager Advisor` |
