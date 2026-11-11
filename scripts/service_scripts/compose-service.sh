#!/bin/bash

set -euo pipefail

. "$(dirname "${BASH_SOURCE[0]}")/service-env.sh"
. "$(dirname "${BASH_SOURCE[0]}")/../tool_scripts/named-options.sh"
ENV_SELECTOR=
USE_PREBUILD=false
CUSTOM_COMMAND=
parse_named_options 'Usage: compose-service.sh --env <name-or-path> [--prebuild true|false] [--command "compose arguments"]' \
    'env:ENV_SELECTOR prebuild:USE_PREBUILD command:CUSTOM_COMMAND' "$@"
require_named_options ENV_SELECTOR
[[ $USE_PREBUILD == true || $USE_PREBUILD == false ]] || { echo '--prebuild must be true or false' >&2; exit 1; }
resolve_service_env "$ENV_SELECTOR"
cd "$SERVICE_PROJECT_ROOT"

# 默认 compose 命令
if [ -z "$CUSTOM_COMMAND" ]; then
    CUSTOM_COMMAND="up -d --build"
fi

IFS=',' read -ra COMPOSE_FILE_LIST <<< "$(read_service_env COMPOSE_FILE_LIST)"
echo "${COMPOSE_FILE_LIST[@]}"

# 公共 compose 文件
COMPOSE_FILES=("-f" "./deploy/docker-compose/docker-compose.yml")
NEEDS_BUILD_ON=false
ENABLED_PROFILES=()

has_profile() {
    local target=$1
    local profile
    for profile in "${ENABLED_PROFILES[@]}"; do
        if [[ "$profile" == "$target" ]]; then
            return 0
        fi
    done
    return 1
}

emit_backend_environment() {
    if has_profile filesystem || has_profile lucene; then
        echo "    environment:"
        if has_profile filesystem; then
            echo "      MEDIA_SERVICE: filesystem"
            echo "      FILESYSTEM_RPC_URL: ws://filesystem:8820/rpc"
            echo '      FILESYSTEM_PUBLIC_URL: ${FILESYSTEM_PUBLIC_URL:?set the externally reachable filesystem HTTP origin}'
        fi
        if has_profile lucene; then
            echo "      SEARCH_SERVICE: lucene"
            echo "      LUCENE_RPC_URL: ws://lucene:8821/rpc"
        fi
    fi
}

emit_bunker_network_if_needed() {
    if has_profile bunker; then
        echo "    networks:"
        echo "      - bw-services"
    fi
}

for profile in "${COMPOSE_FILE_LIST[@]}"; do
    profile=$(echo "$profile" | xargs)
    if [ -z "$profile" ]; then
        continue
    fi
    ENABLED_PROFILES+=("$profile")
    if [[ "$profile" == "server" ]] || [[ "$profile" == "worker" ]] || [[ "$profile" == "cli" ]] || \
        [[ "$profile" == "filesystem" ]] || [[ "$profile" == "lucene" ]]; then
        NEEDS_BUILD_ON=true
    fi
    COMPOSE_FILES+=("-f" "./deploy/docker-compose/docker-compose.${profile}.yml")
done

ENV_FILES=("--env-file" "$ENV_FILE")
if has_profile bunker; then
    resolve_bunker_env
    for required in SERVER_NAME ADMIN_USERNAME ADMIN_PASSWORD EMAIL_LETS_ENCRYPT; do
        if ! grep -qE "^${required}=.+$" "$BUNKER_ENV_FILE"; then
            echo "Bunker environment must define $required" >&2
            exit 1
        fi
    done
fi

GENERATED_COMPOSE_FILE="./deploy/docker-compose/docker-compose.generated-patch.yml"
{
    if ! has_profile bunker && ! has_profile cli && ! has_profile server && ! has_profile worker; then
        echo "services: {}"
    else
        echo "services:"
        if has_profile bunker; then
            echo "  bunkerweb:"
            echo "    env_file:"
            bunker_env_yaml=$(printf '%s' "$BUNKER_ENV_FILE" | sed "s/'/''/g")
            printf "      - path: '%s'\n" "$bunker_env_yaml"
            echo "        format: raw"
            if has_profile pg; then
                echo "  pg:"
                emit_bunker_network_if_needed
                echo "  adminer:"
                emit_bunker_network_if_needed
                echo "  pg-exporter:"
                emit_bunker_network_if_needed
            fi
            if has_profile minio; then
                echo "  minio:"
                emit_bunker_network_if_needed
            fi
            if has_profile server; then
                echo "  signin-redis:"
                emit_bunker_network_if_needed
            fi
            if has_profile filesystem; then
                echo "  filesystem:"
                emit_bunker_network_if_needed
            fi
            if has_profile lucene; then
                echo "  lucene:"
                emit_bunker_network_if_needed
            fi
            if has_profile grafana; then
                echo "  grafana:"
                emit_bunker_network_if_needed
                echo "  prometheus:"
                emit_bunker_network_if_needed
                echo "  loki:"
                emit_bunker_network_if_needed
                echo "  promtail:"
                emit_bunker_network_if_needed
            fi
            if has_profile elastic; then
                echo "  setup:"
                emit_bunker_network_if_needed
                echo "  es01:"
                emit_bunker_network_if_needed
                echo "  kibana:"
                emit_bunker_network_if_needed
            fi
            if has_profile dicebear; then
                echo "  dicebear:"
                emit_bunker_network_if_needed
            fi
            if has_profile etcd; then
                echo "  etcd:"
                emit_bunker_network_if_needed
            fi
            if has_profile app; then
                echo "  app-wasm:"
                emit_bunker_network_if_needed
                echo "  panel-wasm:"
                emit_bunker_network_if_needed
            fi
        fi
        if has_profile cli; then
            echo "  cli:"
            emit_backend_environment
            if has_profile pg || has_profile minio || has_profile elastic || has_profile filesystem || has_profile lucene; then
                echo "    depends_on:"
                if has_profile pg; then
                    echo "      pg:"
                    echo "        condition: service_healthy"
                fi
                if has_profile minio; then
                    echo "      minio:"
                    echo "        condition: service_healthy"
                fi
                if has_profile elastic; then
                    echo "      es01:"
                    echo "        condition: service_healthy"
                fi
                if has_profile filesystem; then
                    echo "      filesystem:"
                    echo "        condition: service_healthy"
                fi
                if has_profile lucene; then
                    echo "      lucene:"
                    echo "        condition: service_healthy"
                fi
            else
                echo "    # no generated dependencies"
            fi
            if has_profile elastic; then
                echo "    volumes:"
                echo "      - certs:/app/deploy/es_ca"
            fi
            emit_bunker_network_if_needed
        fi
        for service in server worker ws; do
            if has_profile "$service" || { [[ "$service" == "ws" ]] && has_profile server; }; then
                echo "  $service:"
                emit_backend_environment
                if has_profile cli; then
                    echo "    depends_on:"
                    echo "      cli:"
                    echo "        condition: service_healthy"
                    if has_profile pg; then
                        echo "      pg:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile minio; then
                        echo "      minio:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile elastic; then
                        echo "      es01:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile filesystem; then
                        echo "      filesystem:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile lucene; then
                        echo "      lucene:"
                        echo "        condition: service_healthy"
                    fi
                elif has_profile pg || has_profile minio || has_profile elastic || has_profile filesystem || has_profile lucene; then
                    echo "    depends_on:"
                    if has_profile pg; then
                        echo "      pg:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile minio; then
                        echo "      minio:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile elastic; then
                        echo "      es01:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile filesystem; then
                        echo "      filesystem:"
                        echo "        condition: service_healthy"
                    fi
                    if has_profile lucene; then
                        echo "      lucene:"
                        echo "        condition: service_healthy"
                    fi
                fi
                emit_bunker_network_if_needed
                if has_profile elastic; then
                    echo "    volumes:"
                    echo "      - certs:/app/deploy/es_ca"
                fi
            fi
        done
    fi
    if has_profile bunker; then
        echo "networks:"
        echo "  bw-services:"
        echo "    name: bw-services"
    fi
} > "$GENERATED_COMPOSE_FILE"
COMPOSE_FILES+=("-f" "$GENERATED_COMPOSE_FILE")

IFS=' ' read -r -a custom_cmd_parts <<< "$CUSTOM_COMMAND"

# Bind-mounted local backends run as UID 1000. Create their data directories
# before Docker does, otherwise Docker creates missing paths as root:root.
if has_profile filesystem; then
    mkdir -p ./deploy/a_file_data
fi
if has_profile lucene; then
    mkdir -p ./deploy/lucene_data
fi
# 条件：USE_PREBUILD = true
if [ "$USE_PREBUILD" = "true" ]; then
    COMPOSE_FILES+=("-f" "./deploy/docker-compose/docker-compose.prebuild.yml")
else
  if [[ "$NEEDS_BUILD_ON" == "true" ]]; then
      BUILD_ON=${BUILD_ON:-$(read_service_env BUILD_ON)}
      if [ -z "$BUILD_ON" ]; then
          echo "BUILD_ON must be set."
          exit 1
      fi
      export BUILD_ON=$BUILD_ON
  fi
fi

CMD=("docker" "compose" "${ENV_FILES[@]}" "${COMPOSE_FILES[@]}" "${custom_cmd_parts[@]}")

echo "Executing: ${CMD[@]}"
# "${CMD[@]}"

docker compose "${ENV_FILES[@]}" "${COMPOSE_FILES[@]}" "${custom_cmd_parts[@]}"

# Nginx resolves named upstreams when loading its configuration. Recreated services
# can have different addresses even when the proxy itself did not change.
if [[ "${custom_cmd_parts[0]:-}" == "up" ]] && has_profile bunker; then
    docker compose "${ENV_FILES[@]}" "${COMPOSE_FILES[@]}" exec -T bunkerweb nginx -s reload
fi
