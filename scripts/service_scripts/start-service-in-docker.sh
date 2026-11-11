#!/bin/bash
set -euo pipefail

. "$(dirname "${BASH_SOURCE[0]}")/service-env.sh"
resolve_service_env "${1:-}"
cd "$SERVICE_PROJECT_ROOT"
# Configuration-only refresh keeps existing Docker-built artifacts.
case "${2:-}" in
  --no-build)
    ./scripts/service_scripts/compose-service.sh --env "$ENV_FILE" --command 'up -d --no-build'
    exit 0
    ;;
  '') ;;
  *) echo "Unknown option: $2" >&2; exit 1 ;;
esac

echo "build and start in Docker"
export BUILD_ON=docker

IFS=',' read -ra PROFILES <<< "$(read_service_env COMPOSE_FILE_LIST)"

has_profile() {
  local target=$1
  local profile
  for profile in "${PROFILES[@]}"; do
    if [[ "$(echo "$profile" | xargs)" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

# Submit all targets together so BuildKit can reuse their common builder stages.
BUILD_SERVICES=()
for service in filesystem lucene cli worker server ws minio; do
  if has_profile "$service" || { [[ "$service" == "ws" ]] && has_profile server; }; then
    BUILD_SERVICES+=("$service")
  fi
done
if has_profile app; then
  BUILD_SERVICES+=(app-wasm panel-wasm)
fi

if [[ ${#BUILD_SERVICES[@]} -gt 0 ]]; then
  ./scripts/service_scripts/compose-service.sh --env "$ENV_FILE" --command "build ${BUILD_SERVICES[*]}"
fi

./scripts/service_scripts/compose-service.sh --env "$ENV_FILE" --command 'up -d --no-build'
