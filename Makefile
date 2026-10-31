SHELL := /bin/bash
.EXPORT_ALL_VARIABLES:

.PHONY: dev-check dev-up dev-down deploy-check prod-deploy package-helm run-api test verify ensure-runtime

-include .env
-include .env.dev

NAMESPACE ?= sis
DEPLOY_ENV ?= preprod
RELEASE_DEF_BB_DB ?= sie-definition-blackboard-database

BBM_CHART ?= ./ops/helm
BBM_ENV_FILE ?= $(BBM_CHART)/environments/$(DEPLOY_ENV)/values.yaml

PORT_FORWARD_PID_FILE ?= .dev-port-forwards.pids

define kill_port_listener
	@ss -ltnp '( sport = :$(1) )' 2>/dev/null | awk -F'pid=' '/kubectl/ {split($$2, parts, /[,)]/); print parts[1]}' | xargs -r kill
endef

ensure-runtime:
	@if [[ -n "$$XDG_RUNTIME_DIR" ]] && [[ ! -d "$$XDG_RUNTIME_DIR" ]]; then \
		echo "Creating missing XDG_RUNTIME_DIR ($$XDG_RUNTIME_DIR)..."; \
		sudo mkdir -p "$$XDG_RUNTIME_DIR" && sudo chown $$(id -u):$$(id -g) "$$XDG_RUNTIME_DIR" && chmod 700 "$$XDG_RUNTIME_DIR"; \
	fi

dev-check:
	@command -v kubectl >/dev/null 2>&1 || { echo "Missing required command: kubectl"; exit 1; }
	@command -v helm >/dev/null 2>&1 || { echo "Missing required command: helm"; exit 1; }
	@kubectl config current-context >/dev/null 2>&1 || { echo "No active Kubernetes context. Configure kubeconfig first."; exit 1; }
	@kubectl get ns >/dev/null 2>&1 || { echo "Cannot reach Kubernetes API with current context."; exit 1; }
	@test -d "$(BBM_CHART)" || { echo "Missing chart directory: $(BBM_CHART)"; exit 1; }
	@test -f "$(BBM_CHART)/environments/dev/values.yaml" || { echo "Missing dev values file: $(BBM_CHART)/environments/dev/values.yaml"; exit 1; }
	@echo "dev-check passed"

deploy-check:
	@command -v kubectl >/dev/null 2>&1 || { echo "Missing required command: kubectl"; exit 1; }
	@command -v helm >/dev/null 2>&1 || { echo "Missing required command: helm"; exit 1; }
	@kubectl config current-context >/dev/null 2>&1 || { echo "No active Kubernetes context. Configure kubeconfig first."; exit 1; }
	@kubectl get ns >/dev/null 2>&1 || { echo "Cannot reach Kubernetes API with current context."; exit 1; }
	@test -d "$(BBM_CHART)" || { echo "Missing chart directory: $(BBM_CHART)"; exit 1; }
	@test -f "$(BBM_ENV_FILE)" || { echo "Missing environment values file: $(BBM_ENV_FILE)"; exit 1; }
	@: "$${BBM_DB_PASSWORD:?Missing BBM_DB_PASSWORD in environment}"
	@: "$${IMAGE_REPOSITORY:?Missing IMAGE_REPOSITORY in environment}"
	@: "$${IMAGE_TAG:?Missing IMAGE_TAG in environment}"
	@echo "deploy-check passed"

dev-up:
	@$(MAKE) ensure-runtime
	@: "$${DB_USER:?Missing DB_USER in .env.dev}"
	@: "$${DB_PASSWORD:?Missing DB_PASSWORD in .env.dev}"
	kubectl get ns $(NAMESPACE) >/dev/null 2>&1 || kubectl create ns $(NAMESPACE) >/dev/null
	helm upgrade --install sis-blackboard-manager $(BBM_CHART) -n $(NAMESPACE) --create-namespace --wait --timeout 5m0s \
		-f $(BBM_CHART)/environments/dev/values.yaml
	@if [[ -f "$(PORT_FORWARD_PID_FILE)" ]]; then \
		xargs -r kill < "$(PORT_FORWARD_PID_FILE)" 2>/dev/null || true; \
		rm -f "$(PORT_FORWARD_PID_FILE)"; \
	fi
	$(call kill_port_listener,5432)
	nohup kubectl -n $(NAMESPACE) port-forward svc/$(RELEASE_DEF_BB_DB) 5432:5432 >/tmp/sis-bbm-pf-db.log 2>&1 & echo $$! >> "$(PORT_FORWARD_PID_FILE)"
	@sleep 2
	@grep -q "Forwarding from 127.0.0.1:5432" /tmp/sis-bbm-pf-db.log || { echo "Database port-forward failed. See /tmp/sis-bbm-pf-db.log"; exit 1; }
	@echo "Dependencies are ready and port-forwards started."
	@echo "Now run: make run-api"

dev-down:
	@PID=$$(lsof -ti :8080 2>/dev/null); \
	if [[ -n "$$PID" ]]; then \
		kill $$PID 2>/dev/null || true; \
		echo "Stopped process on port 8080 (PID $$PID)"; \
	fi
	@if [[ -f "$(PORT_FORWARD_PID_FILE)" ]]; then \
		xargs -r kill < "$(PORT_FORWARD_PID_FILE)" 2>/dev/null || true; \
		rm -f "$(PORT_FORWARD_PID_FILE)"; \
	fi
	helm uninstall sis-blackboard-manager -n $(NAMESPACE) || true

run-api:
	DB_URL="$(DB_URL)" \
	DB_USER="$(DB_USER)" \
	DB_PASSWORD="$(DB_PASSWORD)" \
	mvn spring-boot:run

prod-deploy:
	@$(MAKE) deploy-check
	helm upgrade --install sis-blackboard-manager $(BBM_CHART) -n $(NAMESPACE) --create-namespace --wait --timeout 10m0s \
		-f $(BBM_ENV_FILE) \
		--set image.repository="$${IMAGE_REPOSITORY}" \
		--set image.tag="$${IMAGE_TAG}" \
		--set-string secrets.DB_PASSWORD="$${BBM_DB_PASSWORD}"

package-helm:
	@command -v helm >/dev/null 2>&1 || { echo "Missing required command: helm"; exit 1; }
	@test -d "$(BBM_CHART)" || { echo "Missing chart directory: $(BBM_CHART)"; exit 1; }
	helm package $(BBM_CHART)

test:
	mvn test

verify:
	mvn verify
