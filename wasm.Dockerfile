# syntax=docker/dockerfile:1.7@sha256:a57df69d0ea827fb7266491f2813635de6f17269be881f696fbfdf2d83dda33e

FROM eclipse-temurin:21-jdk@sha256:4d06038800655fe1211760cd561de70ef2ed7a47f5d69255e9834414602b7026 AS builder

RUN apt-get update && \
    apt-get install -y --no-install-recommends libatomic1 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

ARG FLAVOR
ARG BUILD_TYPE
ARG SERVER_URL
ARG WS_SERVER_URL

RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    --mount=type=cache,target=/app/.gradle,sharing=locked \
    --mount=type=cache,target=/app/app/webApp/build \
    --mount=type=cache,target=/app/panel/webApp/build \
    test -n "$FLAVOR" && \
    test -n "$BUILD_TYPE" && \
    test -n "$SERVER_URL" && \
    test -n "$WS_SERVER_URL" && \
    ./gradlew \
        :app:webApp:wasmJsBrowserDistribution \
        :panel:webApp:wasmJsBrowserDistribution \
        -Ptarget.wasm=true \
        -Pserver.flavor="$FLAVOR" \
        -Pserver.buildType="$BUILD_TYPE" \
        -Papp.server.url="$SERVER_URL" \
        -Papp.ws.server.url="$WS_SERVER_URL" \
        --console=plain \
        --no-daemon && \
    mkdir -p deploy/build/app-wasm deploy/build/panel-wasm && \
    cp -a app/webApp/build/dist/wasmJs/productionExecutable/. deploy/build/app-wasm/ && \
    cp -a panel/webApp/build/dist/wasmJs/productionExecutable/. deploy/build/panel-wasm/

# Entry URLs change with their contents so previously cached clients load the new Wasm bundle.
RUN for site in app-wasm panel-wasm; do \
        entry="deploy/build/$site/webApp.js"; \
        digest="$(sha256sum "$entry" | cut -c1-16)"; \
        resource_digest="$(find "deploy/build/$site/composeResources" -type f -exec sha256sum {} + | sort | sha256sum | cut -c1-16)"; \
        sed -i "s|</head>|<meta name=\"compose-resource-version\" content=\"$resource_digest\">\n</head>|" "deploy/build/$site/index.html" && \
        mv "$entry" "deploy/build/$site/webApp.$digest.js" && \
        sed -i "s/webApp\.js/webApp.$digest.js/g" "deploy/build/$site/index.html" && \
        find "deploy/build/$site" -type d -exec chmod 0755 {} + && \
        find "deploy/build/$site" -type f -exec chmod 0644 {} + || exit 1; \
    done

FROM nginx:1.27-alpine@sha256:65645c7bb6a0661892a8b03b89d0743208a18dd2f3f17a54ef4b76fb8e2f2a10 AS app-wasm

COPY --from=builder /app/deploy/build/app-wasm/ /usr/share/nginx/html/

FROM nginx:1.27-alpine@sha256:65645c7bb6a0661892a8b03b89d0743208a18dd2f3f17a54ef4b76fb8e2f2a10 AS panel-wasm

COPY --from=builder /app/deploy/build/panel-wasm/ /usr/share/nginx/html/
