#!/bin/bash
set -euo pipefail
. "$(dirname "${BASH_SOURCE[0]}")/../tool_scripts/named-options.sh"
AVD_NAME=
SYSTEM_IMAGE=
DEVICE=
parse_named_options 'Usage: create-avd.sh --name <avd> --system-image <package> --device <device>' \
    'name:AVD_NAME system-image:SYSTEM_IMAGE device:DEVICE' "$@"
require_named_options AVD_NAME SYSTEM_IMAGE DEVICE

# Set emulator name and device parameters

# Detect OS and set command suffix
if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "win32" ]] || [[ -n "$WINDIR" ]]; then
    CMD_SUFFIX=".bat"
else
    CMD_SUFFIX=""
fi

# Set command names with appropriate suffix
SDKMANAGER="sdkmanager${CMD_SUFFIX}"
AVDMANAGER="avdmanager${CMD_SUFFIX}"
EMULATOR="emulator"

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check if required tools are installed
if ! command_exists "$SDKMANAGER" || ! command_exists "$AVDMANAGER" || ! command_exists "$EMULATOR"; then
    echo "Error: Android SDK tools ($SDKMANAGER, $AVDMANAGER, $EMULATOR) are not available in PATH."
    echo "Please ensure Android SDK is installed and environment variables are set correctly."
    exit 1
fi

# Check if the system image is installed
if ! $SDKMANAGER --list_installed | grep -q "$SYSTEM_IMAGE"; then
    echo "System image '$SYSTEM_IMAGE' is not installed. Downloading..."
    SDKMANAGER_PATH=$(command -v "$SDKMANAGER")
    if [[ $(stat -c '%U' "$SDKMANAGER_PATH") == "root" ]]; then
        echo "Detected root ownership for sdkmanager. executing with sudo..."
        if ! sudo "$SDKMANAGER_PATH" "$SYSTEM_IMAGE"; then
            echo "Error: Failed to download system image. Please check your SDK Manager."
            exit 1
        fi
    else
        if ! "$SDKMANAGER" "$SYSTEM_IMAGE"; then
            echo "Error: Failed to download system image. Please check your SDK Manager."
            exit 1
        fi
    fi
    echo "System image downloaded successfully."
else
    echo "System image is already installed. Skipping download."
fi

# Check if AVD already exists
echo "Checking if AVD '$AVD_NAME' exists..."
if $AVDMANAGER list avd | grep -q "$AVD_NAME"; then
    echo "AVD '$AVD_NAME' already exists. Skipping creation."
    exit 0
fi

# Create the new AVD
echo "Creating AVD '$AVD_NAME'..."
if ! echo no | $AVDMANAGER create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE" -d "$DEVICE"; then
    echo "Error: Failed to create AVD. Please check your SDK configuration."
    exit 1
fi
echo "AVD '$AVD_NAME' created successfully!"
