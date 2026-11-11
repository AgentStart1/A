#!/usr/bin/env bash
FLAVOR=dev
# 检测操作系统
if [[ "$(uname -s)" =~ MINGW|CYGWIN|MSYS ]]; then
  FLAVOR=dev.win
fi

# 设置日志路径
source ./scripts/tool_scripts/set-log-path.sh

./scripts/service_scripts/compose-service.sh --env "$FLAVOR" --command 'up -d --build'