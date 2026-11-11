#!/bin/sh
set -e

# Parsing Arguments
RUN_ANDROID=false
RUN_DESKTOP=false
RUN_E2E=false
E2E_COVERAGE=false
RUN_COMPOSE=false
MODULE="app:composeApp"
TEST_FILTERS=""
GRADLE_CONSOLE_ARGS=""
RUN_ALL=false

appendTestFilter() {
  if [ -z "$TEST_FILTERS" ]; then
    TEST_FILTERS="$1"
  else
    TEST_FILTERS="$TEST_FILTERS
$1"
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --all) RUN_ALL=true; shift ;;
    --android) RUN_ANDROID=true; shift ;;
    --desktop) RUN_DESKTOP=true; shift ;;
    --e2e) RUN_E2E=true; shift ;;
    --coverage) E2E_COVERAGE=true; shift ;;
    --compose) RUN_COMPOSE=true; shift ;;
    --plain) GRADLE_CONSOLE_ARGS="--console=plain"; shift ;;
    --module)
      [ -z "$2" ] && { echo "--module requires a value"; exit 1; }
      MODULE="$2"
      shift 2
      ;;
    --tests)
      shift
      test_count=0
      while [ "$#" -gt 0 ]; do
        case "$1" in
          --*) break ;;
        esac
        [ -z "$1" ] && { echo "--tests requires non-empty values"; exit 1; }
        appendTestFilter "$1"
        test_count=$((test_count + 1))
        shift
      done
      [ "$test_count" -eq 0 ] && { echo "--tests requires at least one value"; exit 1; }
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

runGradle() {
  if [ -n "$GRADLE_CONSOLE_ARGS" ]; then
    set -- "$@" "$GRADLE_CONSOLE_ARGS"
  fi
  if [ "$E2E_COVERAGE" = true ]; then
    set -- "$@" -Pe2e.coverage=true
  fi
  ./gradlew "$@" -Pserver.flavor=dev -Pserver.buildType=debug
}

runGradleWithTests() {
  if [ -n "$TEST_FILTERS" ]; then
    original_ifs=$IFS
    case $- in
      *f*) restore_globbing=false ;;
      *) restore_globbing=true; set -f ;;
    esac
    arguments_initialized=false
    for gradle_argument do
      if [ "$arguments_initialized" = false ]; then
        set --
        arguments_initialized=true
      fi
      set -- "$@" "$gradle_argument"
      case "$gradle_argument" in
        --*) continue ;;
      esac
      IFS='
'
      for test_filter in $TEST_FILTERS; do
        set -- "$@" --tests "$test_filter"
      done
      IFS=$original_ifs
    done
    IFS=$original_ifs
    [ "$restore_globbing" = true ] && set +f
  fi
  runGradle "$@"
}

if [ "$RUN_ALL" = true ]; then
  RUN_ANDROID=true
  RUN_DESKTOP=true
  RUN_E2E=true
  RUN_COMPOSE=true
fi

if [ "$E2E_COVERAGE" = true ] && [ "$RUN_E2E" != true ]; then
  echo "--coverage requires --e2e or --all"
  exit 1
fi

showNotification() {
    title="$1"
    message="$2"
    success="$3"
    if [ -x ./scripts/tool_scripts/show-notification.sh ]; then
        ./scripts/tool_scripts/show-notification.sh --title "$title" --message "$message" --success "$success"
    fi
}

isVmwareEnvironment() {
    if command -v systemd-detect-virt >/dev/null 2>&1 && systemd-detect-virt --quiet --vm && [ "$(systemd-detect-virt 2>/dev/null)" = "vmware" ]; then
        return 0
    fi

    if [ -r /sys/class/dmi/id/product_name ] && grep -qi 'vmware' /sys/class/dmi/id/product_name; then
        return 0
    fi

    return 1
}

vmwareHostIp() {
    if ! command -v ip >/dev/null 2>&1; then
        return 1
    fi

    local_ip=$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{ for (i = 1; i <= NF; i++) if ($i == "src") { print $(i + 1); exit } }')
    [ -z "$local_ip" ] && local_ip=$(ip -4 addr show scope global 2>/dev/null | awk '/inet / { sub(/\/.*/, "", $2); print $2; exit }')
    [ -z "$local_ip" ] && return 1

    echo "$local_ip" | awk -F. 'NF == 4 { print $1 "." $2 "." $3 ".1" }'
}

findConnectedDeviceSerials() {
    adb devices | awk 'NR > 1 && $2 == "device" { print $1 }'
}

tryConnectHostEmulator() {
    host=$(vmwareHostIp)
    if [ -z "$host" ]; then
        echo "VMware environment detected, but failed to derive host IP from local IPv4 address."
        return 1
    fi

    echo "No Android device connected. VMware environment detected; trying host emulator via adb connect $host..."

    target="$host:5555"
    connect_output=$(adb connect "$target" 2>&1 || true)
    echo "adb connect $target: $connect_output"

    device_serials=$(findConnectedDeviceSerials)
    [ -n "$device_serials" ]
}

checkAndroidDeviceReady() {
    if ! command -v adb >/dev/null 2>&1; then
        echo "adb is not available. Connect a booted Android device before running Android/Appium tests."
        exit 1
    fi

    device_serials=$(findConnectedDeviceSerials)

    if [ -z "$device_serials" ]; then
        if isVmwareEnvironment && tryConnectHostEmulator; then
            device_serials=$(findConnectedDeviceSerials)
        fi

        if [ -z "$device_serials" ]; then
            echo "No connected Android device found. Connect a booted phone or emulator before running Android/Appium tests."
            if isVmwareEnvironment; then
                echo "VMware environment detected, but no host emulator accepted adb connections."
            fi
            exit 1
        fi
    fi

    for device_serial in $device_serials; do
        boot_completed=$(adb -s "$device_serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        if [ "$boot_completed" = "1" ]; then
            echo "Android device is ready: $device_serial"
            return 0
        fi
    done

    echo "Connected Android devices have not completed boot. Wait until sys.boot_completed=1 before running Android/Appium tests."
    exit 1
}

# Check emulator readiness (for Android and end-to-end tests)
if [ "$RUN_ANDROID" = true ] || [ "$RUN_E2E" = true ]; then
    checkAndroidDeviceReady
fi

echo "Running detekt..."
if ! runGradle detekt; then
    showNotification "Detekt 失败" "代码静态分析失败！请检查代码规范问题。" "false"
    exit 1
fi

if [ "$RUN_COMPOSE" = true ]; then
    echo "Running Shell regression tests..."
    sh ./scripts/test_scripts/run-shell-tests.sh
fi

# Running android Tests
if [ "$RUN_ANDROID" = true ]; then
    echo "Running Android Connected Tests..."
    runGradleWithTests "${MODULE}:connectedAndroidTest"
fi

# Running desktop Tests
if [ "$RUN_DESKTOP" = true ]; then
    echo "Running Desktop Tests..."
    runGradleWithTests "${MODULE}:desktopTest"
fi

# Running End-to-End Tests
if [ "$RUN_E2E" = true ]; then
    echo "Running Appium and CLI End-to-End Tests..."
    rm -rf ./app/androidAppium/build/test/appium/sessions
    rm -rf ./app/desktopAppium/build/test/appium/sessions
    rm -rf ./panel/androidAppium/build/test/appium/sessions
    rm -rf ./panel/desktopAppium/build/test/appium/sessions
    rm -rf ./app/cliE2e/build/test/e2e/sessions
    rm -rf ./panel/cliE2e/build/test/e2e/sessions
    set -- \
        :app:androidAppium:appiumTest \
        :app:desktopAppium:appiumTest \
        :app:wasmAppium:appiumTest \
        :panel:androidAppium:appiumTest \
        :panel:desktopAppium:appiumTest \
        :panel:wasmAppium:appiumTest \
        :app:cliE2e:e2eTest \
        :panel:cliE2e:e2eTest
    case "${APPIUM_DESKTOP_PLATFORM:-$(uname -s)}" in
        [Ll][Ii][Nn][Uu][Xx]) set -- "$@" :app:linuxAppium:appiumTest ;;
    esac
    runGradleWithTests "$@" --continue
fi

#./gradlew :composeApp:wasmJsTest
#./gradlew :composeApp:iosSimulatorArm64Test
