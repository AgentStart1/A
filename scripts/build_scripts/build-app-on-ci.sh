#!/bin/bash
set -e
. "$(dirname "${BASH_SOURCE[0]}")/../tool_scripts/named-options.sh"
FLAVOR=
URL=
TARGET=
parse_named_options 'Usage: build-app-on-ci.sh --flavor <flavor> --url <API-host> --target <platform>' \
    'flavor:FLAVOR url:URL target:TARGET' "$@"
require_named_options FLAVOR URL TARGET

mkdir -p ~/.gradle
echo "gpr.user=$ORG_GRADLE_PROJECT_gpr_user" >> ~/.gradle/gradle.properties
echo "gpr.key=$ORG_GRADLE_PROJECT_gpr_key" >> ~/.gradle/gradle.properties

BUILD_TYPE="prod"

cat > "./deploy/${FLAVOR}.env" <<EOF
SERVER_URL=https://${URL}
WS_SERVER_URL=wss://${URL}
EOF

echo "$SECRETS_CONTEXT" | jq -r 'to_entries | .[] | "\(.key)=\(.value)"' | while IFS= read -r line; do
    # Ignore empty lines and comments
    [[ -z "$line" || "$line" =~ ^# ]] && continue
    IFS='=' read -r key value <<< "$line"

    echo "export $key=$value"
done > ./secrets_env.sh

# 然后在 shell 中执行：
. ./secrets_env.sh
mkdir -p app/config

restore_signing_key() {
    local sign_key="${storyteller_f_sign_key:-${STORYTELLER_F_SIGN_KEY:-}}"
    if [[ -z "$sign_key" ]]; then
        return
    fi

    local sign_path="$PWD/build/signing/signing_key.jks"
    mkdir -p "$(dirname "$sign_path")"
    printf '%s' "$sign_key" | base64 --decode > "$sign_path"
    export storyteller_f_sign_path="$sign_path"
}

case "$TARGET" in
    android)
        echo "Running Android-specific command..."
        restore_signing_key
        # 在这里添加 Android 相关命令
        ./gradlew app:androidApp:assembleRelease -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/apk/release"
        for f in app/androidApp/build/outputs/apk/release/*.apk; do
            cp "$f" "build/outputs/apk/release/app-${FLAVOR}_$(basename "$f")"
        done
        ;;
    desktop-msi)
        echo "Running DesktopMsi-specific command..."
        ./gradlew app:desktopApp:packageReleaseMsi -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/pkg/release"
        mv app/desktopApp/build/compose/binaries/main-release/msi/*.msi "build/outputs/pkg/release/app-$FLAVOR.msi"
        ;;
    desktop-deb)
        echo "Running DesktopDeb-specific command..."
        ./gradlew app:desktopApp:packageReleaseDeb -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/pkg/release"
        mv app/desktopApp/build/compose/binaries/main-release/deb/*.deb "build/outputs/pkg/release/app-$FLAVOR.deb"
        ;;
    desktop-dmg)
        echo "Running DesktopDmg-specific command..."
        ./gradlew app:desktopApp:packageReleaseDmg -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/pkg/release"
        mv app/desktopApp/build/compose/binaries/main-release/dmg/*.dmg "build/outputs/pkg/release/app-$FLAVOR.dmg"
        ;;
    android-panel)
        echo "Running AndroidPanel-specific command..."
        restore_signing_key
        # 在这里添加 AndroidPanel 相关命令
        ./gradlew panel:androidApp:assembleRelease -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/apk/release"
        for f in panel/androidApp/build/outputs/apk/release/*.apk; do
            cp "$f" "build/outputs/apk/release/panel-${FLAVOR}_$(basename "$f")"
        done
        ;;
    desktop-msi-panel)
        echo "Running DesktopMsiPanel-specific command..."
        ./gradlew panel:desktopApp:packageReleaseMsi -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/pkg/release"
        mv panel/desktopApp/build/compose/binaries/main-release/msi/*.msi "build/outputs/pkg/release/panel-$FLAVOR-panel.msi"
        ;;
    desktop-deb-panel)
        echo "Running DesktopDebPanel-specific command..."
        ./gradlew panel:desktopApp:packageReleaseDeb -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/pkg/release"
        mv panel/desktopApp/build/compose/binaries/main-release/deb/*.deb "build/outputs/pkg/release/panel-$FLAVOR-panel.deb"
        ;;
    desktop-dmg-panel)
        echo "Running DesktopDmgPanel-specific command..."
        ./gradlew panel:desktopApp:packageReleaseDmg -Pserver.flavor="$FLAVOR" -Pserver.buildType="$BUILD_TYPE"
        mkdir -p "build/outputs/pkg/release"
        mv panel/desktopApp/build/compose/binaries/main-release/dmg/*.dmg "build/outputs/pkg/release/panel-$FLAVOR-panel.dmg"
        ;;
    *)
        echo "Invalid target: $TARGET. Use 'android' or 'desktop-*'."
        ;;
esac
