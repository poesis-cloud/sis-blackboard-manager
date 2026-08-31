# PROJECT_BRIEF.md — sie-definition-blackboard-manager

> Last updated: 2025-07 | Sprint 0 | Status: Bootstrapping

## 1. Project Overview

`sie-definition-blackboard-manager` is the **server-side REST blackboard service** for collaborative
GSM definition sourcing within the Systemic Intelligence Engine (SIE). It manages the full lifecycle
of a Blackboard: from open contribution intake, through sealing, to byte-stable immutable output.
Consumed by client sourcers (e.g., `itip-definition-blackboard-repository-sourcer`); clients post
contributions, the server controls state.

The service enforces the Blackboard isolation invariant: once sealed, no mutation is permitted.
All mutations are recorded in an audit ledger for governance traceability.

## 2. Concept / Product Description

The **Definition Blackboard** is a GSM governance mechanism for collaborative, multi-sourcer definition
assembly. The server owns:

- **Blackboard**: root aggregate; lifecycle state (OPEN/SEALED/BYTE_STABLE), panel schema reference,
  list of contributions.
- **Panel**: schema declaration constraining which contributions are valid for a given blackboard.
- **Contribution**: a sourcer-posted JSON payload validated against the panel schema.
- **Blackboard-seal lifecycle**: OPEN -> SEALED -> BYTE_STABLE — no backwards transitions allowed.
- **Audit ledger**: every create/update/seal/reject operation appended as an immutable audit entry.

Client flow:
1. Client discovers an open Blackboard via `GET /blackboards`.
2. Client POSTs a Contribution to `POST /blackboards/{id}/contributions`.
3. Server validates contribution against panel schema; rejects invalid ones (HTTP 422).
4. Operator seals the Blackboard via `POST /blackboards/{id}/seal`.
5. Sealed Blackboard produces a byte-stable contribution stream (no further writes).

## 3. Tech Stack

- **Language / Runtime:** Java 21
- **Framework:** Spring Boot 3.5.x (Web, HATEOAS, Data JPA)
- **Build:** Maven (`cloud.poesis.sie:sie-definition-blackboard-manager:1.0.0-beta.1`)
- **Database:** PostgreSQL (managed by companion repo `sie-definition-blackboard-database`)
- **Testing:** JUnit 5, Testcontainers (Postgres), Mockito; JaCoCo >= 95% instruction coverage
- **Container:** Docker (Dockerfile at repo root)
- **Orchestration:** Kubernetes / AKS, Helm (`ops/helm/` — gap, not yet created)
- **CI/CD:** GitHub Actions (`.github/workflows/ci.yaml`, `cd.yaml` — gap, not yet created)
- **Static analysis:** SpotBugs (`spotbugs-exclude.xml` at root)

## 4. Architecture

```
+------------------------------------------------------------------+
|  Clients (e.g. itip-definition-blackboard-repository-sourcer)   |
|  REST HTTP calls (Spring HATEOAS hypermedia links)               |
+------------------------------+-----------------------------------+
                               | HTTPS / REST
+------------------------------v-----------------------------------+
|  sie-definition-blackboard-manager  (Spring Boot 3 / Java 21)   |
|                                                                  |
|  BlackboardController   -- CRUD + seal lifecycle                 |
|  ContributionController -- POST/GET contributions                |
|  AuditController        -- read-only audit ledger                |
|                                                                  |
|  BlackboardService      -- lifecycle state machine               |
|  ContributionService    -- schema validation + persistence       |
|  AuditService           -- append-only ledger writes             |
|                                                                  |
|  Domain: Blackboard, Panel, Contribution, AuditEntry             |
+------------------------------+-----------------------------------+
                               | JDBC / JPA
+------------------------------v-----------------------------------+
|  sie-definition-blackboard-database  (PostgreSQL, companion)     |
|  Tables: blackboard, panel, contribution, audit_entry            |
+------------------------------------------------------------------+
```

## 5. Key Files Map

| Area | Path | Contents |
|------|------|----------|
| Maven build | `pom.xml` | Dependencies, Java 21, Spring Boot 3.5 |
| SpotBugs exclusions | `spotbugs-exclude.xml` | Static analysis suppressions |
| Domain definitions | `def/` | GSM-aligned JSON schemas / design artifacts |
| Java source | `src/main/java/cloud/poesis/sie/blackboard/` | Application code |
| Java tests | `src/test/java/cloud/poesis/sie/blackboard/` | Unit + integration tests |
| Dockerfile | `Dockerfile` | Container image |
| Helm chart | `ops/helm/` | Kubernetes deployment (gap — not yet created) |
| CI workflow | `.github/workflows/ci.yaml` | Maven build, lint, test (gap) |
| CD workflow | `.github/workflows/cd.yaml` | Helm deploy to AKS (gap) |
| Sprint docs | `docs/sprint-N/` | Plans, progress, done |

## 6. Team Roles

| Agent | Name | Role |
|-------|------|------|
| Producer | **Remy** | Sprint plans, GitHub Issues, PR coordination. Never writes application code. Skills: `product-manager`, `create-pr`, `update-pr`, `sync`, `commit`, `chronicle` |
| Dev | **Nova / Sage / Milo** | REST API, Blackboard lifecycle, contribution validation, audit ledger. Skills: `definition-blackboard-manager`, `gsm-knowledge`, `context-map`, `refactor-plan`, `commit`, `create-pr` |
| QA | **Ivy** | Seal lifecycle tests, contribution validation, audit integrity, JaCoCo >= 95%. Skills: `code-review`, `commit` |

## 7. Sprint Status

| Sprint | Name | Status | Scope |
|--------|------|--------|-------|
| 0 | Bootstrap | Done | PROJECT_BRIEF, Copilot instructions, prompt files |
| 1 | Core Domain | Planned | Blackboard/Panel/Contribution entities, lifecycle state machine, REST API skeleton |
| 2 | Contribution Validation | Planned | Panel schema validation, rejection responses, audit ledger |
| 3 | Seal + Byte-Stable Stream | Planned | Seal endpoint, byte-stable output, immutability enforcement |
| 4 | ops/ + CI/CD | Planned | Helm chart, GitHub Actions ci/cd workflows |

## 8. Current State (rewrite every sprint)

**What works:**
- Maven build skeleton with Spring Boot 3.5 / Java 21
- Dockerfile present

**What does not work yet:**
- No REST controllers, no domain entities (Blackboard, Panel, Contribution, AuditEntry)
- No lifecycle state machine
- No `ops/` directory (Helm chart missing)
- No `.github/workflows/` (CI/CD missing)

**Known gaps:**
- WARNING: `ops/` directory is absent — Helm chart + env values need creation per SIE Repository
  Structure Consistency Rule (`ops/helm/environments/{dev,preprod,prod}/values.yaml`, `ops/README.md`,
  Makefile targets `dev-up`, `dev-down`, `dev-check`, `prod-deploy`, `package-helm`)
- WARNING: No `.github/workflows/ci.yaml` or `cd.yaml`

**What is next:**
- Sprint 1: implement core domain model and REST skeleton

## 9. Security Rules

1. Secrets live in Kubernetes Secrets / env vars only — never in code or git.
2. `.env.dev` (machine-local secrets) must never be committed.
3. Sealed Blackboards are immutable — all write-path handlers must verify state before accepting mutations.
4. Audit ledger entries are append-only; no update/delete endpoints for audit entries.
5. Contribution payloads validated against panel schemas before persistence; reject HTTP 422 on violation.

## 10. How to Run Locally

```bash
# Prerequisites: Java 21, Maven, Docker (for Testcontainers)
cd /home/clem/repositories/poesis/sie/sie-definition-blackboard-manager
mvn clean verify        # build + test (Testcontainers spins up Postgres automatically)
mvn spring-boot:run     # run locally (requires a Postgres instance)
```

## 11. How to Deploy

```bash
make package-helm   # package Helm chart
make dev-up         # deploy to dev (AKS)
make prod-deploy    # deploy to prod
```

CI/CD pipelines (once created):
- `ci.yaml`: triggered on every push — Maven build + test + SpotBugs + Helm lint
- `cd.yaml`: manually triggered — Azure OIDC login + `helm upgrade --install` (namespace: `sie`)

## 12. Cross-Chat Handoff Protocol

Every sprint chat must do these before finishing:
1. Write `docs/sprint-N/done.md` — what was built, what is not done, files changed/created.
2. Update `PROJECT_BRIEF.md`: Section 7 (mark sprint done) + Section 8 (rewrite current state).
3. Commit all changes: `sprint-N: <summary>`.

The repo is the shared memory. If skipped, the next chat starts blind.

## 13. Bug & Fix Tracking

Bugs tracked as GitHub Issues on `poesis-cloud/sie-definition-blackboard-manager`.

**QA (Ivy):** File bugs with labels (`bug`, `severity:blocker/major/minor`). When no blockers:
write `docs/qa/sprint-N-signoff.md` with test count, pass rate, "no blockers" statement.

**Dev team:** Check Issues before starting. Use closing keywords: `fix: description (Fixes #42)`.

**Feature ideas:** add to `docs/ideas-backlog.md`.

## 14. Multi-Repo Setup

Teams use separate clones, separate branches, PRs to main. Never push directly to main.
Never squash or rebase feature branches (causes commit loss).

**Branches:** `feature/sprint-N` (dev), `feature/qa-N` (QA), `main` (Remy coordination).

```bash
git clone git@github.com:poesis-cloud/sie-definition-blackboard-manager.git <folder>
cd <folder> && git checkout -b <branch>
```

## 15. Companion Repos

| Repo | Role |
|------|------|
| `sie-definition-blackboard-database` | PostgreSQL schema, Helm/ops only — paired DB repo |
| `itip-definition-blackboard-repository-sourcer` | Client sourcer — posts contributions |
