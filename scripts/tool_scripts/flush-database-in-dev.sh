#!/bin/bash
set -e
base=./deploy/dev-data

# 执行 Gradle 构建并捕获退出码
if ! ./gradlew cloud:cli:installDist -Pserver.flavor=dev -Pserver.buildType=dev; then
  echo "Gradle build failed"
  ./scripts/tool_scripts/show-notification.sh --title "构建失败" --message "cloud:cli:installDist 构建过程中出现错误" --success "false"
  exit 1
fi

. ./scripts/tool_scripts/set-log-path.sh

. ./scripts/tool_scripts/export-env.sh

cd cloud/cli
cli_path=build/install/cli/bin/cli
# 执行数据库刷新并捕获退出码
if ../../scripts/tool_scripts/flush-database.sh $cli_path "../../$base"; then
  echo "Database flush completed successfully"
  ../../scripts/tool_scripts/show-notification.sh --title "数据库刷新成功" --message "开发环境数据库已成功刷新" --success "true"
  exit 0
else
  echo "Database flush failed"
  ../../scripts/tool_scripts/show-notification.sh --title "数据库刷新失败" --message "开发环境数据库刷新过程中出现错误" --success "false"
  exit 1
fi
