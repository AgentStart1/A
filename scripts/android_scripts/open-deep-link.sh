#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "${BASH_SOURCE[0]}")/../tool_scripts/named-options.sh"
SCHEME=
HOST=
LINK_PATH=
parse_named_options 'Usage: open-deep-link.sh --scheme <flavor-build> --host <host> --path <path>' \
    'scheme:SCHEME host:HOST path:LINK_PATH' "$@"
require_named_options SCHEME HOST LINK_PATH

# 构造完整 URL
URI="alphaheart-${SCHEME}://${HOST}${LINK_PATH}"

echo "🚀 正在启动 Deep Link: ${URI}"
adb shell am start -a android.intent.action.VIEW -d "${URI}"
