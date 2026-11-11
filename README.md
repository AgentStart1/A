# A

面向 Android、Desktop、CLI 和 Web 的 Kotlin Multiplatform 应用，配套云服务和管理面板。

构建、测试和独立服务配置说明见 [DEVELOPMENT.md](DEVELOPMENT.md)。

[下载最新 alpha 构建](https://nightly.link/storytellerF/A/workflows/alpha/alpha?preview)

## 大屏首页

首页将世界、社区和房间保留为本地分区。在较大窗口中，世界采用宽度受限的单一阅读流，包含已加入社区预览；社区显示更多列；房间展示更多已有元数据。社区与成员搜索结果的列数根据可用内容区域调整，不受输入框影响。

调整窗口尺寸时保留已选分区和阅读状态。宽屏列表提供明确的 Refresh 操作。社区和用户详情分区在缩放时保留选择；房间和话题详情将阅读内容与消息输入对齐在宽度受限的一列中。

设置和创建表单在宽窗口扩展为两列。收藏、订阅、评论和回应历史采用自适应网格。空间允许时，话题编辑器在原始或块编辑器旁显示实时预览，并在窗口缩放时保留草稿。

已加入房间的行显示最新消息预览、作者和时间，以及精确未读消息数。房间导航徽标显示所有已加入房间的未读消息总数。宽屏世界的上下文区域提供数量受限的未读房间与已加入社区置顶话题预览，各自独立刷新，并有加载和错误状态。不可用私信显示中性的不可用标签；预览仅保留在内存中，不保存为明文。

## 浏览器文件上传

在 File Explorer 中选择 Upload 并选择文件。大文件上传支持暂停、继续和中止。浏览器进度表示服务器已确认的分块。尚未完成时保持页面打开：重新加载会释放浏览器对所选源文件的访问。已完成上传记录在重载后保留。

## 文件预览与下载

在 File Explorer 打开已上传文件，或打开话题附件，可在独立页面查看。图片、音频、视频和 PDF 支持预览。使用 Download，通过设备保存对话框或浏览器下载管理器保存副本。其他文件类型也可在同一页面下载。Uploaded 列表只显示已完成上传。

## 存储与搜索可用性

文件系统或 Lucene 服务重启后，后续请求自动重连。被中断请求仍报告失败；失败操作和部分上传不会自动重放。

## PDF 快照

PDF 生成使用服务器或桌面系统已安装字体，应用不内置 PDF 字体文件。中文安装 Noto Serif SC/CJK SC，代码安装受支持等宽字体。静态 Regular/Bold 字体优先，缺失字重由 TrueType 或 CFF2 可变字体补充。生成 PDF 会嵌入所选字体，读者无需安装。

快照使用 A4 阅读布局，包含来源面板、有间距的标题和段落，以及带页码的内容页。签名快照保留独立签名页。发布和捕获时间，以及签名页发布时间戳，都以可读日期时间格式明确显示 `UTC+00:00`。导出当前接受纯文本话题；不保留图片、行内公式和 HTML，图表或公式代码块仍保留为源码。列表、表格和引用链接的格式存在限制；将快照用作完整归档前，先查看 [PDF 内容覆盖](DEVELOPMENT.md#pdf-快照布局与内容覆盖)。部署选项见 [PDF 字体配置](DEVELOPMENT.md#pdf-字体来源与可变字体实例化)。

## Worker 配置

Worker 任务设置按任务类型保存。任务没有持久化配置时不会运行。CLI `add` 命令接受 `taskConfig` 预设，其中 `taskConfigData` 条目包含 `type`、`isEnabled`、`fetchSize` 和 `waitDurationMillis`，见 `deploy/dev-data/7_preset_task_config.json`。管理员可从 Panel 的 **Worker task configurations** 页面编辑相同值。Worker 每轮迭代前重新加载设置，启用、禁用或调整任务无需重启。

### 话题审核

持久化的 `TOPIC_MODERATION` 任务配置启用后，worker 从后端存储加载当前 LLM 配置。通过 Cloud CLI 的 `add` 命令应用 `cloud/worker/llm-config.*-example.json` 预设。OpenRouter 需在 `cloud/worker/llm-config.openrouter-example.json` 设置有效 API 密钥和模型标识；其官方 Koog 客户端使用 OpenRouter 默认 API 端点，忽略 `baseUrl`。LiteRT-LM 需在应用 `cloud/worker/llm-config.litert-example.json` 前，将 `modelPath` 指向已有本地 `.litertlm` 模型；worker 不下载模型。

OpenAI、Anthropic、Google、Ollama、Bedrock、DeepSeek、Mistral AI 和 DashScope 要求使用 Koog 模型目录中精确、区分大小写的 ID。未知 ID、目录未列出的带日期版本和别名都会被拒绝。默认模型 ID 为：

| 提供方 | 默认模型 ID |
| --- | --- |
| `OPENAI` | `gpt-4o` |
| `ANTHROPIC` | `claude-sonnet-4-0` |
| `GOOGLE` | `gemini-2.5-flash` |
| `OLLAMA` | `llama3.2:latest` |
| `BEDROCK` | `openai.gpt-oss-120b-1:0` |
| `DEEPSEEK` | `deepseek-v4-flash` |
| `MISTRAL_AI` | `mistral-medium-latest` |
| `DASHSCOPE` | `qwen-plus` |

OpenRouter 要求明确、非空白的 ID，原样接受 Koog 目录之外的模型，采用路由服务的模型支持信息，而不猜测上下文或输出 token 上限。

Google 使用原生 Gemini API，默认 API 根地址为 `https://generativelanguage.googleapis.com`。自定义 Google 端点时，将 `baseUrl` 设置为原生 API 根地址，客户端追加 `v1beta/models/{id}:generateContent`。已有 Google 配置指向 OpenAI 兼容 `/v1beta/openai` 端点时，必须切换到原生根地址，或改用 `OPENAI_COMPATIBLE` 保留该协议。

OpenAI 兼容模型 ID 作为不透明字符串直接传给 API，不查 OpenAI 目录，也不推断元数据；已有默认值为 `gpt-3.5-turbo`。直接提供方配置中，未列出 ID 应替换为精确目录 ID。采用兼容 HTTP 协议的自定义部署，可使用 `OPENAI_COMPATIBLE` 配置其端点和模型名。LiteRT 保留本地模型 ID。

DeepSeek、Mistral AI 和 DashScope 使用 `apiKey`，可通过 `baseUrl` 指定原生 API 根地址；DashScope 默认国际端点。使用对应 `llm-config.*-example.json` 预设。Bedrock 使用原生 AWS Converse API。可选 `region` 默认 Koog SDK 的 `us-west-2`，`baseUrl` 可覆盖 runtime 端点。没有 `apiKey` 时，AWS 凭据来自 SDK 默认链，例如 workload role、环境或共享 profile。也可在 `apiKey` 中放 Bedrock bearer API key，它优先于可用 AWS 凭据。不得将 AWS access/secret key 放入该字段。Bedrock 预设不含凭据，便于运行时提供 workload 凭据。

所有提供方返回相同结构化审核决定。LiteRT-LM 客户端使用原生 JSON-schema 约束输出，优先尝试 GPU 后端，GPU 初始化失败时回退 CPU。将 LiteRT-LM 预设的 `cachePath` 指向 worker 可写目录。已有配置缺少 `cachePath` 时，使用 JVM 临时目录下按模型划分的目录。

LiteRT-LM 预构建 Linux x86_64 库要求 AVX。宿主无法运行所选提供方时，禁用持久化 `TOPIC_MODERATION` 任务配置；此时不审核话题内容，也不会自动将有害话题作者标记为 `READ_ONLY`。

审核覆盖社区、用户空间和属于社区的公开房间中的话题与评论。加密私密房间和通知房间话题不在审核范围。辱骂性脏话、威胁、血腥暴力、色情、仇恨、自残、剥削或鼓吹违法活动等有害内容，会使作者账户被标记为 `READ_ONLY`。

### 为 LiteRT-LM 配置 Windows GPU DLL

Windows GPU 推理需要 Microsoft [DirectX Shader Compiler](https://github.com/microsoft/DirectXShaderCompiler/releases) 的 64 位 `dxil.dll` 和 `dxcompiler.dll`。`com.google.ai.edge.litertlm:litertlm-jvm` 不包含这些 DLL。LiteRT-LM 通过原生 Direct3D 12 运行时加载它们，因此当前 worker 仅将目录加入 `PATH` 还不够。将两个 DLL 放在实际启动 worker 的 `java.exe` 旁。

以下 PowerShell 示例安装 worker 部署当前固定的 DXC 版本。尽量使用应用自身管理的 Java 21 运行时，避免修改共享系统 JDK。运行前将 `JAVA_HOME` 设置为该运行时。

```powershell
$dxcArchive = Join-Path $PWD "dxc_2026_07_29.zip"
$dxcDirectory = Join-Path $PWD "dxc-v1.9.2607"
$expectedDxcHash = "A1DFB116BA3EEAE6A1582291B53A8E7BF65AD760676BD3194685C8F7367CD241"

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    throw "Set JAVA_HOME to the Java 21 runtime used by the worker"
}

$workerJavaDirectory = Join-Path $env:JAVA_HOME "bin"
$workerJavaExecutable = Join-Path $workerJavaDirectory "java.exe"
$dxilDestination = Join-Path $workerJavaDirectory "dxil.dll"
$dxCompilerDestination = Join-Path $workerJavaDirectory "dxcompiler.dll"

if (-not (Test-Path -LiteralPath $workerJavaExecutable)) {
    throw "Worker Java executable not found: $workerJavaExecutable"
}
if (Test-Path -LiteralPath $dxcDirectory) {
    throw "DXC extraction directory already exists: $dxcDirectory"
}
if ((Test-Path -LiteralPath $dxilDestination) -or (Test-Path -LiteralPath $dxCompilerDestination)) {
    throw "DXC DLLs already exist in $workerJavaDirectory; verify or back them up before replacing them"
}

Invoke-WebRequest `
    -Uri "https://github.com/microsoft/DirectXShaderCompiler/releases/download/v1.9.2607/dxc_2026_07_29.zip" `
    -OutFile $dxcArchive

$actualDxcHash = (Get-FileHash -LiteralPath $dxcArchive -Algorithm SHA256).Hash
if ($actualDxcHash -ne $expectedDxcHash) {
    throw "DXC SHA-256 mismatch: $actualDxcHash"
}

Expand-Archive -LiteralPath $dxcArchive -DestinationPath $dxcDirectory
Copy-Item -LiteralPath (Join-Path $dxcDirectory "bin\x64\dxil.dll") -Destination $dxilDestination
Copy-Item -LiteralPath (Join-Path $dxcDirectory "bin\x64\dxcompiler.dll") -Destination $dxCompilerDestination
Get-Item -LiteralPath $workerJavaExecutable, $dxilDestination, $dxCompilerDestination
```

安装 DLL 后重启 worker。GPU 启动成功会记录以下两条消息：

```text
Initializing LiteRT LLM client with GPU backend
Initialized LiteRT LLM client with GPU backend
```

GPU 初始化失败时，worker 仍可用，在初始化新的 CPU 引擎前记录回退：

```text
LiteRT GPU initialization failed; falling back to CPU
Initialized LiteRT LLM client with CPU backend
```

缺少 `libLiteRtTopKWebGpuSampler.dll` 的警告涉及 LiteRT-LM 可选 WebGPU sampler。版本 `0.16.1` 的 JVM artifact 不附带该 DLL，而使用内置 C sampler；此警告本身不表示主模型已从 GPU 回退。以上后端初始化消息才是判断依据。

## 构建与运行

### 环境要求

- Java 21（Eclipse Temurin）。
- 仓库内置 Gradle wrapper（`./gradlew`）。
- Windows 使用 Git Bash 运行仓库 shell 脚本。
- 本地服务栈需要支持 Compose 的 Docker。
- 桌面窗口使用 Nucleus Tao。AsciiDoc 预览使用系统 WebView 引擎：Windows 为 WebView2，macOS 为 WKWebView，Linux 为 WebKitGTK。
- 桌面音视频播放需要 VLC，并使用其原生帧回调。

### 构建命令

```bash
# Assemble included modules
./gradlew assemble -Pserver.flavor=sample -Pserver.buildType=prod

# Build the Cloud server
./gradlew :cloud:server:assemble -Pserver.flavor=sample -Pserver.buildType=prod

# Build the Android app
./gradlew :app:androidApp:assembleDebug -Pserver.flavor=sample -Pserver.buildType=prod

# Build the Desktop app
./gradlew :app:desktopApp:packageDistributionForCurrentOS -Pserver.flavor=sample -Pserver.buildType=prod
```

### 包含 Wasm 应用的 Compose 栈

在 flavor 的 `COMPOSE_FILE_LIST` 中添加 `app`，将用户应用和管理面板部署为 Wasm 站点。Compose 在 `wasm.Dockerfile` 的 JDK 21 builder 阶段构建两个站点，只通过构建参数传入必需 flavor、build type 和服务器 URL，再将各发行产物复制到独立 Nginx 运行镜像。部署在 Docker 内编译后端和 Wasm 发行产物，宿主无需本地 JDK 或预构建产物。启动前顺序构建镜像：

```bash
./scripts/service_scripts/start-service-in-docker.sh sample
```

使用已有 Docker 构建镜像刷新配置时，运行 `./scripts/service_scripts/start-service-in-docker.sh sample --no-build`。

容器健康后，在 `http://localhost:8080` 打开用户应用，在 `http://localhost:8081` 打开面板。省略 `app` 可跳过两个 Wasm 构建及服务。

`compose-service.sh` 和 `start-service-in-docker.sh` 都接受环境名称（`alpha` 或 `alpha.env`）或文件路径（`~/deploy/alpha.env`）。名称先解析为 `~/deploy/<name>.env`，再尝试 `deploy/<name>.env`。以 `deploy/sample.env` 为参考，必须设置 `BUILD_TYPE`，`COMPOSE_FILE_LIST` 选择栈中服务。Server、Worker、WebSocket server 和 CLI 镜像共用根 `Dockerfile` 的 JDK 21 builder，分别打包为 `server`、`worker`、`ws` 和 `cli` target。

`COMPOSE_FILE_LIST` 包含 `bunker` 时，将 `deploy/bunker.env.example` 复制到项目之外的 `~/deploy/bunker.env`，替换示例域名和凭据。在所选应用 env 文件设置 `BUNKER_ENV_FILE=/home/<user>/deploy/bunker.env` 可改用其他路径；相对路径相对于该 env 文件解析，shell 覆盖优先。此文件只通过 `env_file` 注入 BunkerWeb 容器，不参与 Compose 插值，也不覆盖 flavor 配置。要求 Docker Compose 2.30+ 的 raw env-file 格式，按字面保留凭据而不插值。它使用最终 BunkerWeb 名称，如 `SERVER_NAME`、`ADMIN_USERNAME`、`ADMIN_PASSWORD` 和 `api.example.com_REVERSE_PROXY_HOST`，而非旧 `BUNKER_*` 变量。

文件系统存储中，将 `FILESYSTEM_PUBLIC_URL` 设置为外部可达媒体 origin（例如 `https://media.example.com`），`FILESYSTEM_EXPOSE_PORT` 设置为发布的 HTTP 端口（默认 `8822`）。客户端直接从该服务下载 `/objects/<bucket>/<name>`。使用 BunkerWeb 时，媒体域名将此路径转发到 filesystem 端口 8822；8820 是内部管理 RPC 端点，必须保持私有。包括 BunkerWeb 部署在内，都在 `deploy/{flavor}.env` 设置 `FILESYSTEM_PUBLIC_URL=https://<media-domain>`。启动脚本向 filesystem 和应用服务传递相同解析后的 origin。

`PRESET_PATH=/home/<user>/data` 将宿主预设目录绑定挂载到 Server 和 CLI 的 `/app/deploy/preset_data`。CLI 从中读取编号 JSON 预设及引用的本地文件。仅在有意初始化数据时启用 `CLI_INIT_ENABLE=true`，完成后禁用。

## 许可证

这是私有项目，保留所有权利。

### 登录会话

在 App 账户菜单或 Panel 导航抽屉中使用 **Sign out** 结束当前会话。退出后刷新仍保持未登录，保存的账户历史可用于明确发起登录。

HTTP 和 WebSocket 认证使用由 Redis 支持的 HttpOnly 会话 Cookie。连续 30 天没有用户活动，或登录满 90 天后，会话过期。后台刷新、心跳和收到通知不会延长登录。过期或撤销会话要求明确重新登录。浏览器客户端支持一个主账户，原生客户端仍可切换子账户。

服务器 Compose profile 包含启用 AOF 持久化的私有 Redis 存储，不暴露宿主端口。在应用 flavor 环境中将 `SIGNIN_ALLOWED_ORIGINS` 设置为逗号分隔的精确浏览器 origin，例如 `https://app.example.com,https://panel.example.com`。API 和 `/ws` 必须共享 host。可通过 `SIGNIN_REDIS_ENDPOINT` 选择外部 Redis。Redis 失败会阻止认证，直到恢复；临时故障期间，本地保留凭据。

原生 GUI 客户端通过 Android Keystore、Windows DPAPI、macOS Keychain 或 Linux Secret Service（libsecret 0.19+ 且 keyring 已解锁）保护保存的会话 Cookie。保护存储不可用时，Cookie 持久化可以保留内存凭据；保存账户密钥则必须有系统安全存储，没有明文回退。保存的私钥和账户历史也使用保护存储。浏览器密钥历史在 IndexedDB 中通过不可导出的 WebCrypto key 加密；浏览器登录 Cookie 仍为 HttpOnly。忽略旧 Settings 登录历史，升级后重新导入账户私钥。本次迁移会使旧加密会话 Cookie 失效，升级后需登录一次。

只读账户可以登录和浏览，但不能发布修改。系统通知房间允许接收者读取消息，其消息输入已禁用。

快照 PDF 签名接受所选环境文件中的 `SNAPSHOT_KEYSTORE_BASE64`（已有 PKCS#12 文件的单行 Base64）和 `SNAPSHOT_KEYSTORE_PASS`。服务器在内存验证和使用密钥，无需挂载 keystore。Base64 是编码而非加密：保护环境文件，不得提交或记录内容。`SNAPSHOT_KEYSTORE_BASE64` 优先于旧 `SNAPSHOT_KEYSTORE_PATH`；数据格式错误、密码错误或缺失会使启动失败，不回退，也不生成替代密钥。迁移时，通过 `base64 -w 0 < existing.p12` 编码已有文件，私下将输出保存到环境文件，保留密码，并移除 `SNAPSHOT_KEYSTORE_PATH`。两种来源都未配置时，禁用快照签名。

## 房间通话

Android、桌面和浏览器客户端共用房间通话页。打开房间通话操作，选择 **Join with video** 或 **Join with audio**，并允许请求的权限。参与者显示在可滚动自适应网格中，分别显示连接、相机和静音状态。控件支持麦克风静音、隐藏视频、切换受支持摄像头或离开。关闭通话页即结束通话。浏览器通话要求 HTTPS 及相机、麦克风权限。CLI 不提供音视频通话。

iOS 适配器和权限已包含，构建和设备验收需要 macOS/Xcode。

桌面端到端测试使用平台专用 Appium 驱动。macOS 测试要求合格硬件渲染器，或对已观察到的 Skiko 软件渲染停顿有经过验证的修复。加速的 Intel macOS 15.8.1 目标已通过渲染器资格验证，可恢复桌面 E2E。资格证据和当前验收结果见[开发指南](DEVELOPMENT.md#macos-e2e-暂停与-skiko-调查)。

远程桌面 E2E 在目标环境运行应用，Gradle 测试、后端夹具和编译留在宿主。Windows 和 Linux 环境配置共享同一 JVM artifact/service 约定及项目自身桌面依赖要求，连接信息属于操作者。在工作虚拟机内编译需要用户明确请求；仅配置 VM/Appium 或请求测试并不允许在其中编译。

## 实验性 Web 媒体播放

Wasm 实验在 Compose 内绘制视频，让字幕、按钮和对话框可叠加显示，并通过 Web Audio 播放声音。要求 HTTPS 或 localhost、浏览器支持文件编码，以及跨源媒体的 CORS 许可。默认像素复制路径在高分辨率下成本可能较高。可选 GPU 纹理实验避免显式像素回读，但不保证零拷贝解码。支持有限长度、可 seek 文件，标签页隐藏时暂停；不支持的媒体显示错误。隔离实验和限制见 DEVELOPMENT.md。
