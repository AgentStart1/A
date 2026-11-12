# 开发指南

## 构建验证

常规代码验证先编译受影响模块和平台 source set，再执行 Detekt 与相关测试。根 `assemble` 是完整产物构建，不是仅编译检查：它可能涉及无关模块、Android Benchmark/Release 打包，以及 Wasm 生产链接和 Binaryen 优化。用户要求相应产物，或受影响的打包、优化、签名或分发约定需要验证时，才使用这些任务。

验证全仓库编译时，使用已有 `compileAllNoRelease` 任务：

```sh
./gradlew compileAllNoRelease -Pserver.flavor=dev -Pserver.buildType=dev --console=plain
```

此任务排除可执行文件、Release、Benchmark 和测试编译变体。无需全仓库检查时，优先选择更窄的模块任务。

修改应用共享 Kotlin 代码时，将以下命令作为一行执行：

```sh
./gradlew :app:composeApp:compileKotlinJvm :app:composeApp:compileAndroidMain :app:composeApp:compileKotlinWasmJs -Pserver.flavor=dev -Pserver.buildType=dev --console=plain
```

Panel 改动使用对应的 `:panel:composeApp` 任务，JVM 后端模块使用 `:module:compileKotlin`。修改共享模型或协议类型时，纳入受影响的下游使用者。Wasm `compileKotlinWasmJs` 只检查源码编译，不链接生产可执行文件；运行时和浏览器行为仍需相关浏览器测试。需要 Android APK 时，选择受影响的 `assembleDebug` 任务。只有上述产物检查需要时，才运行生产链接、release 打包或完整根 assemble。

Windows 上，如果编译或构建、Detekt、截图及宿主 UI 运行共用 Android 运行时 JAR，就必须顺序执行。始终显式传入所需 server flavor 和 build type。

## 按场景划分的 API 投影

基础模型（`TopicInfo`、`TitleInfo`、`CommunityInfo`、收藏／订阅／日志记录及 `QuotaInfo`）只包含自身数据。关联对象与基础对象直接并列，放入可序列化的场景投影；不得添加 `extension`/`extensions` 集合，也不得将基础对象属性展开到 JSON 包装层。

| 场景 | 响应项 | 基础对象字段 | 关联字段 |
| --- | --- | --- | --- |
| 主题列表与详情 | `TopicDetail` | `topicInfo` | `authorInfo`、`subTopics`、`reactions`、`roomInfo` |
| 嵌套回复预览 | `TopicReplyPreview` | `topicInfo` | `authorInfo` |
| 主题创建与 `RoomFrame.NewTopicInfo` | `TopicCreated` | `topicInfo` | `authorInfo` |
| 房间活动、已加入范围的置顶主题及已评论主题 | `TopicActivity` | `topicInfo` | `authorInfo`、`reactions`、`roomInfo` |
| 头衔列表与详情 | `TitleDetail` | `titleInfo` | 创建者、接收者、描述主题和作用范围对象 |
| 社区搜索与用户成员关系列表 | `CommunityMembershipItem` | `communityInfo` | `targetMemberInfo` |
| 收藏 | `UserFavoriteItem` | `favoriteInfo` | 由记录的 `objectType` 选择的对象 |
| 订阅 | `UserSubscriptionItem` | `subscriptionInfo` | 适用时包含 `topicInfo` |
| 用户日志 | `UserLogItem` | `logInfo` | 关联用户、社区、房间或主题 |
| 配额检查 | `QuotaDetail` | `quotaInfo` | 锁引用上传时包含 `uploadRecord` |

主题置顶和取消置顶修改返回基础 `TopicInfo`；客户端通过只含置顶变化的事件，将返回标志应用到当前缓存投影，保留原始内容与关联对象。不得将 UI 摘要写回详情缓存。列表保留 `data`、分页 token 和服务端顺序。主题列表与详情查询目前加载相同关联，因此有意共用同一投影。缺失或不可用的关联对象保持可空；场景从不加载的字段不应出现在其类型中。主题投影为已有使用者提供计算型读取访问器；这些访问器不参与序列化，更新时显式复制嵌套基础对象。

查看者成员关系保留在 `communityInfo.member`。`targetMemberInfo` 属于查询指定用户，不得覆盖查看者成员关系。当前社区列表 UI 不显示目标用户成员关系，因此只存基础社区。

这是破坏性的线上格式变更，必须配套部署服务端和客户端。HTTP/WS 及本地缓存序列化器使用新投影约定。Alpha 阶段不为已停用的客户端数据库格式提供兼容迁移。活动预览保持临时且按会话隔离，解密消息不进入持久存储。

已有测试保留断言，但改为构造和读取对应投影，不再使用 extension 字段。验证 `TopicProjectionTest`、`EventTest`、`ActivityPreviewHostTest`，以及受影响的服务端 Topic、Community、Title、User、Quota 和 RoomActivity 集成测试。构建和测试使用 JDK 21，显式传入 `-Pserver.flavor=dev -Pserver.buildType=debug`；assembly、Windows Detekt 和截图运行应串行执行。

## 自适应 Home UI

Home 是独立根目的地；进入社区、房间、主题或用户时，正常压入返回历史。旧的根列表／详情元数据和列表面板替换测试已移除，因为原导航约定不再适用。Home、Community 和 User 各自保留一个页面级 pager，在侧栏和底部导航之间共用。Room 和 Topic 详情内容使用居中的阅读栏，最大 900dp。
Settings 和创建表单使用 AdaptiveFormLayout，最大 1200dp；扣除内容 padding 后至少有 800dp 时使用两个等宽列。每项设置与分隔线共同构成一个布局项。个人收藏、评论、订阅和 reaction 记录复用 AdaptiveItemGrid 及其分页键。主题编辑器在可用内容宽度达到 1000dp 时使用编辑／预览面板，最大 1600dp；紧凑视图保留 preview/raw/block 标签。草稿和模式状态放在宽度分支之外。
内容宽度适配应在区域或共享展示组件内部、扣除导航和 padding 后进行。Home 上下文只属于 World，使用有数量上限的未读房间、置顶主题和已加入社区预览。房间摘要和准确未读数使用下文活动端点。

将 `./gradlew :app:composeApp:jvmTest :app:composeApp:adaptiveUiTest -Pserver.flavor=dev -Pserver.buildType=debug --console=plain` 作为一行执行。`adaptiveUiTest` 只运行无需运行时的 Home 布局夹具，与常规宿主套件分开，不需要后端或设备。它检查缩放时的区域、搜索和滚动保留、上下文可见性，以及结果宽度和短窗口滚动。
展示快照使用 `./gradlew validateDebugScreenshotTest -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`。快照夹具必须独立于应用运行时 CompositionLocal。

## 房间活动与 Home 上下文数据

受保护的读取端点使用当前不透明 Cookie 会话，没有目标用户参数：

| 端点 | 客户端请求 | 结果与排序 |
| --- | --- | --- |
| `GET /users/unread/messages` | `getUnreadMessageCount()` | 全部已加入房间的准确未读直接消息总数，与分页无关 |
| `GET /users/unread/room-activities` | `getUnreadRoomActivities(PaginationQuery(...))` | 先过滤再分页的完整已加入未读集合；按最新直接消息 ID 降序 |
| `GET /users/pinned-topics` | `getJoinedPinnedTopics(PaginationQuery(...))` | 已加入社区中的直接置顶主题，按主题 ID 降序 |

两个分页端点接受已有 next/pre token，页面大小为 1–100。上一页仍按相同降序展示。房间活动游标使用 `latestMessageId`，不使用房间 ID，避免因更旧未读消息再次返回同一房间。并发活动可能让条目跨页移动，因此使用者需刷新以获取当前顺序；这些是实时查询，不是快照。

`RoomActivityInfo` 是请求边界投影，不改变 `RoomInfo`。它包含房间元数据、可空的最新消息 ID 和正文，以及显式 `Long` 未读消息数。`TopicInfo` 提供内容、类型、作者信息及保存的创建时间戳。使用者解密后生成长度受限的预览，不要求服务端生成明文摘要。空房间没有最新消息，计数为零。已删消息从投影和计数中消失。被邀请的通知房间接收者保留已有读取权限；新列表有意只覆盖已加入房间，不改变邀请或通知语义。已加入的只读房间仍包含在内。

反向房间消息列表在条件加载行之前保留永久的 1dp 最新边缘项。用户位于最新边缘时，它在 Send 后锚定新消息；历史位置保留可见消息 key。已读标记使用实际可见主题 key 的最大值，忽略锚点和加载行，不假定展示顺序，也不将未看到的消息标为已读。自适应 UI 测试同时检查最新边缘跟随和历史位置保留。

计数包含用户房间已读标记之后的全部现存直接消息；没有标记时包含全部直接消息，包括用户自己的消息，但不含回复。已读标记通过数据库原子最大值前进，因此查看历史或较旧请求迟到都不能让已读消息重新变为未读。ID 只作为有序已读标记比较，绝不通过相减计算消息数。`UnreadRoomsResponse.unreadCount` 仍表示不同未读房间数，并共用服务端过滤后的分组查询。页面 total 表示全部符合条件的房间或主题数量，不是当前页长度。

服务端批量读取房间、成员、计数、主题和作者数据。无访问权限或未加入的房间 ID 被忽略，不泄露其是否存在。私有正文在线上保持加密，只携带当前成员的加密密钥。客户端请求复用 `processEncryptedTopic`；密钥缺失或解密失败时保留已有不可用或失败表示，不回退到明文。不新增后台轮询或会话续期。使用者在显式刷新或已有功能事件时请求新数据；标记已读、退出、取消置顶或删除之后，下一次请求反映当前状态。

使用常规 dev/debug 属性，在配置的 Docker 兼容后端上执行 `:cloud:server:test --tests com.storyteller_f.a.cloud.server.RoomActivityTest`。PostgreSQL 加已有 filesystem 和 Lucene RPC 容器，覆盖房间页面活动排序、超出已加载页的总数、直接消息计数和已读标记、双向游标、只读房间、删除、私有消息线上与客户端内容、成员关系丢失、置顶范围和请求限制。

## 活动预览集成

已加入房间列表、已加入房间搜索、社区房间列表和社区房间搜索，在已有 `data` 和分页信息旁返回 `activities` 投影。它只覆盖同一页中符合条件的已加入房间；匿名和未加入行没有私有活动投影。管理员列表保留兼容的空默认值。客户端请求封装在交付页面之前，通过已有私有主题边界解密活动。`RoomListPreviewHost` 直接接收这些页面，不再发起其他请求，并跨页保留临时预览。刷新替换投影；本地 Room 存储仍只持久化 `RoomInfo`，不保存解密活动。房间事件和生命周期恢复刷新已有 pager，保留滚动管理者。房间元数据和活动来自同一次列表请求，包括搜索和社区调用方。导航角标显示服务端完整未读消息总数，不对已加载行求和。旧的未读房间数保持原含义。

World 仅在宽视图显示的上下文，独立展示最多三个未读房间、三个置顶主题，以及已加入社区预览；预览不完整时显示真实来源总数。上下文滚动独立于信息流。房间或主题条目打开已有根详情目的地，Rooms 操作打开已有 Home 区域。不增加新的路由级拆分或通用活动信息流约定。

`ActivityPreviewHost` 独立于 UI 框架，使用注入且由应用管理的串行协调调度器。请求、解密及长度受限的文字和日期准备在工作调度器执行。结果是不可变、临时的展示状态，不存储解密预览。被取代的任务取消后不能发布迟到结果。按会话 key 管理的 UI 收集器在账户变化时重置；生命周期停止暂停或取消请求，恢复时获取当前数据。组合关闭时取消 Host。房间创建、变更、已读、加入、退出和社区成员事件更新符合条件的数据；房间事件不会重新启动置顶预览。置顶或取消置顶成功后，从请求边界发出共享主题变化事件，包括对话框和菜单调用方。

纯快照夹具覆盖紧凑和宽活动行、大字体、私有摘要不可用及上下文错误。Headless 测试覆盖跨页保留、刷新替换、取消、来源独立和事件过滤。自适应 UI 测试覆盖预览重试与导航、缩放时的准确计数、列表刷新回调，以及活动或非活动生命周期绑定。Home 未读导航角标在活动的已读、消息、成员事件、显式来源刷新和返回 Home 时更新。刷新会等待已有查询完成，避免加载期间丢失更新；本地化标签在侧栏和底部导航都标识未读状态。原生房间单元保留合并后的内容文字供辅助技术使用；浏览器测试覆盖层保留房间名导航标签。

Activity Appium 测试使用隔离的合成账户和真实后端、WebSocket 通道。已读和未读清除、发送和持久化预览、重复房间导航、上下文导航、房间列表缩放及草稿保留各有独立测试结果，共用夹具准备。私有房间读写用例覆盖加密传输和同账户解密，不能证明获准跨用户解密或未授权明文隔离。单独的刷新用例通过另一会话将房间标为已读，并验证 UI 刷新。

桌面和浏览器执行器显式暴露布局与草稿用例。Android 独立宽上下文用例临时设为 1920×1080、160 dpi，即使失败也恢复原始尺寸和密度覆盖。Android 整次调用必须持有租约。桌面视口变化使用已验证的原生窗口及 DPI 换算。桌面键盘测试需要可交互且未锁定的桌面。浏览器用例按 Chrome、Edge、Firefox 顺序运行；不同平台的执行不属于独立业务约定。

使用 `:app:androidAppium:appiumTest`、`:app:desktopAppium:appiumTest` 或 `:app:wasmAppium:appiumTest` 运行受影响用例，传入 `--tests '*activity*'`、`--tests '*room draft*'`、`--tests '*private room*'` 和常规 dev/debug 属性。操作桌面前先完成产物准备；UI 执行期间不重建共享运行时 JAR。保留独立的房间发布和刷新用例。

### issue #100 跟踪的测试约定覆盖

CLI 键盘输入使用 `dev/mosaicInputRuntime` 中的源码重建。Mosaic 0.18.0 的终端事件通道在 64 事件缓冲区填满后丢弃旧事件，可能导致普通粘贴的 PEM 密钥在应用收到之前被截断。重建会核验发布源码归档的 SHA256，并将队列改为有界、可挂起的交付。保留发布的原生终端构件和 Apache 许可证。上游跟踪见 [Mosaic #1241](https://github.com/JakeWharton/mosaic/issues/1241)。上游发布支持无损事件交付，且 CLI 登录与受支持命令套件在该版本通过后，移除此依赖替换。对于键盘事件限于 ASCII 的终端，reaction 输入还支持显式 Unicode 码点，例如 `U+2764 U+FE0F`。

Windows 受保护值集成通过 `:client:composeCore:windowsProtectedStoreTest` 单独运行，使用常规 dev/debug 属性。它使用真实 DPAPI 加密，在目标文件位置放置非空目录来触发真实文件系统写入失败，验证加密原值，并清理模块 build 下的夹具。普通 `jvmTest` 排除此平台专用类；其他操作系统显式调用该任务会失败，而非静默跳过。这是存储集成，不是桌面 UI 验收。

从 File Explorer 的 Uploaded 列表或主题附件打开已上传文件时，使用同一全页预览。图片支持缩放，音视频使用平台播放器，PDF 使用原生渲染器或页内浏览器 PDF 查看器。其他类型显示下载操作，不伪装为已渲染内容。上传记录仍描述上传操作；不完整的本地文件不是预览目的地。主题附件操作位于内联播放器之前，避免大视频把全页入口挤到视口之外。原生已上传文件行将文件名和可见元数据合并为一个可操作的无障碍标签。

全页已上传文件预览要求登录，因为文件详情端点受保护。共享导航操作在点击时检查当前会话。访客看到与房间和交互操作相同的登录确认；只有确认后才打开登录，取消则保留来源主题和浏览器历史。恢复或直接进入的预览条目，在创建文件详情 ViewModel 之前显示登录按钮。该显式按钮直接打开登录，不再次确认。`FilePreviewNavigationTest` 和 `SignInPromptUiTest` 覆盖这些路径、会话变化和返回来源主题。

浏览器音频播放需要可用的默认输出设备。媒体宿主在等待 AudioContext.resume 之前拒绝零声道目标，显示播放失败，并释放解码器和 context。保留 E2E 播放进度断言：可信输入与允许自动播放，本身不能证明音频输出可用。运行 `node --test client/composeCore/wasm-media-host/*.test.mjs`，验证此失败路径及播放和资源生命周期回归。

浏览器 PDF 预览匿名获取上传文档，生成同源 Blob URL。客户端跨源隔离策略会阻止直接的跨源 PDF frame。PdfPreviewHost 管理请求及生成 URL：重试等待前次请求关闭，导航中止待完成接收，释放时撤销 URL。浏览器测试验证原生 PDF 查看器支持、iframe 加载、全页边界和准确文档字节；还应审查捕获的渲染页。合成 PDF 夹具包含可见文字，以区分空 frame。

Filesystem HTTP 在 `/objects/*` 暴露匿名只读 CORS，包括 GET/HEAD 预检和对象不存在响应。浏览器图片及下载客户端可以发起预检，只允许成功 GET 响应还不够。修改操作预检被拒绝，RPC 路由不继承对象读取 CORS。ObjectDownloadTest 覆盖这些边界。

预览下载在 Android 和桌面使用 FileKit 原生保存对话框，Web 使用浏览器下载管理器。原生传输流式写入所选目标，在所有结果下关闭目标，并验证接收字节数。取消保存对话框不发起请求。浏览器导出在将字节交给下载管理器前验证接收大小。原生保存取消通过操作系统选择器测试，随后返回同一预览。成功消息区分已完成原生保存与已启动浏览器下载。错误通过 GlobalDialog 显示，失败操作绝不自动重放。

附件对象块携带名称、URL、封面或标题及播放列表元数据。MIME 类型来自解析后的 FileInfo，因此 MarkdownObject 和 ContentBlock.ObjectBlock 不再序列化 contentType。带该字段的旧 Markdown/JSON 仍通过 commonJson 忽略未知字段的兼容机制读取。对象生成使用 JSON 序列化器，确保带引号的文件名仍有效。解析器和生成器测试改为验证旧格式解码、名称和 URL 保留，以及移除字段不再输出，不再断言重复 MIME 元数据。

项目菜单链接到源码仓库和随包提供的开源库列表。AboutLibraries 导出必须保留 License.name：运行时解析器要求该字段，否则整个文档返回空列表。页面显式报告加载和错误，并显示已加载库数量。过时的应用下载菜单项已移除。

macOS 受保护值集成使用显式 :client:composeCore:macosProtectedStoreTest 任务。只在独占租用的测试账户上运行：它创建临时原生 keychain，通过生产存储边界验证大段 Unicode 值和锁定 keychain 的写入拒绝，删除夹具前恢复原默认 keychain、搜索列表和交互策略。普通 jvmTest 排除此类和 WindowsProtectedStoreTest。平台任务在错误操作系统上失败，不静默跳过。旧 MacKeychainTest 往返用例现位于 MacProtectedStoreTest，在临时 keychain 中验证替换、空值和删除。执行 macosProtectedStoreTest 才会覆盖这些内容；普通宿主测试不再发现一个因 macOS 假设而跳过的用例。客体验收使用宿主编译的测试类，调用客体真实操作系统 API，无需在客体编译。

浏览器上传使用当前活动账户作用域管理的共享 uploader。传输进度按 256 KiB 合并写入，并始终持久化每个请求的准确终点；每个小网络缓冲区都写数据库事务会拖慢上传。浏览器 Fetch 在传输前消费缓冲请求正文，因此进度使用服务端确认的分块，不把正文消费视为字节已送达。暂停或失败期间，所选浏览器文件保留在内存，使同一页面恢复时无需重放已存分块。成功和中止释放所选文件，离开账户则丢弃 uploader。页面重载无法按路径重新打开浏览器所选文件，因此重启测试检查已完成记录，不承诺自动恢复。浏览器选择器夹具让 FileKit 创建真实 file input，通过 WebDriver 向该 input 选择文件。夹具只拦截原生选择器的打开；FileKit change 处理函数、共享 uploader、存储和 HTTP 请求正常执行。这不测试操作系统选择器对话框或客户端下载操作。

浏览器采集管理返回的整个 `MediaStream`，直到所需 track 交付 RTC 引擎。关闭未使用的媒体种类，停止多余 track；交付前取消时释放全部 track，包括取消后采集才完成的情况。仅有已连接 peer 或可见 video 元素不能证明呈现；浏览器 E2E 观察实际 `requestVideoFrameCallback` 回调。

下表描述源码层面的覆盖，不是成功执行记录。实际平台验收以对应 Gradle 结果为准。部分覆盖行仍是 issue #100 中待完成的工作。

| 优先级／功能 | 约定／测试 | 平台 | 层级 | 状态 | 覆盖边界 |
| --- | --- | --- | --- | --- | --- |
| P0 注册 | `test sign up identity and session survive restart` | Android、桌面、浏览器 | E2E | 已覆盖 | 从注册密钥派生身份，重启身份及认证后发布 |
| P0 账户切换 | `test child switch is process local and restart restores parent` | Android、桌面 | E2E | 已覆盖 | 重启前子身份、重启后父身份；浏览器没有切换 |
| P0 退出登录 | `test signed out state survives restart`；Panel 退出用例 | Android、桌面、浏览器 | E2E | 已覆盖 | 重启后已退出 UI；Panel 还检查登录重启 |
| P0 撤销 | App/Panel `test sign in survives restart and revocation requires explicit login` | Android、桌面、浏览器 | E2E + API 断言 | 已覆盖 | 重启前后拒绝旧夹具 Cookie；显式 UI 登录恢复受保护操作 |
| P0 活动读取 | `test reading activity clears unread` | Android、桌面、浏览器 | E2E | 已覆盖 | 准确初始计数、持久化已读标记、行和支持的导航角标清除 |
| P0 活动发送 | `test sending updates persisted activity preview` | Android、桌面、浏览器 | E2E | 已覆盖 | UI 发送、持久化最新文字和已读数、返回列表预览 |
| P0 私有活动 | 私有读写；`ClientRoomScenarios` 中授权 peer 和外部用户用例 | Android、桌面、浏览器 | E2E | 已覆盖 | 原生同账户读写，以及浏览器授权 peer 解密、重启和外部用户明文隔离 |
| P0 权限 | 只读、非成员和通知接收者用例；已有服务端测试 | 浏览器／服务端 JVM | E2E + API 集成 | 已覆盖 | 实际 UI 读取和不可用写入，以及 Cookie HTTP/WS 拒绝；通知接收者保留 INVITED 成员关系 |
| P0 双客户端消息 | `test two clients exchange unread messages and retain read state` | 浏览器 | E2E | 已覆盖 | 独立真实 profile 收发消息，断言对端准确未读／已读状态与重启持久化 |
| P0 认证失败 | 无效／未知 key 用例；Android 受保护写入和 Linux 缺库用例 | Android、桌面、浏览器 | E2E／集成 | 已覆盖 | 无效或未知 key、Android 真实受保护写入错误、Linux 缺库及进程内切换；Windows DPAPI／文件系统失败和 macOS 锁定 keychain 失败通过真实原生存储集成验证，与 GUI E2E 分开 |
| P1 社区操作 | 独立社区收藏／订阅切换和成员资料用例 | Android、桌面、浏览器 | E2E | 已覆盖 | API 持久化、导航后使用的 UI 状态、所选成员独有资料 |
| P1 活动导航 | 重复房间导航及独立上下文用例 | Android、桌面、浏览器 | E2E | 已覆盖 | Android 上下文使用显式宽视图夹具 |
| P1 自适应活动 | 列表布局与草稿缩放用例 | 桌面、浏览器 | E2E | 已覆盖 | 显式窗口矩阵；不隐含 Android 缩放覆盖 |
| P1 文件 | `ClientFileScenarios`、已有 `FileTest` | 浏览器／服务端 JVM | E2E + API 集成 | 已覆盖 | 真实浏览器选择器、上传字节／归属／重启、已确认进度、无分块重放的暂停恢复、取消和配额；主题／列表下载核对保存字节；原生保存取消及传输单元用例覆盖各自边界 |
| P1 附件预览 | UploadedFileScenarios 中独立图片／音频／视频／PDF／不支持文件用例 | Android、桌面、浏览器 | E2E | 已覆盖 | 主题和 Uploaded 列表入口、实际加载的媒体／PDF 内容、返回导航和下载操作；只含上传文件附件，不含 Mermaid／数学／代码块 |
| P1 Android 视频与画中画 | AndroidVideoScenarios 中内联、播放和暂停视频用例 | Android | E2E | 已覆盖 | 内联、PiP 前／中／后及重新进入时的实际解码 RGB 帧；排除暂停视频 |
| P1 Android panel 视频 | test panel uploaded video preview renders and returns | Android panel | E2E | 已覆盖 | All-files 入口、全屏原生解码帧和返回文件详情；预览使用页面管理的 PlayerView |
| P1 项目菜单 | Repository 和 Open Source Libraries 用例 | 浏览器／原生库页面 | E2E + 单元 | 已覆盖 | 仓库目标、非空生成许可数据和显示的库数；已移除过时应用下载入口 |
| P1 搜索／分页 | `ClientSearchScenarios`、拆分搜索和已有分页测试 | 浏览器／服务端／客户端夹具 | E2E／集成／单元 | 已覆盖 | UI 查询与服务端顺序、实际观察的不透明 token 续页、重命名索引替换和刷新锚点；有序数据库加载保留索引顺序 |
| P1 社区内容 | 创建／成员／评论用例，加已有发布／收藏／订阅 | Android、桌面、浏览器 | E2E | 已覆盖支持的操作 | 浏览器创建和重启，以及原生／浏览器成员和评论持久化；创建会更新已加载的加入集合；主题编辑／删除是尚不支持的产品操作，不纳入覆盖 |
| P1 管理 | `PanelMutationScenarios` 加已有认证／导航 | 浏览器 | E2E | 已覆盖 | 通过真实 UI 和后端验证用户状态、文件状态／内容、Worker 配置／效果，以及重试／重启／准确消耗 |
| P1 RTC | `ClientRtcScenarios`、原生协商、采集和信令测试 | 浏览器／JVM／服务端 | E2E + 集成 | 已覆盖 | 两个真实浏览器 UI／信令／呈现帧、静音／挂断、加入前取消及迟到采集释放；原生协商和失败／资源测试；合成浏览器媒体只用于 RTC |
| P1 CLI 客户端 | App/Panel 约定套件，加原注册冒烟测试 | 宿主 JVM | E2E | 已覆盖 | App/Panel 支持的列表／详情／修改命令，以及无效 key／访客／菜单失败；多行私钥及实际持久化结果 |
| Worker 持久化 | 四个独立 Worker 持久化用例 | 服务端 JVM | 集成 | 已覆盖 | 每种 Worker 类型独立发现为一个用例 |
| 预设导入 | `CliTest` 中独立 CLI 主题和头衔用例 | 服务端 JVM | 集成 | 已覆盖 | 分别验证主题内容／作者，以及头衔接收者／范围／说明 |
| 自适应预览 | 重试隔离与 `roomPreviewClickInvokesNavigation` | Compose 夹具 | UI | 已覆盖 | 独立刷新和点击结果 |

Lucene 替换是一次索引提交内的通用“查询加文档”RPC 操作。服务在修改前验证文档，写入失败时回滚。房间和成员适配层声明各自标识查询，并在操作期间固定一个 RPC proxy；只追加更新文档会留下过时关键词匹配。

服务端集成夹具显式管理 Ktor `TestApplication`，在失败安全的清理中先停止应用，再移除服务容器。基于 block 的便利辅助方法只有 block 正常返回才会调用 stop。保留原始失败，包括取消，并将清理失败作为 suppressed exception 附加。

房间重命名在更新返回前，同时刷新房间文档和已加入成员搜索文档。成员页重建索引使用数据库查询的 user-key 游标，排除受邀和已离开成员。搜索刷新夹具比较服务端顺序前先验证索引就绪；第一个完全可见的行作为滚动锚点。

Wasm 桥接几何必须跟随 placement，不只是 composition。Lazy 列表缓存项即使不再放置，也可能仍在组合中；应在 unplaced 回调分离 HTML 节点，重新 placement 时恢复，否则旧行会在过时坐标继续交互。真实 WebDriver 滚轮输入期间，临时隐藏透明测试控件，使输入到达底层渲染器；交互节点必须有无遮挡的指针目标。搜索输入使用独立命名选择器；通用编辑器输入必须定位到唯一可见的非搜索编辑器。

Wasm 桥接操作使用真实 WebDriver 指针点击，以保留浏览器用户激活；合成 JavaScript 点击可能触发 Chromium 安全失败并停止 Compose 渲染。桥接操作可能让焦点留在 document body。注入会话就绪检查需等待用户信息恢复，而非仅创建账户实例；异步结果会改变头像和创建入口位置。解析指针目标前等待稳定、已提交的布局。测试驱动在发送真实 Escape 键前聚焦可见 Compose canvas（包括 shadow root），再等待对话框消失。保留相同 UI 超时和业务断言，不用可点击的对话框表面替代，也不直接调用关闭回调。

有必要依赖关系的流程应保留在一起：写入和读取持久化、登录／撤销／恢复，以及不重放的 socket 重连。独立 API 或无关状态转换必须分别报告结果。外部选取测试时，用上面命名的约定替换旧的组合活动／社区资料和服务端测试名；普通 Gradle 测试任务会发现全部替代用例。

浏览器执行器现在复用相同的公开／私有活动和跨会话刷新场景。Chrome、Edge、Firefox 验收覆盖显式登录、加密预览、准确计数和导航角标、未读／置顶上下文导航、读取、发送，以及在 500/900/1600px 宽度和 500px 高窗口中的草稿保留。已有发布、注册和登录／退出后重启场景提供选择器与认证回归覆盖。除了 HTML 语义覆盖层，还要捕获渲染页；新的预览文字和操作使用已有 Appium 专用桥接，不改变原生无障碍。覆盖层节点堆叠索引相同，因此后台页面存在同标签按钮时，驱动选取最后一个匹配的可见节点，以遵循前台对话框顺序。控件尺寸不变时，桥接元数据仍在重组期间更新。两个活动变体发送后都会再访问同一房间三次。失败产物包括 HTTP 和 SQLite worker 操作计数，不包含凭据、地址、SQL 或消息正文。

Firefox 原生窗口可能强制最小 500px 宽度。浏览器紧凑覆盖使用这一真实视口；桌面覆盖仍为 480px。浏览器 chrome 在缩放时改变高度，因此先固定宽度，再测量 frame，并精确验证最终内容尺寸。每次视口转换后，根据当前 HTML input 值和 Compose 确认核对草稿。活动测试使用 `:app:wasmAppium:appiumTest --tests '*test activity previews*'`，刷新测试使用 `--tests '*test room refresh clears*'`，依赖同样的 dev/debug 和 Docker 前提。`:app:wasmAppium:appiumTest` 与 `:panel:wasmAppium:appiumTest` 都通过 JUnit 参数矩阵，在 Chrome、Edge 和 Firefox 上运行每项测试。报告标识浏览器，变体保持顺序执行。每次运行使用临时隔离 profile，在浏览器重启间保留，测试后删除，绝不使用个人浏览器 profile。

Selenium Manager 先发现本地已安装浏览器，再解析匹配驱动。没有安装时，使用其管理的浏览器缓存，或在支持的平台下载浏览器；浏览器缺失让矩阵条目失败，不静默跳过。非标准安装可将 `WASM_E2E_CHROME_BINARY`、`WASM_E2E_EDGE_BINARY` 或 `WASM_E2E_FIREFOX_BINARY` 设为已有可执行文件，只在测试创建浏览器时读取。运行矩阵前移除旧的共享 `SE_BROWSER_PATH` 覆盖，因为它会应用到全部浏览器。`SE_OFFLINE=true` 要求匹配驱动和浏览器已就绪。过滤后的 Gradle `--tests` 仍会在三个浏览器运行所选场景；不要修改测试超时或跳过缺失变体来隐藏环境失败。

分页客户端将服务端 page token 视为不透明字符串，保留返回条目顺序。本地 Room offset 只属于数据库 PagingSource。映射源为新一代提供 Room 的居中 offset 刷新策略，CompatPagingSource 转换本地 key 时保留占位数量。RemoteMediator REFRESH 沿返回的 next token 请求，直到覆盖先前缓存范围（至少 initialLoadSize），再一次提交替换。失败或取消的请求不发布部分窗口，也不推进已存 remote key。Room 在同一事务替换列表记录、详情 ID 和别名，预览 Host 在该提交之后发布暂存页。不使用 UI 管理的请求账本、token 解析、基于 ID 的 seek 或手动滚动恢复。

`client/room` 集合刷新只删除所请求列表的命名集合。房间和头衔刷新保留默认详情缓存及其他列表；清除默认缓存可能在底层 Home 列表刷新时使已打开详情失效。`:client:room:jvmTest` 在真实 SQLite 上运行共享 headless 测试，覆盖此边界及详情别名。临时数据库和原生驱动文件留在模块 build 目录，使用唯一测试作用域。

Wasm SQLite 取消会等待待完成的 worker 响应。新连接或 statement 无法交付给已取消调用方时，关闭它们。Rollback、commit、savepoint release 的准备和执行必须不可取消地完成：Room 池清理在回收 permit 前执行 rollback，若 rollback 被取消，可能卡住唯一连接，让之后详情页面永久加载。普通 statement 仍响应取消，不重放业务操作。`:client:room:wasmJsBrowserTest` 覆盖响应等待、未交付分配清理和取消期间的终结处理；重复真实浏览器导航覆盖完整 Room/worker 边界。

## 临时固定的依赖版本

在[上游 issue #756](https://github.com/pgjdbc/r2dbc-postgresql/issues/756)（1.1.3 的 DNS UDP socket 未释放）解决之前，PostgreSQL R2DBC 固定为 `1.1.2.RELEASE`。Dependabot 专门忽略 `org.postgresql:r2dbc-postgresql`，分组版本更新也不例外。验证上游修复后，更新版本目录，并移除 `.github/dependabot.yml` 中对应的 `ignore` 条目。配置进入仓库默认分支后，忽略规则才生效。

## 后量子密码

JVM 和 Android 通过 `BC` 提供方，共用 ML-DSA-65 与 ML-KEM-768 的 JCA `Signature`、`KeyFactory` 操作。KEM 在 JVM 使用 Java 21 `javax.crypto.KEM`，Android 使用 BC `KEMGenerateSpec`/`KEMExtractSpec` 配合 `KeyGenerator`，因为 Android SDK 不提供 `javax.crypto.KEM`。Android 显式关闭默认 KDF，使两个平台保留与 Wasm 相同的原始 32 字节共享机密。保存的 PEM 正文仍为长格式原始密钥，不是 PKCS#8/X.509 编码。密文仍由 ML-KEM 封装后接 AES 密钥与共享机密的 XOR 结果构成。

运行 `./gradlew :shared:jvmTest :shared:testAndroidHostTest -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`，执行共享 JVM/Android 兼容测试。测试双向比较 JCA 实现与旧轻量 API，并验证由 Wasm 固定使用的 `@noble/post-quantum` 0.6.1 生成的固定合成向量。Gradle 安装 Wasm 依赖后，可用 `node shared/ml-crypto/generate-compatibility-vectors.mjs build/wasm` 重新生成向量；生成器使用固定合成 seed 和确定性签名。Android 宿主测试通过 Robolectric 运行。设备验证需连接 Android 设备并持有其租约，再运行 `./gradlew :shared:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.storyteller_f.shared.PostQuantumCompatibilityTest -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`。设备 source set 复用 headless 兼容测试及固定向量。

## 后端测试镜像

每个后端服务应用 `test-docker-image`。`buildTestDockerImage` 依赖 Sync 任务 `copyTestDockerDistribution`，后者准备本模块 `build/test-docker-context`。上下文只含自身 distTar 输出、权威根 Dockerfile 的副本，以及必要的暂存和 CLI 辅助脚本。镜像构建前完成准备，其他模块不得修改该上下文。镜像任务没有全局 build-service permit，可以并发。集成和 E2E 任务依赖各自需要的镜像，普通 assembly 不依赖。测试选择 `BUILDER_STAGE=prebuilt-builder`，只解压分发包，不挂载共享 Gradle 缓存。根 Dockerfile 部署默认使用 `source-builder`；源码仍在 Docker 内编译，并对共享可写缓存加锁。Testcontainers 只负责启动镜像。

使用操作者配置的 Docker 兼容后端运行 Gradle。所需测试镜像由 Gradle 通过 PATH 上的 Docker CLI 构建；宿主测试也必须能访问 Docker API 和公开服务端口。外部环境执行器可以透明提供这些连接。提供方安装、网络实现、私有 profile 和生命周期命令保留在仓库之外。其他运行进行中，不重启共享后端。构建上下文必须继续通过 `.dockerignore` 排除机密。

## Wasm Nginx 配置

Compose 将 `deploy/docker-compose/app-wasm.nginx.conf` 只读挂载到两个 Wasm 站点的 `/etc/nginx/conf.d/default.conf`。Wasm 镜像包含构建站点，不包含此配置。修改宿主配置后，重新加载 Nginx 或重建容器，无需重建镜像。单独运行镜像时也必须提供同一配置，启用所需 COOP/COEP 头。

Docker 构建用 SHA256 内容前缀重命名各 Wasm 入口脚本，并更新 index 引用。另一份内容摘要通过 `configureWebResources` 为 Compose 资源 URL 加版本，使翻译字符串与生成字节偏移同步。HTML 和可变资源 URL 必须接受缓存验证（`Cache-Control: no-cache`），否则浏览器可能跨部署保留旧入口脚本，加载与新 HTTP 探测不同的客户端。升级验收时，核对浏览器实际加载的 Wasm 文件名与部署镜像。

## Adminer 反向代理

Bunker Adminer 站点使用正则 location `~ ^/`，而非普通 `/`。Bunker 会给普通根 location 添加 `X-Forwarded-Prefix: /`，导致 Adminer 创建 `Path=//` 的会话 Cookie，无法在 `/` 登录。正则路由保留请求 URI，不添加此前缀。检查部署时，必须通过 HTTPS 域名实际登录 PostgreSQL，不能只检查登录页响应，同时确认会话 Cookie 为 `Path=/`。

## 独立 filesystem 与 Lucene 服务

这些服务共用本仓库构建工具，但运行时依赖图不包含 A 的 `backend/core`、`shared` 或 `api` 业务模块。

- `services/filesystem-protocol` 定义 filesystem RPC 约定和对象元数据。
- `services/lucene-protocol` 定义通用索引文档、查询和 Lucene RPC 约定。文档由具名 text/long 字段列表组成，允许重名。索引模式（`NONE`、`TEXT`、`EXACT`）、存储及 DocValues（`NONE`、`NUMERIC`、`SORTED`）相互独立。读取按字段名分组返回已存值，保留多值；没有 ID 属性。
- `cloud/filesystem-service` 实现对象存储和直接 HTTP 下载。
- `cloud/lucene-service` 实现通用索引与搜索。
- `backend/filesystem` 和 `backend/lucene` 将 A 的业务接口适配到这些协议。Filesystem 元数据在该边界显式转换；服务不实现业务侧 `ObjectStorageService`。

`LuceneUserDocument` 等后端转换类将业务文档映射为通用字段，并从搜索结果还原，不使用 JSON payload 或业务序列化器。服务不创建或保留 `id1`、`id2` 等应用字段。查询声明字段名；排序声明字段名、LONG/STRING 类型和方向。RPC 没有 `get(ids)` 或 `sortByIdDescending`。A 的适配层显式声明自身的 `id` 字段，启用存储、精确索引和 numeric DocValues，通过普通查询查找主题。RPC 约定已改变，必须配套部署服务和后端客户端。通用服务按原名返回旧存储字段，不做翻译。A 的旧 `id1` 或 `_payload` 索引需要从源数据重建，以匹配适配层新字段声明。本变更不删除或自动迁移索引数据。

无需启动 A 应用服务或数据库，即可构建分发包并运行服务测试：

```bash
./gradlew :cloud:filesystem-service:distTar :cloud:lucene-service:distTar \
  :cloud:filesystem-service:test :cloud:lucene-service:test \
  -Pserver.flavor=dev -Pserver.buildType=debug --console=plain
```

Flavor/build-type 属性是仓库应用配置的要求，不是这些服务的要求。将各分发包解压到独立目录，运行 `bin/filesystem-service` 或 `bin/lucene-service`。两者都需要 JDK 21，也可通过已有 Dockerfile target 运行，不读取 `deploy/{flavor}.env`。

Filesystem 配置：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `FILESYSTEM_RPC_PORT` | `8820` | 内部 RPC 监听 |
| `FILESYSTEM_HTTP_PORT` | `8822` | 直接对象下载监听 |
| `FILE_SYSTEM_MEDIA_PATH` | `/data` | 对象存储目录 |
| `FILESYSTEM_PUBLIC_URL` | 必需 | 外部可达的 HTTP origin |

Lucene 配置：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `LUCENE_RPC_PORT` | `8821` | 内部 RPC 监听 |
| `LUCENE_BASE_PATH` | `/data` | 索引存储目录 |

保留已有配置名，避免部署迁移。两个服务均不读取业务选择项、数据库设置、用户凭据或 alpha/bunker 域名规则。RPC 端点必须位于可信内部网络；此次拆分没有引入 RPC 认证或签名下载 URL。Filesystem HTTP 当前向任何可访问 URL 的用户公开提供对象。两个服务都未实现业务授权。

协议提取后，客户端与服务端必须一起重建：RPC 接口现使用独立的 `com.storyteller_f.services.*.api` 命名空间。

## 服务日志收集

- Logback XML 使用权威导入形式：XML 声明和 `<!DOCTYPE configuration>`，随后是完整限定的 `<import class="..." />`。Appender、filter、rolling policy 和 encoder 引用导入后的短类名；每个 encoder 声明 `class="PatternLayoutEncoder"`。
- 服务端请求摘要将 URL、状态、方法、query、header 和 remote-IP 字段连接为一行，不额外插入格式换行，也不转换字段值。
- 日志文件按服务命名：`server.log`、`worker.log`、`ws.log`、`cli.log`、`filesystem.log` 和 `lucene.log`；轮转文件遵循 `<service>.yyyy-MM-dd.log`。已有文件保留名称，仍被 Promtail 的 `*.log` 匹配。
- Compose 将 server、worker、ws、cli、filesystem 和 lucene 的 `LOG_PATH` 覆盖为 `/home/app/logs/<service>`。六个服务共用 `service-logs` volume；Promtail 将其只读挂载到 `/var/log/A`，收集各服务目录，包括轮转 `.log`。
- 设置 `LOG_PATH` 时，即使关闭预设初始化，CLI 入口也将启动和就绪记录写入 `cli.log`。通过 `bash scripts/test_scripts/cli-entrypoint-test.sh` 验证。
- 运行时镜像创建归应用用户所有的日志目录，容器日志不再依赖宿主 `LOG_PATH` bind mount。应用服务和 Promtail 需一起重建以应用 volume 和路径变化；已有宿主日志不自动迁移。

## Windows AVIF 原生库

在 JVM 进程环境将 `AVIF_LIBRARY_PATH` 设为包含 libavif 及依赖 DLL 的目录。PowerShell 开发或测试会话可使用：

```powershell
$env:AVIF_LIBRARY_PATH = 'C:\tools\msys64\ucrt64\bin'
```

Windows 的 `loadAvif()` 将此值用作 `jna.library.path`，缺失或为空时报告配置错误。没有固定安装目录或 vcpkg 自动检测。DLL 命名及依赖安装由调用方负责。

### Windows 与 Linux 桌面 Appium

桌面 E2E 辅助方法默认选择宿主平台。`APPIUM_DESKTOP_PLATFORM` 可显式设为 `Windows` 或 `Linux`，Appium 服务必须运行在相同平台。其他平台显式报错。

- Windows：使用 Appium 3，通过 `appium driver install --source=npm appium-desktop-driver@2.8.0` 安装。辅助方法通过加引号的参数文件启动 JDK 21 JVM，传入 `appium:javaSwing=false`，操作 Tao 原生 UIA 树。无需 Java 无障碍 agent、字节码补丁、Java Access Bridge 或 WinAppDriver。查询使用原生 UIA；激活前等待控件边界可见且稳定。Windows 对可聚焦 TabItem 使用原生焦点和 Space 激活。其他有 InvokePattern 的控件，在可用时聚焦后使用 DesktopDriver `windows: invoke`；SelectionItemPattern 使用 `windows: select`。Tao tab 提供方暴露 InvokePattern 和键盘焦点，没有 SelectionItemPattern；真实原生会话已检查焦点与 Space 激活。其余控件使用 `windows: hover`（200 ms）后接 `windows: click`（按下 100 ms）。这些是标准原生 UIA 和键盘操作；失败操作不重试，断言不变。Windows 返回通过现有键盘接口发送原生 Escape，让当前对话框或导航分派器优先处理；调用后台返回按钮可能绕过模态场景层。文本输入使用原生焦点与按键。具备 ValuePattern 的原生非密码 Edit 控件，通过标准 Appium 扩展调用 DesktopDriver 已有 `windows: getValue` 返回值。DesktopDriver 的 TextPattern 回退可能返回控件标题，而非输入值。其他控件、密码和 WebView 上下文保持原行为。普通 Windows Appium 扩展将精确 UIA `0x80040201` / `UIA_E_ELEMENTNOTAVAILABLE` 映射为标准 stale-element 响应，让已有等待重新定位退役控件。COM 关于事件订阅者的消息对这一 UIA 代码具有误导性；无关错误保持不变，扩展不自行重试命令。测试断言和超时预算不变。子账户在发布到 UI 或发起受保护后台请求前先认证。失败或取消关闭尚未发布的账户，保持前一账户活动，通过已有对话框报告失败；发布拒绝已被取代的账户。活动账户实例变化时关闭账户选择器，避免成功切换后模态弹层拦截下一个工具栏点击。
- Linux：通过 `appium driver install --source=local "$PWD/dev/appium-linux-driver"` 使用仓库驱动。它使用开源 AT-SPI2，以及 `python3-pyatspi`、`xdotool`、`xdpyinfo`、`gdbus` 和 GDK 3。驱动启动应用前，在桌面会话 bus 上启用 `org.a11y.Status.IsEnabled`。一个进程可同时注册 GTK 和 AccessKit 提供方；查询遍历该 PID 的全部提供方，元素标识包括 D-Bus 连接和对象路径。驱动在启动 JVM 前拒绝不可用或零尺寸显示。窗口尺寸和矩形命令只针对启动 PID 所有的唯一可见窗口；归属含糊时失败，不调整当前聚焦的桌面窗口。截图通过 GDK 3（`gir1.2-gtk-3.0`）从活动 X11 显示获取 PNG，不依赖 `libstdspalinux.so`。当前从源码测试此包；发布是独立步骤。
- Linux 驱动回归：在 `dev/appium-linux-driver` 执行 `npm ci` 和 `npm test`。元素句柄跨导航保留 AT-SPI D-Bus 连接与对象路径。点击和文本输入保留已保存标识；即使标签改变，文本输入仍直接解析该路径。目标缺失或 defunct 时返回 stale-element，包括写入过程中消失。输入失败不选择其他可编辑控件，也不使用缓存坐标；非 stale 的 editable-text 失败直接报告。查找遵循 WebDriver implicit wait，不加固定等待。实时几何、可见性和启用状态查询解析已存标识；共享辅助方法在现有超时内等待稳定且可见控件。合并后的房间和活动卡片将房间、可见消息或元数据及未读数呈现为完整无障碍描述。匹配完整的逗号分隔标签部分，不使用任意子串。
- Linux/X11 文件对话框通过 FileKit 借用活动 Tao XID。XDG portal 选择器在应用 PID 之外运行：驱动对应用所属 XID 验证 `WM_TRANSIENT_FOR`，只接纳唯一且归属明确的窗口或提供方子节点。其他 portal 窗口排除。窗口消失使查找失效，不替代为坐标或无关桌面控件。Portal 激活和应用必须位于测试会话的无障碍 bus；继承的桌面服务若仍连接旧 bus，可能显示对话框却不发布到当前测试树。
- `APPIUM_DESKTOP_SERVER_URL` 默认 `http://127.0.0.1:4723`；`APP_DESKTOP_TEST_JAVA` 可选指定 Java 可执行文件。
- 测试 JVM 使用英文 UI 标签、Windows JVM 内 Preferences factory，以及隔离数据库和受保护存储目录。启动器记录 JVM PID，将应用日志写入各测试报告。窗口枚举和切换使用 DesktopDriver 远程接口；项目 Appium 扩展在 Appium 服务端验证进程和线程归属，并进行 DPI 感知的原生窗口缩放。会话清理强制关闭测试 JVM 并删除 PID 文件。测试辅助方法绝不使用远程 PID 操作宿主窗口。

运行 `:app:desktopAppium:appiumTest` 和 `:panel:desktopAppium:appiumTest` 前，使用 `-Pserver.flavor=dev -Pserver.buildType=debug` 构建 `:app:desktopApp:writeAppiumRuntimeClasspath` 与 `:panel:desktopApp:writeAppiumRuntimeClasspath`。同一桌面上顺序运行两套件。后端需要可用 Docker/Testcontainers，使用操作者提供的 Docker 端点。

Linux 桌面应用使用支持原生 X11/Wayland 和 AT-SPI 的 Tao。需要图形会话、兼容系统图形库及 WebKitGTK；仅 headless 操作系统镜像不足。任一平台执行前，按当前 DPI 准备足够桌面空间，以容纳测试窗口边界。

Linux 桌面 E2E 需要安装 `xvfb`、`openbox`、`x11-utils`、`dbus`、`libsecret-1-0`、`libsecret-tools`、`gnome-keyring`、上述 Linux 驱动依赖，并让 Appium 位于 PATH。认证场景要求隔离 D-Bus 会话中有已解锁原生 keyring；使用隔离的 `XDG_DATA_HOME`，不要使用个人桌面 keyring。随后运行：

```bash
bash scripts/test_scripts/run-linux-desktop-e2e.sh -Pserver.flavor=dev -Pserver.buildType=debug
```

执行器管理 1600×1000 虚拟 X11 显示、窗口管理器、独立 D-Bus/AT-SPI 会话和 loopback Appium 服务。`APPIUM_DESKTOP_PORT` 选择端口，默认 4727。它在同一次 Gradle 调用中以 --continue 提交用户和 panel 桌面 E2E 任务。共享桌面 BuildService 串行执行套件，准备阶段仍可并行。测试过滤及支持的 Test 任务选项对两套件生效；Gradle 全局参数只应用一次。执行器传播失败，只停止自己启动的进程。诊断保留在 `build/linux-desktop-e2e.*`；原始日志可能包含测试凭据，分享前必须脱敏。应用渲染设置、测试用例、断言和超时保持不变。

无人值守测试不要使用断开的用户 Xwayland 会话：Java 和 X11 窗口仍存活时，显示可能报告 0×0。已诊断失败中，多次线程转储发现 UI 线程处于 Skiko `swapBuffers`，ATK 等待 UI 分派；随后 AT-SPI 不再暴露应用。使用执行器管理的显示可消除此依赖，而非掩盖元素缺失。诊断中独立的渲染和 vsync 控制见 [Skiko 渲染属性](https://github.com/JetBrains/skiko/blob/master/skiko/src/jvmMain/kotlin/org/jetbrains/skiko/SkikoProperties.kt)。

Appium 选择器统一使用英文。桌面 JVM 设为 en/US，浏览器 E2E 变体设置 en-US 偏好。Android 13+ 夹具在清除数据前保存原应用 locale，临时设为 en-US，清理后恢复原覆盖，不改变系统语言。Linux 模态关闭通过专用 `linux: dismissDialog` 驱动命令发送 Escape。Windows 桌面导航发送原生 Escape，以保留模态优先级；Linux 通过 Appium 使用应用带标签的返回操作。Linux 驱动低层 back 命令仍为 Alt+Left。旧 Android 设备须已使用英文系统 locale。浏览器生命周期场景使用隔离且原地保留的 profile 重启浏览器，要求 HttpOnly Cookie 和加密 IndexedDB 记录经进程终止后仍保留。

### Wasm 浏览器单元测试

浏览器套件使用 Chrome Headless。执行 Wasm 浏览器任务前安装 Chrome 或设置 `CHROME_BIN`。Compose UI 测试目标声明 `binaries.executable()`，使 Compose 插件将 Skiko 与测试运行时一起打包。

共享 Karma 配置通过 HTTP 提供已编译 Wasm 测试包的 Compose 资源，让编译字符串偏移与当前翻译配套。`:client:composeCore:wasmJsBrowserTest --tests '*BrowserClipboardTest*'`（配合 `-Pserver.flavor=dev -Pserver.buildType=debug`）通过实际 Wasm clipboard 适配层测试已挂载播放器：拒绝反馈、第二次成功点击及取消。浏览器 clipboard 替换为受控 Promise 夹具；原生权限对话框不在套件范围内。UI 复制操作使用 `Clipboard.copyText`，原始 `setText` 失败时仍抛出异常。

`wasm-browser-test-interop` 约定应用于 `shared` 和 `client/room`。它只降低生成 `.import-object.mjs` 中的 `kotlinx-io` Node `createRequire` getter，保留模块 URL，防止裸 `import.meta` 别名进入 Karma classic-script bundle。Loader 作为测试输入跟踪，通过 `node --test config/webpack/wasm-node-import-meta.test.cjs` 回归覆盖。不修改缓存依赖或生成文件。

浏览器活动缩放场景使用严格的 504px 紧凑内容视口：Windows Chrome 强制该最小值（522px 外宽、18px 边框），Firefox 则允许 500px。504px 场景仍覆盖底部导航，并与 900px、1600px 布局一起测试；视口相等和草稿保留断言保持严格。

Edge E2E 在发出页面级 WebDriver 命令前，通过浏览器 [DevTools HTTP 端点](https://chromedevtools.github.io/devtools-protocol/index.html#http-endpoints) 创建 `about:blank` 目标，再关闭隔离浏览器的启动标签页。否则这些命令会等待 Edge 外部 MSN 新标签导航。ChromiumDriver 参数中的裸 URL 被解析为开关，不能作为位置参数选取启动文档。Profile 持久化 E2E 验证首次启动和重启只有一个空白目标，同时验证 Cookie 和 IndexedDB 保留。普通浏览器窗口、页面加载、严格视口检查和超时保持不变。

`app/composeApp` 浏览器 bundle 关闭对间接 Kotlin wrappers 生成、但未使用的 `events`、`fs`、`os` 和 `path` externals 的解析。这些生成 namespace 别名没有浏览器调用方，不要为其添加 Node polyfill。通过 `assemble` 验证库的生产 bundle，再通过三浏览器 E2E 矩阵验证真实 `app/webApp` 浏览器入口。

导航测试比较 `NavBackStack.toList()` 与预期历史，因为包装对象相等不是栈内容的跨平台约定。

Wasm 根和嵌套页面栈使用 `client/composeCore` 的 `BindBrowserNavigation`。每个 document 为客户端（`app` 或 `panel`）管理一个 history controller；嵌套 ID 位于根下，例如 `app/signin` 和 `panel/login`。每个 History API 条目包含序列化路由 key 和历史位置，使应用 Back 后仍可使用浏览器 Forward。保留当前 URL 和无关 history state。刷新恢复当前条目；编辑草稿和认证状态不存入导航历史。根 Back 返回前一 document，不移除最后应用路由。对话框关闭仍由平台 Escape/back 分派器管理，不创建页面历史条目。

路由 JSON 使用数组多态，避免目的地 payload 的 `type` 等字段与类判别字段冲突。该格式仅属于导航历史，不用于业务 JSON。[navigation3-browser v1.1.0 POC](https://github.com/terrakok/navigation3-browser/tree/v1.1.0) 提供单栈时间顺序导航，或不支持 Forward 的嵌套层级导航；共享 controller 同时支持嵌套栈和 Forward，应用 Back 穿越已有历史位置，不追加新状态。祖先位置遵循最新完整后代快照，因此应用 Back 保留前页所选嵌套标签。文件导航在侧栏和底部布局间共用一个 back stack 和 pager；浏览器恢复取消过时拖拽，不发布另一条路由。

聚焦导航验证（Chrome、Edge、Firefox，两个客户端）：

```bash
./gradlew :client:composeCore:jvmTest --tests '*BrowserNavigationTest' -Pserver.flavor=dev -Pserver.buildType=debug --console=plain
./gradlew :app:wasmAppium:appiumTest --tests '*BrowserNavigationTest' :panel:wasmAppium:appiumTest --tests '*BrowserNavigationTest' -x buildTestDockerImage -Pserver.flavor=dev -Pserver.buildType=debug --console=plain
```

这些浏览器用例不需要后端，测试真实访客登录页面。镜像构建排除只对该过滤器生效；认证 E2E 仍需完整容器拓扑。它们验证根和嵌套 Back/Forward、不产生重复条目的应用 Back、刷新、模态 Escape 关闭，以及根边界的离开和重新进入。原始产物保留在各执行器 `build/test-results/appiumTest`，静态查看器位于 `build/reports/appiumTest`。

Appium 执行器将目标和平台辅助对象传给强类型 `AppiumTestBase` 构造函数。共享测试流程扩展该基类，从中获取两个辅助对象和 JUnit 测试名；具体测试方法只提供场景选项，例如 `privateRoom = true`。Wasm 专属扩展要求接收者类型为 `WasmAppiumHelper`。

### E2E 导航与就绪

房间行原生无障碍将房间名描述和消息预览描述合并。标签保留在名称 Text 上，使原生读取者仍可获得预览和未读数。会话撤销和 socket 重连场景在启动应用前准备社区和房间。Home 挂载后，缓存 pager 不必观察另一客户端的准备写入；这些场景测试会话生命周期，不是跨客户端列表刷新。活动读写场景同时等待服务端预期最新消息和零未读；仅有乐观 UI 回显不能证明持久化或已读标记完成。

Panel 抽屉将账户操作放在可滚动导航列表下方，在紧凑窗口仍可见。清除认证时关闭模态抽屉，避免覆盖登录页。

### 本地 Android 测试初始化

宿主测试基类必须为抽象类。访问 ICU 或 Compose 资源的 Android 宿主测试通过 Robolectric 运行；资源测试初始化 Compose AndroidContextProvider。AsciiDoc 预览测试位于 headlessTest，在 Android 宿主、JVM 和 Wasm 上运行；Javet parser 测试及 npm 夹具位于 jvmAndroidTest，因为其原生 Java API 不支持 Wasm。Gradle 通过 npm ci 准备夹具依赖（npm 必须位于 PATH），缺少依赖则测试失败。Robolectric 在宿主运行时 classpath 解析 Javet JVM loader，Android 生产仍使用 Android loader。

### Android Appium 夹具生命周期

Android 辅助方法在创建后端容器、重新绑定 `adb reverse` 之前清理前一测试应用；在录像和日志导出、会话关闭之后，后端拆除之前，强制停止应用。`noReset` 会在会话关闭后保留应用进程；下一夹具替换 reverse 目标时，这些进程不能继续请求旧宿主端口。设备连接目标不在当前 reverse 配置中时，ADB 会致命退出（见[上游守卫](https://android.googlesource.com/platform/packages/modules/adb/%2B/HEAD/adb.cpp)）。这是夹具归属问题，与环境传输不同。不要绕过守卫、添加连接重试或放宽测试超时。

### 文件上传完整性

可恢复上传同时要求原始文件 SHA256（64 个小写十六进制字符）和整个文件 CRC32C（四字节大端值，以规范 Base64 编码）。客户端一次读取源文件计算两者，与可恢复状态一起保存，并通过 `Files.Chunks.InitBody` 发送。`UploadRecords.sha256` 和 `UploadRecords.crc32c` 均为 NOT NULL，无默认值。后端模型验证两种编码。OSS `compose` 边界负责校验：filesystem 使用 SHA256，MinIO 使用 CRC32C。已有最终文件 SHA256 字段保持不变。

Filesystem 按顺序将源内容复制到目标目录临时文件时，同步更新 SHA256。只有验证通过才原子重命名发布目标；失败则移除临时输出。验证失败不会删除已有目标和源分块；上传协调器另行将会话标为失败、释放配额并删除其分块。

MinIO 通过带 CRC32C 和 FULL_OBJECT checksum 的缓冲流上传源对象，在读取上传流时计算 SDK checksum。Compose 时，OSS 请求各源对象的原生 checksum 元数据，要求 FULL_OBJECT CRC32C，再按源顺序，用真实长度组合已验证 CRC。CRC32C 线性化得到拼接后字节的校验和，不是 hash-of-hashes 复合哈希。服务端 compose 复制前，将该值与预期 CRC32C 比较，并通过 ETag 固定每个源对象。不下载对象正文，不重新读取源文件，也不将用户元数据当作校验依据。MinIO multipart-copy handler 不向 checksum reader 提供数据，因此直接在此路径请求 checksum 会失败，见 [MinIO handler](https://github.com/minio/minio/blob/RELEASE.2025-07-23T15-54-02Z/cmd/object-multipart-handlers.go)。合成对象无需暴露原生全对象 CRC32C 响应头；OSS 边界通过不可变源快照完成验证。

普通批量上传和文件复制使用独立的配额操作锁，不使用缺少 checksum 的上传会话行。管理员上传记录列表现在表示可恢复会话；测试初始化会话，并检查两个摘要值经 PostgreSQL 和 API 往返保持一致。直接上传文件列表的覆盖仍保留。

此 alpha 协议没有旧格式回退。新客户端与服务端一起使用前，按更新 schema 重建已有 alpha 数据库，并清除旧可恢复上传状态。不得用虚假 checksum 填充旧行。实现不会重置任何运行中的数据库。MinIO checksum 集成测试使用 RELEASE.2025-07-23T15-54-02Z；registry 拒绝匿名拉取时，可导入已验证的上游缓存镜像。Cloud server 集成测试保持 PostgreSQL、filesystem RPC 和 Lucene RPC 拓扑；MinIO 测试在 `backend/minio` 单独运行。

Linux 服务端图片测试还需要 libavif 原生共享库及 codec 依赖。若通过隔离运行时提供、而非全系统安装，则通过 LD_LIBRARY_PATH 暴露其目录。

### Shell 工具回归测试

支持 Windows 和 Linux。需要 Python 3.10+、Bash 4.4+（Windows 使用 Git Bash）和 [PyYAML](https://github.com/yaml/pyyaml)。先在 Python 环境安装测试依赖，再执行：

```sh
python -m pip install -r scripts/test_scripts/shell_regression/requirements.txt
python scripts/test_scripts/run-shell-tests.py
python scripts/test_scripts/run-shell-tests.py --list
python scripts/test_scripts/run-shell-tests.py --suite compose
python scripts/test_scripts/run-shell-tests.py --suite infrastructure
python scripts/test_scripts/run-shell-tests.py --tests test_compose.ComposeTests.test_build_mode_comes_from_env
```

`sh scripts/test_scripts/run-shell-tests.sh` 是可移植 Shell 入口，必要时通过 `PYTHON_COMMAND` 指定解释器可执行文件。`SHELL_TEST_BASH` 选择 Python 子进程使用的 Bash。已有五个 `*-test.sh` 入口分别选择对应 unittest 模块，并转发额外参数。`build-and-test.sh --compose --plain` 现在在 Detekt 后执行完整 Shell 回归套件，不启动真实 Docker、Android 或桌面 UI 会话。

每个参数行都是独立 unittest 测试 ID，因此一个约定失败不会阻止无关用例。迁移覆盖原有分组：

| Shell 入口 | unittest 模块 | 约定 |
| --- | --- | --- |
| `build-and-test-args-test.sh` | `test_build_and_test` | 空格和 glob、拒绝参数、准确 Detekt/E2E 调用、任务过滤和退出状态、平台目标选择 |
| `build-service-images-test.sh` | `test_images` | 四个并发构建、宿主本地与 Docker 构建的区别、完成和失败状态 |
| `cli-entrypoint-test.sh` | `test_cli` | 就绪响应、禁用预设初始化、文件与 stdout 日志、失败状态 |
| `compose-service-test.sh` | `test_compose` | Profile、依赖健康条件、RPC 环境、证书、Bunker 环境和网络、外部路径、Docker 构建与启动顺序、失败阻止部署、部署配置约定 |
| `run-linux-desktop-e2e-args-test.sh` | `test_desktop` | 任务和全局选项、空格与 glob、成功或失败清理、无关进程保留、启动前拒绝过滤器 |

每次调用创建唯一 `build/test-results/shell-regression/run-*`，包含 `discovered.txt`、`junit.xml`、`junit.raw.json` 和各用例 stdout/stderr。JUnit 独立记录每个发现的用例，包括失败与错误；失败时执行器返回非零状态。夹具仓库、合成 env 文件和模拟命令按用例隔离，由 unittest cleanup 删除。生成的 YAML 和 NUL 分隔命令 argv 在夹具内准确解析，不读取或删除真实 `deploy/*.env`。生产 Shell 脚本复制到夹具后以子进程执行，只规范化夹具换行；测试不在 Python 重写其行为。Bash 只负责传输准确 argv，执行遵循脚本 shebang 或显式解释器，包括 CLI 入口的 `sh`。工作检出中已有生成 Compose 补丁不在此套件清理范围内。

执行管理者为 `scripts/tool_scripts/process_container.py`，与 `client/composeCore/wasm-media-host` 的浏览器验证兼容入口共用。Windows 将暂停的 root 分配到 kill-on-close Job 后才恢复；Linux 使用专用 subreaper，接管脱离的后代。完成要求空容器确认、root 退出和管理者退出。执行期限默认 45 秒，与清理期限独立。输出直接写文件，继承的 pipe handle 不会延迟超时交付。超时或调用方中断时，在删除夹具前请求清理整个所管容器。每次调用在启动前注册进程租约，从 not-started 进入 owned，只有获得完整确认才变为 released；不能靠异常类型判断删除安全。夹具清理要求所有注册租约均安全，因此后续成功命令不能覆盖之前未确认的释放。中断与清理同时失败时，中断仍是主异常，清理失败保留为 cause。进程终止和 control-pipe 关闭共用一个清理边界；先尝试全部清理操作，再传播结果；多个失败保留在 `ProcessCleanupError.errors` 和异常链。即使进程已确认释放，仅清理失败也会使命令失败。真实断开 pipe 回归覆盖 close/flush 失败与中断、I/O 错误、超时的组合；pipe 关闭错误绝不改变独立确认的租约状态。

基础设施回归覆盖活动或已退出 root、脱离后代、无关进程保留、解释器模式、非法 XML 字符，以及中断或 I/O 失败与清理失败的组合。旧 Shell 入口和 bootstrap 在 `.gitattributes` 中配置 LF 检出。回归通过 `--list` 执行工作树原文件，检查原始字节并比较全部发现 ID，不执行这些入口的换行规范化夹具副本。

JUnit 序列化在全部诊断字段转义 XML 1.0 禁止字符；原始 stdout/stderr 和无损 `junit.raw.json` 保留原诊断。原有浏览器容器生命周期套件仍可通过 `node --test client/composeCore/wasm-media-host/verification-reports.test.mjs` 运行。

### 部署与 RPC 生命周期验证

应用后端环境覆盖只为启用的 cli/server/worker/ws 生成。容器 Server 和 WebSocket 监听 80，发布宿主端口仍可配置。Filesystem public URL 来自 flavor 文件，同一 Compose 解析值传给 filesystem 和应用。Bunker 凭据与域名前缀设置使用 deploy/bunker.env.example 的最终变量名，仅通过 raw env_file（Compose 2.30+）注入 BunkerWeb。已有使用 BUNKER_* 占位符的外部文件必须按示例重写，不自动迁移或覆盖。

运行 scripts/test_scripts/compose-service-test.sh 验证生成配置回归。部署前使用不含机密的夹具环境文件验证真实 Compose 组合。MinIO 运行时镜像安装 curl，并为 UID 1000 初始化 /data，使新 named volume 可用。文件搜索同时索引原始分词名称和不存储的小写完整名称字段；查询搜索两者，响应保留原名。Filesystem RPC 上传状态属于一个 WebSocket 连接；会话完成时等待调用停止，再清理未完成上传。断开回归关闭一个真实 RPC WebSocket，验证其临时文件消失，而不影响另一连接。

Worker 使用固定摘要的 Fedora 44 基础镜像，从官方 [Adoptium RPM 仓库](https://adoptium.net/installation/linux/#centosrhelfedora-instructions) 安装 Temurin 21 JRE，以支持链接 glibc 的 LiteRT JNI 库。Fedora 签名仓库提供 libavif、Vulkan loader、libstdc++、Noto CJK/mono 字体和 HarfBuzz core/subset（要求 13.0.0 或更新版本）。无需从源码构建原生库。共享 Docker build stage 仍使用 JDK 21 Alpine；Worker 软件包安装和用户创建保持 Fedora 专用。

### RPC 适配层恢复

Lucene 与 filesystem 适配层保留 RpcServiceProvider，不永久保留 RPC proxy。ManagedRpcService 用协程 Mutex 串行建立传输，发布新连接前执行一次 health 调用。每次业务操作只获取一次；filesystem 上传批次和分块下载在整个操作期间固定该 proxy。断开的连接只为后续操作替换。失败读写绝不自动重放，部分上传绝不在另一 WebSocket 继续。调用方取消继续传播；建立失败或取消时关闭候选连接，不填入缓存。WebSocket 完成时关闭对应客户端资源。

Loopback 恢复测试保留同一适配层，在同一端口重启真实 CIO RPC 监听，检查共享并发重连、中断写入、上传、调用方取消、建立连接取消和 Mutex 等待者取消。Core 容器测试替换独立 Lucene/filesystem 容器，显式绑定原发布端口，不重建适配层。Docker 随机分配的发布端口在普通容器重启后可能变化。Ktor loopback 夹具启动显式调用顶层 embeddedServer，避免 CoroutineScope 重载让启动作用域等待整个服务生命周期。

环境选择共用于 scripts/service_scripts/service-env.sh。它解析名称或文件路径，不把 dotenv 当作 shell 代码 source，导出 APP_ENV_FILE 供全部应用 env_file 使用，并从所选文件解析 BUNKER_ENV_FILE，相对路径以该文件目录为准。通过 compose-service-test.sh 和真实 Docker Compose config 验证明路径（含空格）、相对 Bunker 路径及名称回退。全新 Docker Wasm 构建拒绝锁文件、而增量宿主构建接受时，先比较生成 package.json，再考虑改锁。旧间接 npm 项可能留在生成的应用 manifest；依赖变化后重新生成 manifest，并在实际部署平台验证严格锁存储。

部署浏览器检查必须完整读取 Wasm 响应，并操作实际渲染和认证功能。App 和 panel Permissions-Policy 必须在 COOP/COEP 之外允许 cross-origin-isolated=(self)。媒体 CORS 属于 BunkerWeb：启用 USE_CORS 并配置 app/panel origin 允许列表，使 OPTIONS 返回 204，而不是进入对象 handler 并触发 bad-behavior 封禁。仅刷新配置、复用已有 Docker 构建产物时，使用 scripts/service_scripts/start-service-in-docker.sh <environment> --no-build；默认命令仍在 Docker 内构建产物。Panel 登录使用所选签名算法，必要时使用其加密密钥。Wasm 需提供显式 ViewModel initializer，默认 factory 无法构造登录 ViewModel。PanelInputHost 接收协调和工作调度器，管理输入状态直到父生命周期结束。

### Redis 登录认证

App 退出登录以确认替换账户模态，不在一个 Dialog 内嵌套确认 Dialog。PanelSignoutHost 在注入的串行协调器上管理确认；账户 GlobalDialog 管理请求、受保护历史退出及仅成功后重置账户。

Bunker 部署将 signin-redis 接入与 Server 和 WebSocket 相同的 bw-services 网络。升级已有 bunker.env 时，按 deploy/bunker.env.example 保留 API CORS_ALLOW_HEADERS 的 X-Account-Id，认证浏览器请求需要它。

`cloud/runtime` 注册独立用户和 panel 会话提供方（`user_session`、`panel_session`）。Cookie 包含随机会话 ID；Redis key 对凭据做哈希，隔离两个命名空间。待完成登录和 2FA challenge 有效五分钟。登录完成轮换凭据。成功会话具有 30 天空闲和 90 天绝对时限。Redis TIME 和原子 Lua 操作负责过期、活动更新与撤销；普通读取不续期。账户索引支持撤销全部设备。Redis 必须使用持久、不可驱逐存储；恢复旧备份可能恢复已撤销会话，因此灾难恢复必须在接受流量前使恢复的认证数据失效。

原生 GUI 会话存储与 key history 分开，由 HTTP 和 WS 共用。浏览器 Cookie 由浏览器管理，不使用 Ktor 内存 jar。客户端收到 401 时结束账户作用域，要求显式登录；503 或网络失败保留凭据。修改操作绝不自动重放。浏览器 Origin 必须包含在 `SIGNIN_ALLOWED_ORIGINS`；原生客户端允许没有 Origin。生产 Cookie 使用 Secure、HttpOnly、SameSite=Strict。测试和开发 HTTP 通过 BUILD_TYPE 显式启用；生产要求 HTTPS/WSS 和同站浏览器托管。

WS 在每条业务消息前、空闲时每五秒重新检查 Redis；检查不续期活动，显式向外发送的业务消息才更新活动。Redis 不可用会暂时关闭 WS。客户端直接带 Cookie 打开 WS。只有握手失败才探测 HTTPS 会话端点，因为浏览器握手错误不提供可靠 HTTP 状态。策略关闭使本地登录失效。持续握手失败在三次尝试后停止，并暴露显式房间输入刷新操作；临时 HTTPS 失败退避到一分钟。不重放任何发出操作。

指针和键盘输入通过认证活动端点报告，更新按五分钟合并。用户或 panel 的 sign-out-all 端点原子删除该账户 Redis 会话。`cloud/runtime` 测试使用隔离 Redis 容器；服务端集成夹具除 PostgreSQL、filesystem/Lucene RPC 外也包含 Redis。客户端宿主测试使用 `./gradlew :client:core:jvmTest :client:composeCore:jvmTest -Pserver.flavor=sample -Pserver.buildType=test`。Redis 回归通过配置的 Docker 兼容后端运行 `:cloud:runtime:test`。

认证 Appium 夹具现在连同合成账户 key 一起注入显式准备登录所得 Cookie。原生夹具将 Cookie 加入隔离内存 jar；浏览器夹具通过 WebDriver 在自有 bootstrap origin 安装。Wasm 分发服务先于后端启动，使准确 origin 可加入允许列表。这替代旧的保存 key 自动登录假设，不构成生产认证绕过。

恢复的账户地址和公开 X-Account-Id 断言，避免共享浏览器 Cookie 静默改变已有页面身份。升级时向网关 CORS allowed headers 加入 X-Account-Id。该断言从不作为认证，只发给 API origin。WS 在 URL 携带公开预期账户 ID，并在注册订阅前验证。

显式登录通过 get-data 获取服务端新生成的 UUID challenge，不接受客户端时间戳作为认证 challenge。开始重新认证会使当前角色会话失效；每个 get-data 都要求 X-Requested-With: A-client，防止普通浏览器导航签发待完成 Cookie、覆盖已有登录。旧登录签名不能利用调用方选择的时间戳创建另一会话。普通 401 challenge 不会仅因预期账户 ID 不再匹配，就使另一标签页的有效会话失效。

### 受保护的客户端账户历史

SessionHistoryManager 操作为挂起操作。账户历史包含全部私钥和选中账户，使用与原生 Cookie 相同的受保护值 expect/actual 边界。Settings 依赖和 AndroidKeyStoreUserPass 已移除。旧 Settings 历史不读取、不迁移也不删除，用户需重新导入账户 key。新受保护写入必须读回验证后才报告成功。用户和 panel 历史使用独立存储 key。Redis 过期或撤销后，不得用保存的 key 静默重新认证。

Android 使用 AES-GCM 和 Android Keystore key 加密记录，将密文提交到私有 SharedPreferences `protected-data`。两个 Android 应用均将其排除在云备份和设备迁移之外，因为 Keystore key 无法一起迁移。Windows 使用原子文件替换，将 DPAPI 密文存储于 `~/.a/protected-storage/`，避免 Java Preferences 的单值大小限制。macOS 通过原生 Security framework 调用将机密存入 Keychain，避开 `security -i` 交互解析器的 4096 字节命令限制，也不将机密放入进程参数。已有 Base64 payload 格式保持兼容。设置 `app.protected.storage.dir` 时，macOS 账户名包含其哈希 namespace，避免独立测试共享历史；未设置时生产名称不变。Linux 通过已有 JNA 依赖使用 libsecret 二进制 Secret Service API。每次操作在 IO 执行，管理自己的 `GCancellable`，保留参数直到取消完成，并维持已有超时。`secret-tool` 会将 stdin 静默截断到 8192 字节，无法保存任意账户历史，见[上游读取器](https://github.com/GNOME/libsecret/blob/main/tool/secret-tool.c#L248)。Linux 要求 libsecret 0.19 或更新版本。已有原始 UTF-8 值和 Secret Service 属性保持兼容，不添加明文回退或重放。显式 `app.protected.storage.dir` 也为 Linux 凭据名划分作用域，隔离测试历史；未设置时生产名称不变。受保护存储不可用时，账户历史持久化失败；Cookie 持久化可保留内存登录，但绝不回退到明文。浏览器账户记录和不可导出的 AES-GCM key 使用 IndexedDB，记录名作为附加数据认证。这保护已存 key 字节，但同 origin 的脚本仍能请求解密，不具备登录 Cookie 的 HttpOnly 边界。WebCrypto 要求 HTTPS 或 localhost。

运行已编译 Wasm 存储边界回归：`python scripts/test_scripts/test-browser-protected-storage.py --browser <Chrome-or-Edge-executable>`。执行器针对每个约定单独调用 `:client:composeCore:wasmJsBrowserTest`，使用 `--rerun` 和测试过滤器，让 Karma 为各用例启动全新可丢弃浏览器 profile 和隔离 origin 存储。`--test <method-name>` 只运行一个约定，不从 Kotlin 源码提取字符串。测试调用与 `readProtectedValue`/`writeProtectedValue` 相同的已编译 `BrowserProtectedValueStore`，使用唯一数据库；即使失败也关闭连接、删除自己的数据库。持久化测试在读取前关闭并重建连接。独立用例覆盖空读取、账户隔离、密文存储、不可导出 key、篡改拒绝及记录名认证。检查辅助方法只查看或破坏原始浏览器记录，不重复加解密。各用例 Gradle 日志、JUnit XML 和 HTML 报告位于 `client/composeCore/build/test-results/browserProtectedStorage/<browser>/<case>/`。普通 `:client:composeCore:wasmJsBrowserTest` 在一个 Karma 浏览器中一起运行相同测试，各自使用独立数据库；需要按用例隔离 origin 时，使用 Python 执行器。两条命令都需要 JDK 21 和 Chrome 兼容浏览器；直接 Gradle 调用使用 `CHROME_BIN`，必须传入 `-Pserver.flavor=dev -Pserver.buildType=debug`。模块 Karma Edge launcher 关闭 Windows 兼容 shim 重启，使 Karma 保持进程归属，并分配动态 DevTools 端口。执行器要求每次调用恰好执行一个成功 JUnit 用例；缺失、跳过或重复均验证失败。客户端宿主测试还覆盖受保护历史恢复，以及写入失败或无法验证时保留已存状态。已有历史测试使用隔离的受保护 namespace，并将 `app.protected.storage.dir` 放在模块 build 目录，避免访问真实账户存储和 Java Preferences。

登录 proof 在创建账户或首因子登录之前、TOTP 验证成功之后，在 Redis 中原子消费。首因子和 TOTP 转换轮换 Cookie，并发重放不能凭同一 proof 再签发会话。匿名公开和 401 请求不创建待完成 Cookie，避免较旧后台响应覆盖刚登录的浏览器 Cookie。

登录存储使用 `backend/core` 的 SigninSessionService / SigninSessionServiceFactory 约定，与其他后端服务采用相同 ServiceLoader 注册模式。`backend/redis` 在 META-INF/services 注册 Redis 提供方。`SIGNIN_SESSION_SERVICE` 默认 `redis`，提供方未知或端点缺失时启动失败。`cloud/runtime` 只将服务适配到 Ktor SessionStorage，并在应用停止时关闭已注册服务，不自行构建 Redis driver client。已有 service-file 合并将此提供方包含在 server/WS 分发包中。回归测试构建已注册提供方，同时验证注册和 Redis 存储行为。

`compileAllNoRelease` 聚合先过滤 `tasks.names`，再获取各 `tasks.named` provider，保证依赖选择不会实现被排除任务。不要替换成过滤 task collection（`tasks.matching` 或 `tasks.named { ... }`），依赖展开仍可能实现排除任务。AGP 9.5 为没有 unit-test component 的变体注册 Compose Preview 任务，实现被排除的 release Preview 会在编译开始前失败。

Gradle 默认启用项目并行执行。Detekt Worker API 保持关闭，因为 Detekt 2.0.0-alpha.6 启动的进程隔离 worker 无法配置堆，Compose 分析会耗尽其堆。Detekt 改在 6 GB Gradle daemon 内执行，内部并行 AST 分析仍启用。编译和 Detekt 的 worker 容量遵循 Gradle 默认值，不强制 Detekt 使用 --max-workers=1。编译与 Detekt 分开调用。Docker 构建继承相同项目设置。

Detekt 默认只读检查（`autoCorrect = false`）。显式应用支持的自动修正时，单独运行：

```bash
./gradlew detekt --auto-correct -Pserver.flavor=dev -Pserver.buildType=debug --console=plain
```

其余并发边界：Appium 任务使用一个测试 fork 和按平台划分的一次使用 Gradle build service（android、desktop、browser），在各平台内串行执行 App 与 Panel，同时允许不同平台在一次 Gradle 调用中并行。这些服务不协调独立 Gradle 进程，因此仍需共享设备租约。标准 --e2e 执行器在一次带 --continue 的 Gradle 调用中提交六个 Appium 和两个 CLI 目标，使某个测试失败后独立平台仍可推进。测试过滤应用于每个目标。CLI E2E 将全局 Gradle worker 数作为 fork 容量，App、Panel 任务及独立测试类可并发；目前各自只有一个包含单场景的类，所以已有套件每目标最多一个进程。测试 Docker 镜像使用独立不可变上下文并发构建。部署将全部所选目标放在一次 Compose build；独立镜像辅助程序并发启动四个构建，并等待全部结果；BuildKit sharing=locked 串行化共用 Gradle 缓存的 builder。CI concurrency 取消同一 workflow/ref 的过时运行，不限制构建内部 worker。已审查宿主测试允许列表之外的单元任务，保留 Gradle 默认单 fork；独立项目任务可并发。

已审查的 api、shared、client/core、client/composeCore、client/asciidoc-parser、backend/redis、app/composeApp、panel/composeApp、cloud/service、services/lucene-protocol、cloud/filesystem-service、cloud/lucene-service、dev/appiumCore 和 cloud/worker 宿主测试任务，默认 fork 容量使用 Gradle 最大 worker 数，不设置独立固定上限。已审查的 backend/core、backend/filesystem、backend/lucene、backend/minio、cloud/runtime、cloud/server 和 cloud/cli 容器测试任务也使用默认 worker 容量。可用 -PunitTest.maxParallelForks=1 或其他正数覆盖这些宿主任务。UI、其他持久化和 Appium 任务保留已有 fork 设置；全部 fork 仍受 Gradle worker 容量约束。

Wasm 编译没有专用并发上限，遵循全局 Gradle worker 预算。六个生产编译任务显式一起重跑，使用 8 GB Kotlin daemon，1m29s 通过。堆采样接近容量且发生大量 full GC，因此只验证当前六任务图，不代表任意未来负载。

默认 Kotlin daemon 最大堆为 8 GB（gradle.properties），在 32 GB 宿主观察到 Wasm 堆压力后从 6 GB 提升。这是最大值，不是内存预留；应为 Gradle、测试 fork 和原生工具保留足够内存。

Gradle daemon 最大堆从 4 GB 提升到 6 GB，为进程内 Compose Detekt 分析提供余量。Kotlin daemon 堆独立配置，保持 8 GB。

Detekt 2.0.0-alpha.6 类型解析有两项相关上游限制。扁平化 KMP 源码模型会拒绝独立 source set 中合法 expect/actual 声明（[detekt #9602](https://github.com/detekt/detekt/issues/9602)）；分析模块中由编译器插件生成的 serializer 方法也可能不可见（[detekt #7531](https://github.com/detekt/detekt/issues/7531)）。在不改变现有 serializer 和 JSON 约定的前提下，使用序列化库公开的泛型 encode/decode 和多态 subclass API，避免直接引用生成方法。Fragment 模型限制仍在时，保留 KMP 源码覆盖和类型解析。这些消息来自 Detekt 内部编译器诊断，不是具名 Detekt 规则。该版本不支持逐诊断过滤：debug 控制详细输出，但仍显示 compiler-error 计数。不要关闭无关类型感知检查或排除源码来隐藏消息。上游修复可用后重新评估。诊断时关闭 autoCorrect、启用 Detekt 任务 debug，并用 --rerun 重跑；仅改变 debug 不会使缓存失效。将编译器诊断与实际 Kotlin 编译比较，不能仅凭 Detekt 任务成功就声称类型解析完整。

扩展的宿主测试允许列表包含进程内 Ktor 夹具，以及使用唯一临时目录的本地 Lucene/filesystem 测试。App JVM 已排除 device_based UI，其 protected-history 测试限制于一个类。dev/appiumCore 测试覆盖桥接和 capability 辅助方法，不是 Appium 套件。普通测试与 Docker 集成混合的模块（backend/core、backend/minio、backend/filesystem、backend/lucene、cloud/runtime、cloud/server 和 cloud/cli），在审查每容器映射端口和隔离存储后，现使用相同基于 worker 的 fork 容量。cloud/worker 在审查全部五个测试类后也加入：LiteRT 测试替换后端 factory，不初始化原生引擎、不执行模型推理；其他测试覆盖客户端构造、JSON／配置、调度和审核逻辑。Fork 容量不预留 VM 资源：4 GB／4 CPU 测试客体无限制启动容器时出现 Elasticsearch 就绪超时。七模块验证使用 -PunitTest.maxParallelForks=4，通过 106 项；这是该次运行覆盖，不是项目默认。之后本地测试 VM 升到 8 GB，在默认 28 worker、无 fork 覆盖下，同样 106 项通过，客体内存峰值 6290 MiB。后端资源容量和私有生命周期配置属于操作者，公开服务端口范围应与环境传输一致。client/kotbase 与 client/sqlite-now 未包含在 settings.gradle.kts 中。

登录生命周期回归位于 cloud/runtime RedisSessionStorageTest 和 cloud/server SigninLifecycleTest。后者使用已有 PostgreSQL/Redis/filesystem/Lucene 夹具，检查真实 HTTP/WS Cookie 共用、撤销后策略关闭，以及 panel 会话恢复与撤销。管理员注册必须立即签发成功 Cookie；会话过期后 signout 幂等，signout-all 则要求已认证账户。

在已配置 Docker 测试环境，用 `./gradlew :app:wasmAppium:appiumTest --tests '*test browser socket*' -Pserver.flavor=dev -Pserver.buildType=debug --console=plain` 运行三浏览器 WebSocket 回归。测试显式 UI 登录，并使用仅测试观察器记录原生 WebSocket 连接与消息数，覆盖传输断开及无消息重放恢复、认证浏览器重启、退出、浏览器 Cookie 删除或过期、服务端撤销或过期、已撤销或过期 Cookie 重放，以及拒绝未获准 Origin。观察器不存凭据、URL 或消息正文。服务端过期测试保留浏览器 Cookie 和已存 key，将隔离 Redis 会话截止时间移到过去，不缩短生产超时。Redis 空闲和绝对过期另有存储层覆盖。

快照签名配置在 cloud/server 边界加载为内存 Base64 PKCS#12 值，与 PDF 适配层共用。OpenPDF 和 PdfBox 都读取缓冲内存流。旧文件配置在启动时转换；Base64 配置绝不写出解码后的私钥文件。SnapshotKeyStoreTest 覆盖来源优先级、旧输入、无效 Base64、缺失或错误密码及脱敏诊断。SnapshotTest 使用 Base64 输入生成签名 PDF。

从隔离源码检出部署时，首次 `up` 之前重新连接已有 filesystem 和 Lucene bind-mount 目录；不同于 named PostgreSQL volume，这些目录相对于检出解析。修正错误挂载时，另行保留新建目录。Compose 包装器在 `up` 后重新加载 BunkerWeb Nginx，使容器替换后解析当前服务地址。

Dockerfile 基础镜像和 build frontend 保留可读版本 tag，并固定 manifest digest。升级时，通过 `docker buildx imagetools inspect <image>:<version>` 解析各上游 manifest，再显式更新摘要。临时 registry 连接失败先重试几次，再进一步调查。

## 跨平台 RTC

共享 `RtcCallHost` 独立于 Compose 管理通话状态。每个远端用户有独立 peer job 和带缓冲的信令队列。远端 SDP 就绪前，ICE 进入队列；某 peer 离开不能清除其他 peer 状态。原生回调携带 peer 身份，迟到回调不能修改替换后的 peer。资源获取与关闭在取消期间仍保留管理责任。页面管理通话生命周期。共享 WebSocket 分派器将接收方 channel 取消转换为交付失败，同时保留自身取消，避免一个连接断开停止其他房间协商循环。

Android、JVM、Wasm 和 iOS 的媒体使用 Ktor 3.6.0 实验性 `ktor-client-webrtc` API。平台引擎、权限提示、渲染器和原生清理封装在 `RtcEngine` 后。Ktor JVM 引擎使用 webrtc-java 0.16.0：关闭连接前分离 sender track，释放 track 前移除 video sink，并显式关闭 media factory 和本地 audio track。远端 JVM track 从连接借用。帧转换在 worker 执行，正确配对原生帧 retain 和释放。Android EGL/media factory 由应用管理。浏览器音频属于 peer，不属于 lazy video tile，避免滚动使音频静音。浏览器空字符串 ICE candidate 表示收集结束，应在共享 RTC 边界过滤，因为原生适配层会拒绝它。非空 candidate 错误仍传播。原生协商回归检查收到这些标记后仍有解码帧。

Ktor 3.6.0 仍调用已移除的 `js.core.JsPrimitives` 转换 ABI。仅 Wasm 使用的内部兼容对象将所需四个转换转发到 Kotlin 标准 JS interop。为保留 Kodio URL API，应保留较新 Wrappers，不降级整个 wrapper 依赖图。Ktor 针对新 ABI 重建后，移除该对象。Web 可执行程序将 partial-linkage 诊断视为错误。浏览器测试覆盖真实 constraint 转换、合成音视频采集、SDP 生成和 track 清理。

浏览器媒体采集将 Wasm `JsException` 失败规范化为 Ktor 设备异常。缺少采集设备必须使通话回到失败的 ready 状态；DOM 异常可能没有 JavaScript 栈，不要将其保留为 Kotlin 格式化诊断异常的 cause。

运行 `./gradlew :app:composeApp:jvmTest --tests "*Rtc*Test" :app:composeApp:testAndroidHostTest --tests "*Rtc*Test" -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`，覆盖共享状态与信令，以及原生 SDP/ICE/清理。原生 JVM 夹具使用合成音视频源和真实时间。编译 Android、桌面和 Wasm 适配层；iOS 使用 macOS 和 `-Ptarget.ios=true`，Xcode target 链接 WebRTC 137.7151.04。真实多客户端检查必须覆盖权限、媒体、加入和离开、静音、切换相机，以及超过四名参与者后的滚动。纯音频加入协商 audio track。浏览器呈现使用 `requestVideoFrameCallback` 和递增 `presentedFrames` 验证。Firefox 在帧已到 compositor 时，仍可能令实时 MediaStream 的 `getVideoPlaybackQuality().totalVideoFrames` 为零。入站音频包需单独验证。

浏览器 origin 需要 HTTPS，`Permissions-Policy` 允许 `self` 使用 camera、microphone 和 autoplay，无关 origin 保持限制。部署后，将浏览器实际加载的 Wasm 入口指纹与运行镜像比较，包括带版本 Compose 资源。

静态客户端 Nginx 对 Wasm、JavaScript、CSS 和 SVG 响应压缩。部署后，以 `Accept-Encoding: gzip` 请求入口 Wasm 资源，验证 `Content-Encoding`/`Vary`，并将解压字节与镜像比较。HTML 缓存验证保持开启。仅配置发布需重建静态容器，以重新挂载被替换的 bind-mounted 配置文件，再重新加载代理上游解析。

### Worker 模型解析

KoogClientFactory 对直接提供方只接受目录中的精确 ID：OpenAI、Anthropic、Google、Ollama、Bedrock、DeepSeek、Mistral AI 和 DashScope。元数据来自 Koog，不手工添加别名或回退限制。默认值引用 SDK 常量。Google、DeepSeek、Mistral AI 和 DashScope 模块以 1.3.0-beta 发布，与稳定 Koog 1.3.0 模块配套。

分配客户端前先解析模型。OpenAI-compatible 端点传递不透明模型 ID，不附加 capability 或 token 限制。OpenRouter 也接受任意非空 ID；其 LLModel 只声明通过 Koog 本地请求验证所需的 completion 和标准 JSON-schema 操作，真实支持由远端路由器验证。两条路径都不推断 context 或输出 token 上限。LiteRT 保留本地模型元数据。

Google 将 GoogleModels 与原生 GoogleLLMClient、GoogleClientSettings 配套。Bedrock 使用原生 Converse，以保留标准 JSON-schema 输出。Factory 接受 bearer API key 或 AWS SDK 默认凭据链、region 和可选 runtime endpoint，请求只尝试一次。显式 API key 的 HTTP bearer 认证优先于环境 AWS 签名凭据。Runtime SDK 自行构造并管理默认凭据链，绑定配置 region，关闭时连同 runtime HTTP engine 一起关闭凭据缓存。Koog DeepSeek 适配层使用 JSON-object 模式，不传输 JSON schema。服务保留调用方 system 指令，并将 schema 放入该提供方 system prompt；审核解析器仍验证返回决策。可选 region 字段与已有序列化 LlmConfig 记录向后兼容。

迁移有意拒绝未知直接提供方 ID。更新已存 ID，使其匹配目录；自定义兼容端点使用 OPENAI_COMPATIBLE。Google endpoint 覆盖必须使用原生 API root，不再使用旧 OpenAI-compatible 路径，配置见 README.md。OpenRouter ID 保持不透明，无需目录迁移。

GoogleLlmExecutionTest 和 OfficialProviderExecutionTest 在临时 loopback HTTP 夹具上测试原生路由、认证、提示词、请求 schema 和响应解码。BedrockFactoryExecutionTest 通过 KoogClientFactory 创建客户端，只替换网络传输，验证 signed/bearer Converse。合成 JVM 凭据验证 API-key 优先级、region 签名、endpoint 路由和 schema 转换。检查 SDK provider 验证默认和显式 region 传递；两种认证模式下，关闭服务必须关闭真实默认凭据缓存及其 HTTP engine。测试恢复 JVM properties，并串行访问这些属性。HTTP 夹具使用真实 IO 调度器和唯一端口，无需真实云账户或模型调用。

运行 `./gradlew :cloud:worker:test -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`。

### Panel Android 返回回归

`:panel:androidAppium:appiumTest` 的 `*drawer back*` 用例在窄屏验证系统 Back 先关闭抽屉、保留用户列表，下一次 Back 才回概览。`*predictive back*` 两项用例要求 API 34+、窄屏及系统手势导航（`navigation_mode=2`）；运行前由设备管理者配置，持有设备租约并在结束后恢复设置。用例比较排除系统栏与边缘箭头的页面区域，断言手势中页面发生变化、取消后恢复原帧且页面栈不退，输出起始、进度与恢复 PNG。它们不能由三键导航或普通 Back 替代。

### Appium 产物报告

每次 Android、桌面和 Wasm 辅助方法调用，都在执行模块 `build/reports/<test task>/index.html` 写报告。打开索引可查看准确测试名、平台、结果、失败栈和全部收集产物。各用例报告链接可读后端控制台日志、服务日志归档、应用日志、截图或 page source、Android 录像和可用失败诊断。收集错误与原测试失败分别显示。

服务端集成、CLI E2E 和 Appium 将原始文件写入 `build/test-results/<test task>/artifacts/<test class>/<test name>/`，Appium 额外添加平台或浏览器子目录。名称保持可读，不加哈希、随机后缀或尝试计数；文件系统非法字符和保留设备名使用百分号编码。每用例写入 `result.properties`、日志、截图及其他收集文件，不生成 HTML，不扫描其他用例。

`test-artifact-reports` 约定管理两个阶段。Test 任务实际执行前，只清除该任务原始产物目录；缓存或 up-to-date 的 Test 任务保留结果。随后 `<test task>ArtifactReport` JavaExec finalizer 在 `build/reports/<test task>/index.html` 生成独立报告，包含产物副本和安全 HTML 查看器。测试失败后也会运行。Gradle 原生 JUnit XML 和 HTML 仍在正常位置。可直接执行 finalizer，从已保存原始结果重建报告，不运行测试。同模块、同任务输出目录的独立构建必须串行。导出时保留完整报告树，确保相对产物链接可用。私有注入会话和桌面运行时存储在报告生成前移除。

Appium 收集生命周期测试使用 `./gradlew :dev:appiumCore:test -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`，无需 Appium 服务或设备。共享元数据、HTML 查看器和生成器测试位于 `:dev:testInfrastructure:test`，其完整套件还包括真实 Redis 启动失败测试。旧 Appium 查看器测试已随共享渲染器迁到该模块；生命周期断言先验证原始元数据，再显式调用聚合，核查生成页面和链接。

产物链接打开静态 HTML 查看器，不依赖服务端 MIME 映射。日志、文本、XML、HTML 内容会转义；tar、tar.gz 和 zip 展示归档目录及文本预览，不解压 entry 路径。图片和录像嵌入查看器。原文件通过明确的下载链接保留。文本预览每文件或 entry 限 2 MiB，归档最多 100 项、总预览文字最多 8 MiB。

Filesystem 和 Lucene 服务各自打包 Logback provider 和配置，均输出控制台和滚动文件日志。服务事件直接使用 SLF4J；INFO 到 stdout，WARN/ERROR 到 stderr，全部启用级别写滚动文件，与 server/worker 一致。Promtail 从共享 volume `/var/log/A/<service>/*.log` 读取，附对应 `job` label 并推到 Loki。`LOG_PATH` 控制目录；未设置时，独立运行使用 `build/logs/filesystem`、`build/logs/lucene`。运行时镜像创建非 root 服务用户所有的 `/home/app/logs/<service>`，Compose 将已有 `service-logs` volume 挂载供 Promtail。Appium 在关闭容器前导出控制台及完整 filesystem/Lucene 文件日志目录。两处都必须出现服务专用启动消息，仅 SLF4J provider 警告不能证明收集成功。Loki 可查询 `{job=~"server|worker|ws|cli|filesystem|lucene"}` 获取后端日志，或使用 `{job="filesystem"} |= "Filesystem service starting"` 和 `{job="lucene"} |= "Lucene service starting"` 验证独立服务。确认返回记录包含对应 `/var/log/A/<service>/` 文件名；容器 stdout 不能单独验证共享 volume、Promtail 和 Loki 整条链。

### 共享 Testcontainers 夹具

`dev/testInfrastructure` 管理服务端集成、Appium 和 CLI E2E 的测试拓扑。`useBaseServiceFixture` 创建隔离网络、PostgreSQL、Redis、filesystem 和 Lucene 服务。`hostEnvironment` 包含映射后的宿主端点，`containerEnvironment` 包含 Docker 网络别名。完整 `useBackendFixture` 添加预设初始化、WebSocket、HTTP server 和 worker。每次调用独立管理容器和数据，不启用容器复用或共享数据库。Ktor test-host 配置和业务断言留在服务端测试；Appium 保留设备转发、浏览器 origin 和会话过期操作；CLI E2E 保留进程驱动和断言。

夹具通过 Docker API 复制预设，不使用宿主 bind mount。测试前镜像构建仍由 Gradle image 任务负责。上下文复制将 shell 脚本规范为 LF，不修改工作树换行。输入相对仓库解析，容器日志忽略 test.env 的宿主 `LOG_PATH`。

阻塞 Testcontainers 启动在 Dispatchers.IO 执行。取消等待当前原生启动调用返回，再通过非挂起 Java IO 清理释放已注册资源。管理作用域在 Dispatchers.IO，Kotlin use 保留原始失败，将 close 错误作为 suppressed exception。清理在删除容器前导出控制台和应用日志归档，包括 Testcontainers 内部就绪失败。收集或移除错误后仍继续清理，并附加到原始失败；没有原始失败时报告清理错误。Appium 和 CLI 服务日志使用原始用例的 `logs`；服务端集成容器日志使用 `logs/containers`，SQL explain 使用 `explain`。夹具清理先导出文件，再写结果元数据，因此任务级报告无需活动容器或设备。完整后端容器使用 Testcontainers 默认单次启动尝试。收集错误记录为 *.error.txt。

通过 Docker 包装器运行 `:dev:testInfrastructure:test :dev:appiumCore:test :cloud:server:test`，传入 `-Pserver.flavor=dev -Pserver.buildType=debug`。基础设施测试包含真实 Redis 就绪失败，验证启动清理后日志仍保留。完整后端通过 `:app:cliE2e:e2eTest :panel:cliE2e:e2eTest` 及已有 Appium 套件验证。

## 已配置的桌面测试环境

测试需要 Docker 兼容后端和 Appium 桌面端点。为目标设置 `APPIUM_DESKTOP_SERVER_URL` 和 `APPIUM_DESKTOP_PLATFORM`，再使用正常宿主命令：

```bash
./gradlew :app:desktopAppium:appiumTest --tests '*test sign up' \
  -Pserver.flavor=dev -Pserver.buildType=debug --console=plain
```

`dev/test-environments/local.json`、`windows-remote.json` 和 `linux-remote.json` 向外部环境执行器描述项目工作区及 JVM 文件或服务角色。连接 profile、VM 生命周期、传输准备和执行器安装属于操作者环境，留在项目之外。远程执行器在编译后暂存宿主构建的 classpath/JDK/native 产物，提供后端可达性、收集日志并清理私有状态。应用和测试方法都不知道使用何种环境提供方。App 与 panel 共用角色配置。Linux 与 Windows 使用同样加引号的 JVM 参数文件约定，包含 Appium 参数数组和工作目录。跨平台执行器需转换宿主路径和 classpath 分隔符，提供已有目标 JDK 及匹配的预构建 native runtime JAR，并返回声明输出。Gradle 测试 JVM 和后端夹具留在宿主，目标桌面只运行应用。Linux 要求图形 Appium 会话、AT-SPI 及解锁的隔离原生 keyring。选择 `APPIUM_DESKTOP_PLATFORM=Linux`，一次执行两个正常 Gradle 任务：`./gradlew :app:desktopAppium:appiumTest :panel:desktopAppium:appiumTest --continue -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`。

`dev/test-environments/desktop-requirements.json` 是桌面运行时需求的可移植权威来源，包含 Java/Appium 版本、普通驱动和扩展、Linux 二进制运行包、匹配原生 classpath 产物及桌面尺寸。两个远程环境文件都引用它。连接 profile、预构建包位置和平台准备由操作者管理。正常宿主 Gradle 测试前，按这些需求准备目标；应用远程运行，测试和后端夹具留在宿主。

特殊运行时依赖可在平台声明 `hooks.host_prepare`，包含字面 `command` argv、相对 `cwd` 和 `timeout_seconds`；或声明 `hooks.guest_setup`，包含相对且自包含的 `script`、`interpreter`、字面 `arguments` 和 `timeout_seconds`。宿主准备可编译或打包依赖。客体初始化可配置 OS、安装预构建产物，但不得编译应用、测试或原生依赖。Guest hook 接收不透明 `TEST_DESKTOP_USER` 和 `TEST_RUNTIME_ASSETS`，不必使用提供方专用用户名或存储路径。Hook 必须前台执行，遇错失败，避免记录凭据。环境初始化器保留私有日志，失败或超时即停止；guest 脚本内容变化后需要重新初始化。这些是普通项目设置脚本，不得引用 AI 插件安装路径、VM 提供方或私有连接 profile。

原生无障碍诊断来自当前 Appium 会话 page source 和报告截图。原生 driver 构造函数接收用例 AppiumTestReport，成功快照和失败捕获都使用其管理的产物目录。快照收集失败单独记录，不替代原始用例失败；收集错误保持独立附件。Tao 不提供 AWT 无障碍树，旧 attach-agent exporter 和 Java bridge 已移除，不把空 AWT 树报告为原生诊断。

在已交互登录的 Windows 会话使用 Appium 3.2.2 和 DesktopDriver 2.8.0。为 `dev/appium-windows-plugin` 准备预构建依赖，通过 Appium 本地插件安装，并用 `--use-plugins desktop-windows` 启用。其 native 依赖固定在 package-lock.json。标准远程窗口枚举和切换仍由 DesktopDriver 提供；扩展负责进程归属检查（包括应用独立线程上的原生对话框）、逻辑像素原生缩放和完整物理像素截图。原生调用后恢复 Appium 线程 DPI 上下文，将调整后窗口保持在 monitor work area，无法容纳的尺寸直接拒绝。经过验证的 inspect/resize 请求注册会话进程后，截图使用 DWM 可见边界，排除不可见缩放边框；拒绝复用 handle，读取像素前验证会话应用在前台。其他 Appium driver 保留标准截图行为。这样可避开 Tao 在 150% 缩放下的逻辑像素 UIA 边界问题，两条路径都不调用宿主 User32/JNA。本地桌面运行也需要该扩展。完整套件将窗口调整到最大 1600×1000 逻辑像素，monitor work area 必须容纳；验收使用 1920×1080、100% 缩放桌面。原生截图和缩放也在 150% 缩放单独验证。

macOS 桌面 E2E 要求具备资质的渲染器，见 [Skiko 调查](#macos-e2e-暂停与-skiko-调查)。保留集成使用 Appium 3、`appium-mac2-driver@4.5.0` 和兼容完整 Xcode（Sonoma 14.8 使用 Xcode 16.2）。选择 Xcode developer directory、接受许可证并完成首次启动。构建 WebDriverAgentMac，在专用已登录测试账户授予 runner 和 Xcode Helper 无障碍权限。通过验收的目标使用 `APPIUM_DESKTOP_PLATFORM=Mac` 与正常 `APPIUM_DESKTOP_SERVER_URL`。执行器将 JDK 预编译 native launcher 打成临时 ad-hoc 签名应用 bundle 供 Launch Services 使用；采用 Mac2 原生 predicate/class 定位，等待点击目标可见、启用且边界稳定。由于 element click 可能成功却未聚焦 Compose 编辑器，应使用原生指针点击；可用时点击不被子元素遮挡的文字容器区域。使用原生按键与窗口调整：JVM 输入先聚焦原生编辑器，通过标准 Appium keys 一次发送完整值，再严格读回验证；逐字符 modifier 事件可能超过现有输入超时。清理时移除 bundle。应用和测试需要预构建 macOS JDK 21 及匹配 native runtime 库。Intel macOS 还需要匹配 SQLite JNI：官方 SQLite bundled 2.7.0 JVM 构件包含 macOS ARM64，没有 x64。应在宿主或指定构建环境，用该版本 JNI 源码（AndroidX 提交 `190a3431c064ca5e90e451b3443714ae684b8127`）、SQLite 3.50.1 和上游编译参数准备 x64 resource JAR，并将 `natives/osx_x64/libsqliteJni.dylib` 加入应用 runtime classpath。UI 测试前验证真实查询，不替代为其他平台二进制，也不在工作 VM 静默编译。Appium endpoint 就绪不能证明 UI 自动化可用；套件前先验证真实 session、source、截图和点击。

Windows/Linux 宿主驱动合格 Mac 目标时，测试辅助方法输出标准加引号 JVM 参数文件。外部环境提供方暂存宿主构建产物，转换声明路径和服务角色，将目标 JDK 预编译 native launcher 打成 ad-hoc 签名 bundle，并将 app path 和 bundle identity 传给 Mac2。本地 Mac 宿主仍直接打包 bundle。提供方必须收集日志、删除私有输入及 bundle，并在传输错误时失败。原始参数文件不是 app bundle，不能直接传给 Mac2。

macOS、Xcode 或 Runner 变化后，检查当前 WebDriverAgentRunner 与 Xcode Helper 的无障碍权限，再重启 test agent。保留的已启用权限项可能指向过时应用身份。套件前验证真实 `macos: adjustWindow` API 和原生窗口尺寸；仅 session 成功或指针拖拽缩放不能证明该 API 合格。先准备测试环境的应用网络权限，再判断被阻塞点击是否为渲染失败。

### 原生桌面窗口、WebView 与视频

两个桌面入口都使用 `nucleusApplication(backend = NucleusBackend.Tao)`，以及围绕 `DecoratedWindow` 的共享 `DesktopWindow` 适配层，包含标题栏和现有应用内容及 provider。适配层通过生命周期管理的 `NavigationEventInput`，将 Escape key-down 绑定到窗口组合的 `NavigationEventDispatcher`，与 Compose AWT window mediator 对齐。这样保留对话框关闭和导航优先级，无需测试专用关闭路径；根导航仍由已有 enabled 状态保护。两者有意关闭 Nucleus 额外单实例锁，让并行隔离进程和重启测试保留原生命周期。Deep link 使用 Nucleus 原生 `onDeepLink` 及原应用回调，避免 macOS AWT 专用 Apple Event handler。普通打包仍使用 Compose Gradle application 配置和 Java 21。

[ComposeNativeWebview 1.0.3](https://github.com/NucleusFramework/ComposeNativeWebview/releases/tag/v1.0.3) 使用 `dev.nucleusframework:composewebview`，共享导入为 `dev.nucleusframework.webview`。[1.0.0 迁移](https://github.com/NucleusFramework/ComposeNativeWebview/releases/tag/v1.0.0) 移除了 Swing/Wry 桌面后端。应用和 Tao runtime 模块固定为稳定 Nucleus 2.5.18。AsciiDoc HTML 预览使用临时或隐私 WebView profile，避免 WebView2 在 Java 可执行文件旁创建默认数据目录，因为该目录可能只读。WebView 仍是嵌入的原生表面：Windows 用 WebView2，macOS 用 WKWebView，Linux 用 WebKitGTK。Compose 对话框使用 Tao 宿主内部 scene layer，保留 native WebView parent 和应用 composition local。旧 `desktop-native-webview` 约定、校验和保护的 Wry binding transform、JNA AppKit dispatcher 和 transform 测试已移除。依赖迁移不能证明历史 macOS 死锁或 Skiko 停顿已修复，仍需下文渲染器验收。

桌面音视频使用 VLC callback surface，而非 `SwingPanel` 或 AWT 播放器组件。音频在按 Play 前保持停止，视频保留自动播放。`DesktopVideoHost` 在注入串行调度器上管理播放状态及原生生命周期，在 worker 调度器上执行 native 构造、命令和释放，并消费复制后的不透明 BGRA 帧合流。VLC 报告就绪后，worker 读取流宽高比和旋转元数据；即使解码缓冲尺寸不同，可见图像仍保持该比例。Compose 适配层在 UI 线程之外构建 raster image，渲染最新帧、控制和加载或失败状态。源变化和组合释放取消管理者，并释放 player/factory。需要兼容系统 VLC 安装；便携安装可通过 JNA 标准 `jna.library.path` 和 VLC `VLC_PLUGIN_PATH` 提供原生库目录。安装运行时或播放媒体，都不授权在客体编译。

运行 `:client:composeCore:jvmTest --tests '*DesktopVideoHostTest' :dev:appiumCore:test`，验证帧归属、播放生命周期和启动 capability。在隔离 Windows/Linux 桌面验证实际解码视频帧、暂停恢复、原生无障碍查询和输入、模态关闭、缩放、AsciiDoc 预览及重启持久化。保留已有业务断言和超时预算。macOS 集成只在下文合格的加速桌面运行。

工作 VM 使用宿主构建产物运行应用和测试。客体编译，包括 native 依赖安装 hook，必须有用户明确要求；VM/Appium 设置或测试请求本身不授权编译。不要增加客体编译回退或为构建扩大 VM 资源。传输失败让测试失败，不改为在宿主启动应用。

## macOS E2E 暂停与 Skiko 调查

状态：2026-10-25，加速 Intel macOS 15.8.1 渲染器通过验收后，已移除硬编码的 macOS 桌面 E2E 暂停。上游 Skiko 0.150.1 与未修改的 Nucleus Tao 2.5.18、3.0.0-dev-202610230652，可见地渲染账户、登录、注册和私钥编辑器导航、合成文字输入，以及从 800 x 600 到 900 x 700 的窗口缩放，与原生无障碍状态一致。未使用已停用的 Skiko 类替换或帧率上限。2026-10-25 一次全新完整验收通过全部 18 个原始桌面 E2E 用例（15 个 app、3 个 panel），零失败、零跳过，执行器成功退出。原断言和超时不变；原生 WKWebView HTML 与窗口缩放通过真实截图核对。本结果与历史软件上传器实验分开。

只有验证上游 Skiko 修复能解决观察到的软件渲染停顿，或环境具有让 Skiko 使用硬件渲染器的有效 GPU 加速后，才运行 macOS 桌面 E2E。WHPX 加速虚拟 CPU，本身不提供客体 GPU 加速。显示设备、可见桌面或选择 OpenGL 选项，都不足以证明硬件渲染可用。

### 观察到的失败与证据

受影响环境为 Intel macOS 14.8、JDK 21、Skiko 0.150.1、Appium Mac2 4.5.0 和 XCTest。VM 有四个 vCPU、4 GiB RAM，使用 WHPX，没有可用 Metal adapter。Skiko 回退到 CPU 渲染。这些发现不能证明实体 Mac 或其他架构具有相同延迟。

重复栈显示 AWT 事件线程耗时位于 `ComponentColorModel.getRGBComponent`、`OpaqueCopyAnyToArgb` 和 `OGLGeneralBlit`，调用来自 `SoftwareContextHandler.flush`。无障碍查询中，AppKit 通过 `CAccessibility.invokeAndWait` 等待 AWT，因此原生查询和实体按键输入可能排在软件帧上传之后。诊断查询样本约耗时 4–28 秒，不是受控跨平台基准测试。

[上游 0.150.1 上传器](https://github.com/JetBrains/skiko/blob/v0.150.1/skiko/src/awtMain/kotlin/org/jetbrains/skiko/context/SoftwareContextHandler.kt) 将完整 native bitmap 读到 byte array，创建 byte raster 和使用 `ComponentColorModel` 的 `BufferedImage`，再通过 Java2D 绘制。原生 bitmap 为 premultiplied，image 构造函数却将 raster 描述为非 premultiplied。观察到的转换路径表明应调查图像表示和 alpha 处理，而非延长测试超时。

### 已移除的实验补丁

实验通过缓存 Gradle artifact transform，用宿主编译类替换固定的软件上传器。准确审查的 `skiko-awt:0.150.1` JAR SHA-256 为 `bb610ada28509ebee3a623c0469aff3665689fbec73d118055f3dbf5a37211f5`。Transform 拒绝其他字节；它不是上游发布版本或受支持扩展 API。

替换实现保留 native premultiplied BGRA bitmap，将实时像素直接复制到可复用的 `BufferedImage.TYPE_INT_ARGB_PRE` 整数缓冲。显式 little-endian 解释保留 BGRA 到 ARGB 通道顺序，避免中间帧大小 byte 分配和 byte-component 颜色转换。完全不透明帧用 `AlphaComposite.Src`，半透明帧保留 source-over 混合。实验保留 macOS 透明窗口清除，并释放 native bitmap 和 graphics 资源。

另一实验在 Skiko 已有 `FrameLimiter` 中插入可选周期上限。已移除 JVM 属性为 `desktop.software.maxFps`：未设置或零时保留上游周期，1–240 选择最小周期，但不加速本已更慢的上游调度。测试环境选 10 FPS。这是环境适配，不是通用性能修复：它降低动画平滑度，且 hook 修改共享 limiter，没有在结构上限制于软件渲染。因此不能将其作为所谓 software-only 设置恢复。

运行时类替换、Gradle 约定、共享 limiter hook、替换源码、补丁专用测试和 Skiko 专用测试依赖均已移除。两个桌面应用重新解析上游 Skiko 构件。操作者归档中的实验 transformed JAR 仅是历史诊断，不得暂存用于新验收。Appium 默认 capability 和 launcher 选项也应移除过时 JVM 属性，因为项目中已无实现。

### 历史验证及限制

三个补丁专用宿主测试覆盖通道顺序、premultiplication 和合成、先前帧内容替换，以及 limiter 默认、配置和较慢周期行为。另一个原生调度探针和 WebView 修复处理独立的 AppKit/EDT 相互等待。文字点击定位和异步退出同步也分别修正。

2026-10-21，组合实验环境完成一次 macOS 桌面 E2E：15 个 app 和 3 个 panel 测试，两个 JUnit 套件及完整执行器均成功退出。保留原 10 分钟测试超时、15 秒 UI 等待和原断言。1,066 次元素查询中，耗时中位数 69 ms、P95 338 ms、最大 826 ms。实体按键命令批次中位数约 6.65 秒、最大 11.60 秒，批次内容和长度不同。仍有一次原生点击耗时 18.54 秒。

这些结果验证的是组合实验环境，不是独立 Skiko 补丁。它们不能认证回滚后的未修改运行时，也不能认证新验收的硬件环境。历史通过不能当作当前 macOS 验收。

提出上游 Skiko 变更前，应通过合成复现隔离上传器优化，分别测量帧上传 CPU 时间和分配，验证不透明与半透明渲染、部分 alpha、重复帧、缩放、HiDPI、透明窗口、支持的 CPU 架构，以及全部桌面软件渲染后端。额外的全帧 alpha 扫描也需性能评估。三个单元测试或一次 macOS E2E 不能证明广泛渲染器兼容性。

WebView AppKit run-loop 适配层是独立依赖修复，与 Skiko 分开，后来在前述 ComposeNativeWebview/Tao 迁移中移除。尚未为这些发现提交上游 Issue 或 PR。迁移本身不能使 macOS 渲染器获得验收资格。

### 资格验收与回归流程

1. 记录已修复的上游 Skiko 版本及相关变更，或提供 VM 实际使用硬件 Metal/OpenGL、而非软件实现的证据。不恢复本地类替换或帧率限制，核对可见导航、文字编辑、对话框和缩放与无障碍状态一致。仅库加载或首帧不足。
2. 平台可用性独立于提供方机制。维护外部渲染器资格，合格运行时变化时更新状态，不引入无条件绕过标志。
3. 在宿主或指定构建机编译应用、测试和必要 native 依赖，再暂存包含预期上游 Skiko 构件的全新 classpath。私有连接 profile 和环境编排留在项目之外。未经明确授权，不回退 TCG，也不在工作 VM 编译。
4. 先验证真实 Appium 会话、查询、输入与值读回、原生 WebView 生命周期和截图，再从干净报告根运行全部 18 个桌面 E2E，保留已有预算和断言。新验收结果与历史实验报告分开记录。

## 带类型的操作结果

与业务无关的 `:result` KMP 模块将 `CustomResult<T, E>` 定义为 `Success(value)` 或 `Failure(error)`。成功的可空值与失败不同。HTTP 响应继续将成功 null 映射为 404、Unit 为 200，其他值映射到已有响应模型；CustomResult 不序列化到 HTTP 或 RPC payload。

操作管理者声明密封错误接口，分别用于共享加密或内容验证、后端数据库／搜索／对象存储、客户端请求或凭据，以及各 cloud 业务服务。签名必须写明场景错误类型，不擦除为 Throwable、Any 或 FailureDetails。FailureDetails 为日志及 Paging、加载状态、HTTP 响应写入器等终端使用者暴露诊断 cause。

场景之间通过显式 `mapError(ScenarioError::fromError)` 转换。异常分类使用 `ScenarioError.from`；带类型的错误转换保留认证、权限、输入和约束类别，与诊断异常类相互独立。数据库认证拒绝经服务和 HTTP 转换后仍保持认证类别。Reviewer 构造在 factory 边界捕获提供方初始化失败，使 worker 可重试初始化，而不取消同级任务。纯映射或组合不捕获异常。`captureFailure` 必须接收边界错误 mapper，捕获普通异常，传播协程取消和致命错误。浏览器加密在平台边界规范化 JavaScript 异常。基于异常的互操作使用 `getOrThrow`，新业务决策应检查带类型的变体。旧 shared Kotlin Result 工具已移除，不保留兼容别名。

`cloud/runtime` 管理 CustomResult endpoint 绑定，复用 endpoint4k 参数解码与 HTTP 方法绑定。服务端路由应导入 `com.storyteller_f.a.cloud.server.common.invoke`，不使用依赖中基于 Kotlin Result 的调用。错误容器和平台密钥编码不改变外部 API 约定。

结果、加密边界和请求错误测试覆盖 null 成功、错误转换、诊断 cause、普通失败、取消，以及业务 401 与会话认证失败的区别。已有宿主和集成测试直接使用 CustomResult，业务断言仍适用。

## PDF 字体来源与可变字体实例化

`cloud/openpdf` 发现已安装字体文件，不使用 AWT 枚举或私有 JDK 字体 API。生产 PDF 构件不含字体二进制。CFF2 回归使用固定版本的原始测试专用字体资源。首次 PDF 请求前安装字体，安装变化后重启进程。发现过程只读取 SFNT/TTC 目录、Unicode family name 和 OS/2 metrics，不读取轮廓或 variation 内容。元数据布局遵循 [OpenType 规范](https://learn.microsoft.com/en-us/typography/opentype/spec/otff)。所选文件内容惰性加载；未使用的 variable fallback 不会被完整读取或实例化。

发现过程递归搜索以下标准位置的 TTF/OTF/TTC/OTC：

- Windows：`%WINDIR%/Fonts`、`%LOCALAPPDATA%/Microsoft/Windows/Fonts`。
- Linux：`/usr/share/fonts`、`/usr/local/share/fonts`、`~/.fonts` 和 `$XDG_DATA_HOME/fonts`，默认 `~/.local/share/fonts`。仅 fontconfig 配置的自定义路径需使用下方显式目录覆盖。
- macOS：`/System/Library/Fonts`、`/Library/Fonts`、`~/Library/Fonts`。

正文字体优先顺序为 Noto Serif SC、Noto Serif CJK SC、Source Han Serif SC。等宽字体顺序为 Noto Sans Mono、DejaVu Sans Mono、Liberation Mono、Consolas、Menlo。各受支持字体家族的正体静态 Regular（400）和 Bold（700）分别优先，可变源只补缺失字重。应安装覆盖文档语言的正文字体；代码中等宽字体缺字的文本段，通过显式的比例正文 fallback 保留。未知代码字形失败，不静默消失。粗体使用真实轮廓，斜体使用 PDF oblique。缺少必需字面时报安装错误，不隐式使用 PDF 字体。保留集合 face index。

Linux 可安装 `font-noto-all`、`font-noto-cjk`（Alpine），或 `fonts-noto-cjk`、`fonts-dejavu-core`（Ubuntu/Debian）。Docker runtime 安装系统字体包，不从应用资源复制字体。Alpine `font-noto-all` 通过 `font-noto` 包含静态 Noto Sans Mono Regular/Bold，`font-noto-cjk` 单独提供。固定的 Temurin Alpine 基础镜像通过 `ttf-dejavu` 已包含 DejaVu，因此 runtime stage 不重复请求 `font-dejavu`。发现仍优先 Noto Sans Mono。Windows/macOS 可通过 OS 字体管理器安装 Noto Serif SC，代码使用已安装 Consolas/Menlo。

可选 JVM 属性 `pdf.font.directory` 必须在首次 PDF 请求前设置，会替换两类字体的发现。使用 `serif-regular.ttf`、`serif-bold.ttf`、`mono-regular.ttf`、`mono-bold.ttf`；可选 `serif-variable.ttf`、`mono-variable.ttf` 通过 `wght` 补充缺失静态字重。也接受同名 `.otf`；同一槽同时存在两扩展时优先 `.ttf`。静态槽必须包含静态字体，选中的静态字面无效时显式失败。备用可变文件只在所需静态字重缺失时读取；未使用且不可读的备份不能阻塞静态字体（P2 回归）。不自动下载、复制或注册 fallback 文件。

每个 PDF 独立管理不缓存的 OpenPDF BaseFont 和全新 Font style。只缓存不可变实例字节，最多 16 项，key 包含源 SHA-256、集合 face index、排序后的轴（包括默认值）和引擎版本。实例化失败及抛出的取消异常不发布。HarfBuzz 调用是同步的，不能靠取消协程 Job 安全中断。原生资源管理持续到调用返回且清理完成；等待协程仍收到取消。交付取消时，已完成且验证的不可变实例可能已经入缓存。

独立静态 TrueType/OpenType 无需原生实例化库。可变 TrueType `glyf` 和静态 TrueType/CFF 集合提取需要匹配的公共 HarfBuzz core/subset **8.3.1 或更新版本**及 JNA。CFF2 可变字体要求 **HarfBuzz 13.0.0+**：固定全部轴，再用公开 `HB_SUBSET_FLAGS_DOWNGRADE_CFF2` 生成兼容 OpenPDF 的静态 CFF1 轮廓。适配层保留已有 subset flags，拒绝仍含 CFF2 或 variation 表的输出。较旧库在分配 CFF2 handle 前显式失败；TrueType 和静态 CFF 最低仍为 8.3.1。不支持的轴或范围显式失败，未请求轴固定为默认值。适配层将源内存复制到 DUPLICATE blob，复制结果字节后按逆序释放全部 native handle。实例化输出不能保留 `fvar`/`gvar`。

- Windows：使用[官方 HarfBuzz 发布包](https://github.com/harfbuzz/harfbuzz/releases)，将 `libharfbuzz-0.dll`、`libharfbuzz-subset-0.dll` 和依赖放在一起。目录加入 `PATH`；Gradle 测试设置 `JAVA_TOOL_OPTIONS=-Djna.library.path=C:/tools/harfbuzz`。
- Linux：安装匹配 `harfbuzz-subset`（Alpine）、`harfbuzz`（Fedora，含 core/subset）或 `libharfbuzz-subset0`（Debian/Ubuntu），并核对版本。Alpine runtime target 要求 `harfbuzz-subset>=13.0.0`；Fedora worker 从签名仓库安装 `harfbuzz`，检查主版本至少 13。
- macOS：安装匹配 HarfBuzz core/subset，并将目录暴露给 JNA。全部平台均支持绝对覆盖 `pdf.harfbuzz.core.library` 和 `pdf.harfbuzz.library`。

分发包附带 HarfBuzz 的 [Old MIT 许可证](https://github.com/harfbuzz/harfbuzz/blob/14.6.0/COPYING)。字体安装和许可仍由系统软件包或字体安装负责。生产实例化不使用 Python、子进程、JDK/Skiko 私有库或字体注册。

已安装独立可变 TrueType Noto Serif SC/CJK SC 或 Source Han Serif SC、等宽 Regular/Bold 和公共原生库后，运行 `./gradlew :cloud:openpdf:test -Pserver.flavor=dev -Pserver.buildType=debug --console=plain`。CFF2 回归读取 `cloud/openpdf/src/test/resources/fonts` 内原始 Noto Serif SC Variable 和 Source Serif 4 Variable Roman/Italic OTF。其上游修订、路径和 SHA-256 记录在 `fonts/fixtures.properties`；使用前验证每个内置字体。对应上游 OFL 许可证放在各家族旁。这些文件只属于测试 classpath，不运行时下载、不注册系统字体，也不进入生产包。
可选诊断覆盖 `pdf.test.cff2.font`、`pdf.test.cff2.multiaxis.font`（Roman）和 `pdf.test.cff2.collection.font`（Italic）仍可通过 `JAVA_TOOL_OPTIONS` 设置；显式覆盖无效时失败，不回退内置字体。两个 Source Serif 输入都有范围匹配的 `wght` 和 `opsz` 轴。测试在内存构造对齐集合，将每个提取字面与独立源比较，验证 face-index 缓存隔离，并比较随 optical size 变化的 advance。独立固定 opsz 用例以 NORMAL PDF style，在 400/700 字重渲染相同文字，检查真实粗体轮廓更厚，并验证输出 OS/2 weight 值。
验证回归守卫时，在隔离变异中固定全部默认值，只覆盖排序第一轴 opsz。Optical-size 用例必须仍通过，而固定 opsz 字重用例应失败，因为两次渲染都用了默认字重。在可丢弃 worktree 运行变异，最终测试、构建和静态检查前恢复正确源码。取消测试通过公共 table callback 暂停真实 `hb_subset_or_fail`，取消所属 Job，再释放回调，验证转换完成、handle 逆序释放及交付被取消。同步 C 回调使用隔离的阻塞协程桥接；测试期限和 native 工作运行于真实调度器，失败清理始终释放 gate 并 join worker。
可选外部验证覆盖留在源码和资源树之外。保留内置原件和许可证，在内存生成静态集成输入，不提交派生字体。合成元数据夹具覆盖发现、集合 index、无效及无关字体，不附字体程序。测试还覆盖静态优先、P2 延迟 IO、混合家族、轴、native 失败、取消、逆序清理、并发缓存发布、渲染、双语提取、嵌入、等宽 advance、CFF2 降级和版本门槛，以及逐文档归属。诊断 PDF/PNG 位于 `cloud/openpdf/build/test-output/fonts`。显式更新已有服务端快照参考前，先审查预期视觉差异，不自动接受比较结果。

## PDF 快照布局与内容覆盖

OpenPDF 快照使用 A4、48 point 左右边距、双语快照标题、来源面板（发布者与发布时间、捕获者与捕获时间）、带间距正文与各级标题，以及标识内容页的页脚。顶部时间戳用空格分隔日期和时间，例如 `2026-01-02 12:30:45 UTC+00:00`，省略小数秒并明确标注 UTC。`PdfGenerationSpec` 从主题创建时间和 `shared.utils.now()` 接收 UTC 日历值；宿主默认时区不得改变显示时钟值。签名页为发布时间使用相同可读 UTC formatter，保留原时间含义，不替换为签名时钟。签名流程仍插入独立签名页；页脚编号指内容页，不是签名后的物理位置。
`SnapshotLayoutTest` 检查来源、多页内容保留、嵌入字体、页码和准确代码文字。`SignedSnapshotTest` 用生成测试 key 自动验证完整文件 ByteRange 覆盖和密码学签名。负例在内存修改已签元数据字节，保持 PDF 可解析及完整 ByteRange，要求签名验证失败。这验证完整性，不验证证书信任链或 TSA 信任。布局测试通过独立管理的内存副本读取实际 signature widget appearance，断言 UTC 显示，再渲染原 PDF。共享夹具始终在 finally 删除生成 PKCS#12。阅读样本输出到 `cloud/openpdf/build/test-output/layout`。来源回归同时检查 UTC 标签和准确时钟值。在 `JAVA_TOOL_OPTIONS` 加入 `-Duser.timezone=Pacific/Honolulu`，连同 native/font 选项运行 PDF 模块测试，验证非 UTC JVM 默认时区不能改变 UTC 显示。

服务只导出 `TopicContent.Plain`。`Encrypted`、`DecryptFailed`、`Extracted` 和 `Nil` 被快照边界拒绝，不解密或转换。PDF 接收单主题 Markdown、作者和捕获者及时间戳，不包含回复、reaction、主题标识或其他主题元数据。下载 `fileInfos` 不会自动嵌入附件或媒体预览。

当前 OpenPDF visitor 有以下限制，已通过合成导出样本检查：

| 内容 | 当前结果 |
| --- | --- |
| 正文、基础强调、行内代码、标题 | 能渲染；嵌套样式可能残留 Markdown 标记，标题保留行内标记。 |
| 围栏或缩进代码 | 输出源文字；重叠高亮不再重复字符，未高亮尾部保留。 |
| Markdown 图片，包括独立图片 | 常见段落内图片被忽略，即使 attachment map 含图片；alt 文本也消失。 |
| 行内数学 | 省略。 |
| Mermaid、AsciiDoc 和围栏数学 | 只显示源码，不渲染图表、文档或公式。 |
| HTML block | 省略。 |
| 表格 | 正文单元能渲染，但表头可能消失；不解释行内格式与对齐。 |
| 列表和任务 | 源标记可能重复；不忠实保留原有有序起点、嵌套结构及勾选状态。 |
| 链接 | 行内链接创建 PDF annotation；引用链接保留原始 Markdown，尖括号自动链接可能消失。 |
| 硬换行与实体 | 换行可能合并，实体可能仍是编码形式。 |
| Emoji 和正文未覆盖字形 | 可能消失；仅安装字体包不会添加正文 fallback 或彩色 emoji shaping。显式字体之外的代码字形则失败。 |

这些是渲染器限制，与静态或可变字体转换无关。需要保留富内容时检查导出 PDF。生成成功不能证明与主题 UI 内容一致，也不能修改断言来接受内容静默缺失。

## Wasm 媒体组合实验

实验性 Wasm 媒体路径使用 `client/composeCore` 中独立于 UI 的 `wasm-media-host` npm 包。专用 Worker 管理 Mediabunny input、网络读取、demux、decoder iterator 和 offscreen canvas。主线程 Host 管理播放协调、不可变快照、Web Audio graph 及媒体时钟。Compose 适配层只管理展示状态、Skia bitmap 和接管的 texture Image，创建延迟启动的 Host，在组合提交后的 DisposableEffect 中启动 Worker。顶层 `@JsModule` factory 导入 npm default export；必须同时保留验证使用的具名 JavaScript factory。实验不改变 Android 和桌面 actual。

Mediabunny 固定为 **1.62.1**，采用 MPL-2.0，来自[官方包](https://github.com/Vanilagy/mediabunny/blob/main/package.json)。`client/composeCore/wasm-media-host/NOTICE` 标明未修改包及准确源码归档；分发 bundle 时保留上游声明及源码可获得性。Wasm Yarn lock 记录包完整性。Codec 可用性通过实际每条输入 track 的 `canDecode()` 检查；[容器支持不保证浏览器 codec 支持](https://mediabunny.dev/guide/supported-formats-and-codecs)。本实现要求 HTTPS 或 localhost、module Worker、压缩 track 所需 WebCodecs、声音所需 Web Audio，以及跨源媒体读取的 CORS 权限。使用有限、可 seek 媒体；直播流和与浏览器原生控件完全一致不在实验范围内。不声称选择硬件解码器。

### GPU texture 路径与本地 Skiko 边界

已发布 Skiko 0.150.2 API 不暴露 renderer context 或 texture table。较新的 [0.156.0 texture 注册](https://github.com/JetBrains/skiko/blob/v0.156.0/skiko/src/webMain/kotlin/org/jetbrains/skiko/GLTexture.web.kt)及 `Canvas.recordingContext` API 提供部分边界，但新 `RenderNodeContext(Boolean, Boolean)` 构造函数与 Compose 1.12.2 已编译的 `RenderNodeContext(Boolean)` 调用不兼容。只升级 Kotlin 构件还会让 Compose native Wasm runtime 留在旧版，因此编译成功不足以验证升级。

`dev/skikoMediaRuntime` 是仅 Wasm 使用的 included build，替换 `org.jetbrains.skiko:skiko-wasm-js`。它重编译准确且经过 SHA256 检查的 0.150.2 源码归档，保留已发布的 **0.150.2 原生 Skia/Emscripten runtime**。不替换 JVM/Android 构件，也不重建原生 Skia。保留上游 Apache-2.0、源码来源及上游兼容 Kotlin 2.4 的 import-generator（含项目头）。避免配置期 IO，源码验证和提取属于任务输入及 action。`patches` 中三个文件是源码覆盖，不是加载到已发布库旁的替换类：

- `WebGLMediaInterop.kt` 注册 renderer 已有 context，暴露有作用域、借用的 context token 及 `pushTexture`/`unregisterTexture`。Token 将自身 context 设为 current，再恢复原 context。注册同时更新 Emscripten texture table 和 texture 对象 `name`，使原生 GL 绑定查询能往返获取数字 handle；注销时清除两者。绝不关闭借用的 `DirectContext`、创建第二个、在回调外保留它，或在回调内挂起。
- `CanvasRenderer.kt` 注册和释放 token，释放时停止排队 redraw，释放 surface/context，缩放时保持同一 context。
- `SkiaLayer.web.kt` 对同一 canvas 缩放时复用 renderer，detach 时释放，保持导入 Image 与原 renderer 关联。

Included build 是仓库自身的临时集成。`patches`、import-generator 源码、`LICENSE` 和 `NOTICE` 一起维护；全新检出通过根 Gradle Wrapper 构建，无需单独发布 fork。生成源码提取和编译输出留在被忽略 build 目录。实际 texture-name 修复已在 `patches/WebGLMediaInterop.kt`。

[`upstream-patches/SKIKO-1208.patch`](dev/skikoMediaRuntime/upstream-patches/SKIKO-1208.patch) 保留针对上游 v0.156.1 的最小修复，供审查或向前移植，base-file SHA256 和来源在 `NOTICE`。`prepareSources` 不应用它；当前 0.150.2 使用活动源码覆盖。在上游应用前，先对干净 v0.156.1 检出执行 `git apply --check`。

只有在真实 Compose recording 路径验证兼容的 Compose/Skiko Kotlin 与 native-runtime 组合、上游修复 texture-name 映射，并迁移本地 interop 调用点后，才移除临时 included build。`settings.gradle.kts` 替换和 included-build 源码一起删除，再重跑 texture 归属、GL 状态、缩放释放和 release 浏览器检查。单独修复 SKIKO-1208 不会替代其余 renderer 生命周期和 context 访问集成。

Skiko 0.156.1 暴露 `Canvas.recordingContext`，但是否可用取决于 canvas：GPU surface canvas 有借用 context，PictureRecorder 和 RenderNode recording canvas 可能返回 null。同步 SkiaLayer render 回调可以结合相同 DOM canvas 已有 `getContext("webgl2")` 使用，但不能据此证明 Compose 绘制回调有同样访问能力。直接 layer interop 验证应与 Compose recording、ABI 兼容验证分开。

0.156.1 的 `GLTexture.web.kt`（[SKIKO-1208](https://youtrack.jetbrains.com/issue/SKIKO-1208)）将外部 texture 注册进 `GL.textures`，却未赋值 Emscripten `texture.name`。因此即使 JavaScript binding 和 table entry 正确，原生 `glGetIntegerv(GL_TEXTURE_BINDING_2D)` 仍可能返回零。本地 0.150.2 overlay 已赋值 name。评估升级时必须保留双向映射；图像渲染成功不能验证原生 GL 状态查询。注册修复还需审查重复注册与注销的归属，不能把 table entry 当成完整约定。

默认仍为 `rgba-copy`。通过实验查询参数 `mediaPath=gpu-texture` 选择 `gpu-texture`。Worker 使用 `VideoSampleSink`，克隆并 transfer 解码器支持的 `VideoFrame`，关闭自身 sample。适配层通过 `texImage2D(VideoFrame)` 将帧导入**当前 Compose renderer** 的 WebGL2 context，`pushTexture` 注册 handle，`Image.adoptTextureFrom` 将管理责任转给 Skia。`drawImageRect` 直接将 Image 记录进 Compose scene，不使用 `Image.toComposeImageBitmap` 或 DOM 媒体／canvas overlay。旋转和翻转元数据通过 Skia transform 应用。浏览器内部仍可能转换或复制；本路径只声称移除显式 RGBA readback 和 JavaScript 到 Skiko 像素复制，**不**声称零复制解码或保证硬件解码。实验针对不透明 8-bit SDR 媒体，不承诺 alpha video、HDR 或更广色彩管理一致性。

受保护 GPU 导入创建全新 texture，恢复其修改的全部 GL 状态，包括 unpack buffer、texture unit/binding 和 pixel-store 值，因此已有 Skia 状态缓存仍有效。默认 `gpuImport=restored` 使用已经 current 的 renderer token，不重复激活 context、不逐帧 flush/submit，也不完全失效 GL cache。`gpuImport=legacy` 在同构建保留这些旧操作，供消融比较。本优化不允许外部写入已接管 texture，也不移除复用资源协议所需同步。每帧新建 texture，避免覆盖 recorded picture 仍引用的 texture。成功导入归 Skia 管理；失败则注销并删除外部 texture。被取代、迟到、丢弃和释放的 `VideoFrame` 在所属边界关闭。Context loss 是终止性媒体失败：取消解码和音频、关闭待处理帧，不静默切换渲染路径。根 renderer 自动恢复不在实验范围内。

已实现的默认路径明确为 8-bit sRGB `rgba-copy`：解码 sample 绘制到 Worker `OffscreenCanvas`，`getImageData` 读取 RGBA，transferable buffer 到达适配层，`installPixelsFromArrayBuffer` 复制进 Skiko linear memory，不可变 bitmap 通过 `asComposeImageBitmap` 不转换地封装，再由普通 Compose `Image` 绘制。替换或释放时关闭旧 bitmap。避免 `Image.toComposeImageBitmap`，它会再构造 Bitmap。短暂像素交接不可取消地完成，因为 Skiko 在等待 runtime Promise 前分配；适配层随后检查管理者，立即关闭取消后才生成的 bitmap。Offscreen canvas 从不插入 DOM。字幕、裁剪、按钮和对话框属于同一 Compose scene。30fps 时，仅原始像素负载就达到 720p 的 110.6 MB/s、1080p 的 248.8 MB/s，尚未计入 Skiko 复制和 GPU 上传。这是字节量计算，不是实测硬件保证。

### 播放资源管理

媒体时间戳单位为秒。音频 sample 经一个 gain node 转为已调度 `AudioBufferSourceNode`。AudioContext render time 驱动音频调度和饥饿检查。视频、显示进度和结束检测跟随 `getOutputTimestamp()` 映射到 monotonic performance clock 的值，受 render progress 上限约束，并在同一播放 generation 内保持单调。有效 output timestamp 出现前，使用报告的 base/output latency 估计，缺失 latency 按零计。静音视频直接使用 monotonic performance clock。AudioContext 创建和恢复在用户操作内启动，使自动播放限制保持可见。调度初始提前 80ms。Host 将提前调度声音限制在约 500ms、最多 64 node，保留一帧待处理视频，以及 Mediabunny 1.62.1 的四 sample 解码预取。剩余已渲染声音低于 40ms 时暂停 render clock；已调度达到 300ms、受限 node 队列填满或 audio iterator 结束后恢复。缓冲期间，已排队输出和对应视频可继续消耗，但受已暂停 render progress 限制。迟到视频帧根据 output clock 丢弃。指标描述 Host 分派时间戳和 Worker readback，不代表扬声器或显示延迟，也不保证硬件加速。

暂停和 seek 停止并断开全部已调度 audio node，在请求位置替换 decoder Worker。Generation 检查拒绝迟到元数据、sample 和 resume 完成。播放意图可跨越被取代 seek 保留，但 pause、end、error 和移除会清除。替换源创建新 Host；移除时终止 Worker、清除待读取和计时器、关闭 AudioContext。Worker termination 负责取消网络读取和 native decoder 资源；audio Worker 内复制的 sample 也显式关闭。适配层在替换和移除时释放 Skia bitmap。隐藏页面暂停播放，返回后必须显式点击 Play。现有 media-item 约定无 loop 选项，结束后可重播。

### 共享播放器展示

Android、桌面和 Wasm 通过 `MediaPlayerSurface` 渲染传输控制。视觉布局、时间格式、seek、音量菜单和展开行为保留在通用组件，适配层只转换引擎状态和命令。展开保留调用方管理的播放引擎。Android 将 playlist 和画中画入口加入共享 options menu，不在播放器下另设工具栏。桌面进度由 `DesktopVideoHost` 在 worker 采样；共享 UI 边界的媒体时间为秒，音量范围为 0..1。桌面 Retry 替换失败 native player 并恢复播放；初始音频预览保持暂停。Android 默认 playlist 条目使用媒体标题或文件名，不用源 URL。

Android 原生播放回归位于 `:app:androidAppium:appiumTest`，通过 `*video expansion*`、`*video playlist*`、`*picture in picture*`、`*audio seeks*` 和 `*audio volume restores*` 过滤，使用内置真实媒体夹具、解码 RGB 帧检查和原生进度、音量状态。保留普通设备租约、Appium/后端前提及 flavor/build-type 属性。展开的 Android SurfaceView 可能不在无障碍树中；帧检查读取已验证对话框截图。

使用标准 flavor/build-type 属性运行 `:client:composeCore:jvmTest` 聚焦用例 `*MediaTimeTest`、`*MediaVolumeControlTest`、`*MediaPlayerSurfaceTest` 和 `*DesktopVideoHostTest`。Surface 测试使用合成媒体状态，布局截图保存到 `client/composeCore/build/test-results/jvmTest/media-ui`，不需要账户或播放器。

真实 VLC 播放单独执行 `:client:composeCore:nativeMediaTest`。此显式集成任务要求可发现 VLC native 库和可用桌面图形 runtime，排除在普通 JVM 测试外；runtime 不可用时失败。它检查紧凑视频控制、seek 和 pause、展开连续性、音频结束与重播、音量恢复及 HTTP 503 后显式音视频恢复。任务本地提供内置合成夹具，截图保存到 `client/composeCore/build/test-results/nativeMediaTest/media`。使用标准 flavor/build-type 属性，与其他桌面 UI 任务顺序执行。

Windows Tao 在提取内置 ANGLE runtime 前，从系统库路径加载 EGL/GLES。应用进程 PATH 不应包含无关工具链 EGL/GLES DLL 目录，系统 EGL 与内置 GLES 混用可能在 native window attach 时崩溃。Windows Appium JVM 清除开发 JNI 搜索路径（`-Djava.library.path=`），使用 classpath 打包 native 资源；VLC 保留独立 JNA 发现配置。改变渲染设置前，用 JVM native-library 加载诊断核对两种库来源。桌面窗口跟踪排除 Tao message-dispatch window。原生 UIA 可能将内联图片合并到内容控件，因此视频验收在组合截图中检查合成 RGB 图案。精确 Windows 控件使用 native name/control-type selector；XPath 会物化整棵原生树，内联视频更新期间可能耗尽断言预算。超时预算不变。

Windows 使用标准 flavor/build-type 属性运行 `:app:desktopAppium:windowsMediaTest`，验证完整应用视频暂停和展开连续性、音视频 seek、结束重播、音量与静音恢复及显式 HTTP 503 恢复。Retry 夹具通过后端 filesystem public URL 宣告自己管理的 loopback HTTP endpoint；只有 UI 显示 Retry 后才恢复成功媒体响应。显式套件共享桌面串行服务，每次明确请求都执行。视频音量验收使用 `sample-with-audio.mp4`，video track 来自 `sample.mp4`，AAC audio 由 `sample.wav` 编码。原静音视频夹具不变；静音片段没有可调 VLC audio output。附件和列表导航运行四个 `*test audio fullscreen*` / `*test video fullscreen*` 用例。

隔离实验显式启用测试专用 semantics bridge。浏览器按名称定位传输控件，在已定位 slider 边界内 seek，因此布局变化无需改固定坐标。生产播放器页面除非测试模式显式启用，否则保持 bridge 关闭。

`player-expansion` 浏览器套件在 Chrome、Edge、Firefox 上，对 RGBA 与 GPU 两条路径检查展开和折叠保留 engine、worker 和 audio context。使用已有浏览器测试前提，通过 `node client/composeCore/wasm-media-host/run-browser-tests.mjs player-expansion` 运行。

### 复现隔离实验

需要 JDK 21、Node 22+、Python 3.10+，以及支持 VP9、H.264、Opus 和 AAC 的 FFmpeg。无需服务端、账户、VM 或部署。从仓库根构建夹具应用：

```bash
./gradlew :dev:wasmMediaExperiment:wasmJsBrowserDevelopmentExecutableDistribution -Pserver.flavor=dev -Pserver.buildType=dev --console=plain
```

修改 npm 依赖后，只用 `./gradlew kotlinWasmUpgradeYarnLock -Pserver.flavor=dev -Pserver.buildType=dev --console=plain` 重建 Wasm lock，普通 JS lock 独立。根 Wasm npm-install 任务跟踪本地 Host 源文件，需安装时传入 Yarn `--force`，否则复制的 `file:` 依赖可能在 Kotlin 编译成功后仍保留旧实现。未改变的构建仍复用该任务及配置缓存。

在被忽略 build 目录安装可丢弃浏览器验证工具，再生成合成媒体并运行约定测试：

```bash
npm install --prefix build/wasm-media-research/browser --no-package-lock --ignore-scripts playwright@1.64.1 esbuild@0.28.2 selenium-webdriver@4.51.0 pngjs@7.0.0
npm --prefix client/composeCore/wasm-media-host install --no-package-lock --ignore-scripts
node client/composeCore/wasm-media-host/generate-fixtures.mjs
node --test client/composeCore/wasm-media-host/*.test.mjs
npm --prefix client/composeCore/wasm-media-host run test:browser
node client/composeCore/wasm-media-host/benchmark-browser.mjs
```

Chromium 检查使用已安装 Windows Chrome、Edge 二进制和隔离 headless profile。`MEDIA_CHROME_BINARY`、`MEDIA_EDGE_BINARY` 可覆盖路径。Firefox 使用已安装 Firefox 和已有 geckodriver；将 `MEDIA_GECKODRIVER` 设为其可执行文件，再运行 `npm --prefix client/composeCore/wasm-media-host run test:browser -- firefox compose-firefox`。Compose 套件在静态同源 bootstrap 安装诊断后，加载同一已编译夹具，检查变化和暂停的 PNG 像素、native Skia 上传、audio graph、菜单、对话框、源替换和释放，不替换成 DOM 播放器，不复用个人 profile。Compose 验证期间，不运行共享夹具输出的独立构建。`node client/composeCore/wasm-media-host/serve.mjs` 输出本地夹具 URL，添加 `?audio=true` 用于纯音频，`?media=/fixture-fhd.mp4` 用于较大样本。

Host 检查覆盖视频变化、Web Audio 信号路径、时间戳偏差、暂停、seek/resume、静音、结束、错误和释放。Compose 还观察真实 Skia texture 上传尺寸，比较变化和暂停 scene 像素，操作覆盖的 Compose 按钮及 Escape 关闭对话框，检查裁剪角、替换播放中的源，验证移除关闭 Worker 和 audio graph。WebGL instrumentation 只观察调用，不替换结果。截图、JSON 指标和 benchmark 结果保留在 `build/wasm-media-research`，不跟踪会话报告。原生 `<video>` benchmark 只是测试对照，不是生产 fallback。
同步夹具每秒包含一次白闪和音频脉冲。检查将捕获的 Compose canvas 白闪起点与 Web Audio analyser 脉冲比较，映射到浏览器 audio-output timestamp，在 seek 前后使用 80ms 边界。原始 graph-onset 差异单列诊断。设置 `MEDIA_SYNC_OUTPUT_DELAY_MS=200` 可在 timestamp API 模拟额外输出队列，必须延迟视频而不改变音频调度。默认和注入延迟检查都使用 `MEDIA_RENDER_PATH=rgba-copy` 与 `MEDIA_RENDER_PATH=gpu-texture`。注入只改变报告时序，不改变实体设备。Canvas 捕获本身增加诊断 readback 成本；观察到的唯一帧率不同于 Host 分派或原生 video callback。非零 analyser 信号证明 graph 有声音，不证明实体扬声器已被听见。时间戳和 headless 检查不能证明实际音画同步，人工试听和实体输出延迟测量仍为独立验收。

Texture 验证在运行 `verify-compose.mjs`、`verify-compose-firefox.mjs` 或 `verify-av-sync.mjs` 前设置 `MEDIA_RENDER_PATH=gpu-texture`。`benchmark-compose.mjs` 在同一已编译 Compose 夹具比较两条路径，在 Chrome 和 Edge 上对 320x180、1280x720、1920x1080 各交错运行三轮，记录真实 canvas frame hash、Worker readback 和字节、texture 上传调用 CPU 时间、CDP 主线程任务时长、renderer 身份及资源释放。上传调用耗时不代表 GPU 完成；canvas 捕获和诊断对两条路径都增加开销。PNG 比较检查暂停帧的颜色和方向。`verify-gpu-lifecycle.mjs` 覆盖缩放、重挂载、旋转媒体和 context-loss 清理。结果截图留在 `build/wasm-media-research`，不能将一次机器专用短运行推广为持续性能或实体延迟保证。

源码构建 Skiko 会使 Web release link 失效。并发链接填满共享 Kotlin 编译器堆时，确认 GC 压力，不将停滞认作媒体失败。用受限 worker 顺序运行完整检查，不停止共享 compiler daemon，也不改变测试预算：

```bash
./gradlew assemble -Pserver.flavor=dev -Pserver.buildType=dev --max-workers=1 --console=plain
./gradlew detekt -Pserver.flavor=dev -Pserver.buildType=dev --console=plain
./gradlew validateDebugScreenshotTest -Pserver.flavor=dev -Pserver.buildType=dev --max-workers=1 --console=plain
```

最终 release 路径性能比较设置 `MEDIA_COMPOSE_BUILD=production`，提供夹具 `productionExecutable` 分发。快速开发检查不设置。记录构建类型，不将两种分发样本混在同一比较。选择前先运行 `:dev:wasmMediaExperiment:wasmJsBrowserDistribution` 构建生产分发。

Wasm 开发和生产分发必须分开 Gradle 调用。Kotlin 2.4.21 通过同一 npm package 的 `kotlin` 目录暂存两者，一起请求可能触发 Gradle 隐式依赖验证失败，也让暂存目录不再适合作为不可变测试目标。提供 `build/dist/wasmJs` 下复制后的分发，一次构建完成再开始下一次，绝不 benchmark 正被其他任务重写的暂存目录。

### Wasm 媒体回归执行器

`npm --prefix client/composeCore/wasm-media-host run test:browser` 使用 Node 内置 `node:test`，顺序运行九个 verify 模块，每个模块在独立受监督 worker 进程中执行，无需新测试框架依赖。已有 `*.test.mjs` 单元约定保留独立 `npm test` 入口。通过 `npm --prefix client/composeCore/wasm-media-host run test:browser -- worker-regressions` 选择套件，支持多个名称，并保留全部适用矩阵参数。未知名称在运行开始前失败。直接 `node verify-*.mjs` 便于调试，但绕过进程监督器。

| 套件 | 独立用例／有依赖的约定 |
| --- | --- |
| browser / firefox | 五种媒体各自独立；Chromium 还拆分 native baseline、不支持 codec 和缺失源 |
| worker-regressions | open 或 pause 时网络失败；copy/GPU 路径各自的尾部 skip 统计 |
| rgba-failure | preview／playing 与 upload 拒绝／抛错的组合 |
| bitmap-ownership | 已提交释放、待提交释放或替换、播放中释放 |
| compose / compose-firefox | 每渲染路径的 AV、HD、FHD 和音频；播放／替换／释放保留连续流程 |
| gpu-lifecycle | 缩放、重复重挂载、context loss、upload 拒绝、旋转 GPU 与 RGBA 对照 |
| av-sync | seek 前后保持连续；copy/GPU 与 0/200ms 输出延迟组合 |

Chrome 和 Edge 运行全部 Chromium 套件；已安装 Firefox/geckodriver 运行两个 Selenium 套件。工具或 codec 缺失时显式失败，不静默声称覆盖。独立子用例失败不阻止后续用例或矩阵批次。执行器不加重试，保留原 80ms 同步阈值。Benchmark 和探索性 serving 命令保持独立，不属于回归用例。

站点作用域为套件，浏览器作用域为浏览器用例，页面作用域为单独约定。即使断言失败，finalizer 也关闭未结束页面。媒体状态、截图和 trace 终结各有独立 10 秒预算，读取或截图卡住不会消耗 trace 预算。原生 page、browser、WebDriver 和 site 的关闭尝试各有独立 5 秒预算。Chromium 夹具管理 Playwright BrowserServer，优雅关闭失败时调用 kill。Firefox 夹具直接管理 geckodriver 及专用进程树，WebDriver quit 失败时强制终止该树。客户端或会话准备前先注册归属，包括失败路径。文件写入使用 abort signal。这些预算不改变播放等待或 80ms 同步断言。

监督器启动独立 Python 进程，管理每个 worker 及全部后代。受监督入口支持 Windows、Linux，其他 OS 显式失败。`PYTHON_COMMAND` 可选解释器，默认 Windows `python`、Linux `python3`。Windows 暂停创建 Node root，加入 kill-on-close、不可 breakaway 的 Job Object 后恢复。Node 退出后 Job handle 仍由管理者持有。Linux 启动前启用 `PR_SET_CHILD_SUBREAPER`，接管脱离或孤立后代，包括其他进程组中的后代；要求 `/proc` 访问和 subreaper 支持。

Root 退出、测试流程完成、输出 pipe 关闭和容器释放分别确认。Token 绑定、仅追加的 `container.jsonl` journal 报告 root 与容器状态，读取者忽略末尾不完整记录。Node worker 在等待的根用例返回后原子写 `completed.json`。300 秒执行期限与独立 5 秒退出宽限覆盖流程完成和 root 提早退出。到期时管理者终止整个 Job；Linux 则逐代终止并回收新接管子进程。只有空容器、确认 root 退出和有界 pipe 关闭全部满足，才能进入下一模块。外层监督器不将 PID 退出或 `close` 事件等同于完整释放，不操作无关进程。

释放失败中止运行，保留输出和包含 owner/root PID 的 `supervisor-error.json`。Control-pipe EOF 或监督器死亡会要求管理者清理，后者由 Windows parent-process handle 或 Linux parent-death signal 检测。Windows Job handle 关闭也会终止剩余成员。Linux 管理者保持 subreaper 身份，直到未释放后代消失。归属回归需要 OS 进程控制权限，覆盖脱离子进程、继承 pipe 的 root-first 退出、CPU 卡住和无关 sentinel。

显式 finally 与自动 teardown 共用一次 close 尝试。清理错误延后到用例结果确定：用例失败时保留原抛出值，额外失败写入 `cleanup-errors.json`；用例本成功、但资源释放失败时应失败。WebDriver 和 site 归属遵循同样规则，quit 失败不能阻止 site 清理。Playwright 页面关闭前保存 trace ZIP、截图及 JSON 状态和错误诊断；Firefox 失败保存截图、page source 和媒体状态。收集错误作为诊断报告，不替代原断言。
报告（包括每矩阵批次一个 JUnit XML）先写入 `build/test-results/wasmMediaBrowserTest` 下独立 run/batch/suite。独立 verify 命令也在此分配唯一 suite 目录。JSON 指标和具名 PNG 使用同一原始 suite 目录；浏览器工具、夹具和 benchmark 输出仍在 `build/wasm-media-research`。所有 batch 进程关闭后，执行器才将完整原始 run 复制到 `build/reports/wasm-media/<run-id>`，复制完成后发布目录，失败测试也收集。各快照包含 `manifest.json`，记录 batch 和 module 退出码、信号、启动错误与强制终止原因。Worker stdout/stderr 和原始部分 JUnit 留在原始 batch `workers/<suite>`；已完成 batch 的 `junit.xml` 聚合完成的 worker 报告。非零退出、退出信号、超时和不完整报告都形成显式生命周期失败；已完成用例与生命周期失败同时保留，未完成 XML 留作原始证据。中断运行只留原始存储，不发布；先前快照独立且不变。命令输出两个目录。`MEDIA_REPORT_DIR` 是内部 batch 输出边界，不改变夹具或工具位置。
成功用例也保留 trace，方便查看连续生命周期。同步探针保留 action trace 和最终 PNG，但关闭周期 trace 截图及 DOM snapshot，避免第二条捕获流影响帧时序。运行前构建 Compose 夹具并生成媒体，保持输出不可变直到全部 batch 完成。测试衡量浏览器渲染和 audio graph，不是实体扬声器或显示延迟。

### 按实际 1080p 视频尺寸分析呈现

`generate-benchmark-fixtures.mjs` 生成 14 秒、30 和 60fps 的 720p/1080p H.264/AAC 输入，包含编码的 16-bit 帧编号和彩色校准边缘。`benchmark-fullhd.mjs` 以 `display=1080p` 提供 release 夹具，断言 DPR 1、1952x1360 canvas，并在实际 1920x1080 边界检查四个视频边缘。它交错比较 RGBA、legacy GPU 同步和 guarded GPU import。专用 Worker 从两行捕获条带解码 ID，关闭每个捕获帧，报告唯一媒体帧、跳过 ID 和呈现间隔。Canvas capture 仍只是浏览器输出观察器，不是实体显示探针。使用 `MEDIA_BENCH_NO_CAPTURE=1` 获取配套 CPU-only 样本；CDP 主线程任务时长不包含 Worker observer，不能将其解释为进程或解码器总 CPU。30fps 输入形成上限，测试余量应使用 60fps。

`MEDIA_BENCH_PROFILE=1` 启用 `media.gpu.*` timing 和 `media.rgba.*` handoff mark；最终计时样本保持关闭。`MEDIA_BENCH_GPU_TRACE=1` 额外跟踪活动 shader 实际采样视频 texture 的 GL draw。这些是已提交绘制命令，不能等同于观察到的 canvas frame ID 或显示输出。诊断 texture tracker 跟踪真实 bind，不额外逐上传 `getParameter`。`profile-presentation.mjs` 单独测量旧的全区域 readback observer 开销和 GL 调用数。

两条解码路径都读取 `VideoSampleSink`。视频 RPC 请求携带 media-clock 快照；sample 比快照落后超过已有 50ms 预算时，在昂贵 canvas 转换、readback 或 VideoFrame transfer 前关闭。请求传输期间时钟可能前进，Host 因此仍保留 transfer 后检查。`skippedBeforeConversion` 区分提前剔除、已传输帧及 Host 迟到丢弃，避免过载 copy 路径反复用帧预算读取随后立即被 Host 丢弃的像素。Raster 适配层保留上游 opaque-canvas/Firefox alpha 变通，旋转翻转使用公开 sample 绘制。不引入 native texture 池，也不修改 Skia 管理的 Image。

同时报告导入帧和观察到的唯一媒体 ID，并给出三轮范围。即使每个导入 texture 都由真实 GL draw 采样，后者仍可能较低：canvas capture/readback 是另一条 pipeline，不能证明实体呈现丢帧。当前 draw tracer 证明视频 texture 被采样，不证明最终屏幕呈现或完整 offscreen-layer 来源链。30fps 的小幅观察差异不能证明导入饱和。60fps 时按每秒和每导入帧报告主线程时间，因为路径处理帧数可能不同；两种指标都不包含完整 Worker、解码器或 GPU 成本。保留原同步策略供消融，不应仅因移除 flush/reset 就声称提速。

Draw tracer 规范化 nullish framebuffer 参数：此 runtime 可能将 native handle 零映射为 undefined 默认 framebuffer，WebGL 将其视为 null。开启 tracing 时，前六次 video draw 还查询真实 draw framebuffer，验证 tracker。最终计时运行没有这些诊断查询。

### Windows 有窗口 Web 呈现诊断

同一 release 夹具支持 `benchmark-fullhd.mjs` 的 `MEDIA_BENCH_HEADED=1`。使用已安装 Chrome/Edge、隔离 profile、DPR 1，以及主显示器上的普通测试窗口。Windows `DesktopFrameProbe.cs` 在物理 1920x1080 校准四边，验证浏览器进程拥有前台窗口，测量期间只读取编码的两行条码。它恢复线程 DPI 上下文并释放全部 native handle，不修改系统显示设置。桌面 observer 衡量 DWM 可见内容，不是光学面板扫描输出。

设置 `MEDIA_BENCH_CASES=720-30`、`MEDIA_BENCH_CAPTURE=both`、`MEDIA_BENCH_GPU_TRACE=1`、`MEDIA_BENCH_OBSERVER_CONTROLS=1` 和 `MEDIA_BENCH_OUTPUT=headed-study.json`，对 RGBA、legacy GPU、恢复状态 GPU 交错执行三轮，分别启用或关闭 capture。第一轮还为 GPU 路径分别关闭桌面采样和图形 tracing。每条件只有一个样本，只作诊断，不是性能置信估计。Helper 等待手动激活后测量，丢失前台即拒绝。浏览器完成前保持测试窗口前台。`MEDIA_BENCH_FOREGROUND_GATE=1` 还会在 `build/wasm-media-research/headed-window.json` controller handshake 暂停；只有用户准备激活窗口后，才在 `headed-foreground.json` 确认匹配 token。不绕过键盘或输入，不使用个人 profile。

图形捕获使用显式 CDP traceConfig，包含 cc/viz/gpu、benchmark、user timing 和 compositor frame 事件，排除无关类别。类别发现可能返回空列表，用它过滤请求会静默启用宽泛默认 tracing。确认保存 trace 包含测量 start/end mark、GPU 事件和 PipelineReporter。Trace 文件名包含输出数据集名，分开原型证据与后续研究。

运行 `analyze-headed-study.mjs headed-study.json headed-analysis.json`，关联源视频 ID、最终 canvas draw mark、包含它的 AnimationFrame interval、AnimationFrame::Presentation feedback 及桌面条码观察。大型 trace 标识保留为字符串。同一 feedback timestamp 可覆盖多个更新，不能将每次 callback 都计为独立可见视频帧。NO_UPDATE_DESIRED 不是丢帧，整个窗口 compositor 丢帧也不能证明视频内容丢失。只有桌面确实观察到、具有匹配且未合并 presentation feedback、却不在 capture 中的内部源 ID，才计为已确认 capture 遗漏。未观察到的桌面 ID 保持不确定，不自动算真实丢帧。

隔离 capture consumer 时，设置 `MEDIA_BENCH_QUEUE_STUDY=1` 并选择 `MEDIA_BENCH_VARIANTS=gpu-restored,gpu-restored-buffer8`。只改变 observer MediaStreamTrackProcessor 队列容量（1/8），在支持时记录 native totalFrames 和 discardedFrames，测量 Worker copyTo 调用。捕获帧不等于唯一源视频 ID：capture stream 可以重复或遗漏部分 ID，而 consumer queue 不丢帧。补充 headless 研究与有窗口呈现证据分开。Observer 控制不改变生产媒体队列和默认 RGBA 路径。

`MEDIA_BENCH_FRAME_STAGES=1` 只在 transfer 后的 decoder frame 未经 texture upload 就关闭时记录调用栈。历史 `GpuBridge.replace/releasePending` 栈识别旧 pending-slot 覆盖问题。计时结论应关闭此可选 instrumentation，将有意 generation 取消、clock-late 丢弃与 pending-frame 丢失区分。

GPU bridge 现在通过 `offer` 接收帧：false 保留调用方管理责任，true 转移。Pending slot 满时拒绝同播放 generation 的另一帧。Host 保留该帧并停止请求新帧，直到被接受或超过已有 50ms 迟到预算。Texture 消费在 Compose draw 返回后调度 producer 唤醒。Pause、seek 和源替换可取代旧 generation，过时消费回调不能重启它。Preview delivery 可取消等待，失败或取消释放未转移帧。Bridge 和 Host 各最多持有一个已转移 video frame。

测量 schema 3 将 canvas 观察命名为 `canvasCaptureUniqueFps`、`canvasCaptureSkippedIds` 和 `canvasCaptureGapMsP95/Max`；独立桌面观察为 `desktopObservedUniqueFps`，无采样时 null。Host `acceptedFrames` 计数的是向 UI 转交管理责任，不是屏幕呈现。`backpressureWaits` 计数拒绝的交付尝试，不是丢失源帧。旧 `presented`、`presentedFps`、`observedUniqueFps` 或通用 gap 字段使用者应改用对应显式计数。Headed analyzer 只为比较读取历史 `observedUniqueFps` 数据。这些变更不使 captureStream 无损，不改变音频时钟或迟到上限。

Schema 3 使用不显式请求帧率的 `canvas.captureStream()`，benchmark 行标注 `capturePolicy: "per-paint"`，关闭 capture 时为 `"disabled"`。此前 schema-2 full-HD 数据使用 `captureStream(120)`，其他 canvas observer 使用 60fps 请求计时器。显式帧率选择由计时器限制的 listener，不会捕获每次变化 paint。视频更新间可能有多次 canvas paint，因此请求帧率高于视频帧率不能证明完整观察全部视频 ID。不要混合不同 capture policy 的计时样本。手动 `captureStream(0)` 加最终 video draw 后 `requestFrame()` 是另一诊断，没有完成确认。Per-paint capture 仍受实时交付、队列和 GPU readback 限制；保留独立桌面或呈现证据，不声称无损录像。

Worker 回归在 Chrome 和 Edge 上使用内置 decoder，阻塞初始和 pause 触发的源请求，要求终止错误且无存活重试，并在两种 wire 路径将过期 sample 消耗到 EOF。网络重试策略使用固定 Mediabunny 的 `getRetryDelay`。Video EOF 响应带 `ended: true` 和 `skippedBeforeConversion`；Host 在向 playback 返回 null 前统计 skip。EOF 不增加 decoded/accepted frame、copied byte 或 readback time。诊断 `videoSkips` 包括 EOF，使 full-HD benchmark 总数不依赖后来再交付一帧。

原生 video baseline 读取 `requestVideoFrameCallback` 元数据 `presentedFrames`，callback 通知数单独报告，因为延迟 callback 可覆盖多帧。生命周期检查保留原 AudioContext，即使 Host 清除引用，也能独立验证 closed 状态。

RGBA bitmap-install 错误通过 MediaHost rendering-failure 操作报告，终止解码和音频，不取消 Compose scene。取消的导入仍传播取消，被取代 effect 不能报告过时失败。RGBA failure 检查在 preview 和活动播放时，让 native bitmap 安装返回 false 或抛错，观察错误状态及资源清理，再操作真实 Compose 控件卸载和重挂载，验证新播放恢复且无未捕获浏览器异常。使用 `MEDIA_COMPOSE_BUILD=production` 在生产夹具执行。

RGBA 帧在发布时从导入协程转交给稳定 bitmap 管理者。管理者保留已提交帧和最新待提交帧，commit 后退役旧帧，释放时显式关闭全部余留帧。归属不依赖每 bitmap 的 DisposableEffect 到达 commit。向已释放管理者的迟到发布立即关闭，取消导入仍释放未转交数据。Bitmap-ownership 浏览器检查统计 native install/finalizer，并延迟 GC 清理回调，防止 GC 掩盖遗漏关闭。覆盖已提交释放、commit 前发布后移除或换源，以及播放中替换和释放。在生产夹具使用 MEDIA_COMPOSE_BUILD=production 执行。

## 离线 E2E 应用覆盖率

向 `scripts/test_scripts/build-and-test.sh --e2e --plain` 添加 `--coverage`，或向显式 Appium/CLI E2E Gradle 任务传入 `-Pe2e.coverage=true -Pserver.flavor=dev -Pserver.buildType=debug`。关闭覆盖率时，普通分发和 release 应用不包含 control receiver、collector 或重写字节码。整个命令包括安装，都必须获取并续租 Android 设备租约。桌面仍需可交互、未锁定会话。Windows 桌面进程 PATH 不应包含 MSYS 图形 DLL 目录，混用其 EGL 与 Nucleus 内置 ANGLE 可导致启动崩溃。带覆盖率的 Windows launcher 使用 manifest classpath JAR，在不超出 cmd.exe 命令长度限制的同时保留依赖顺序。

JVM 应用分发、桌面 classpath 和后端测试镜像使用 Kover 0.9.11 离线插桩。Android debug 在 D8 前转换项目所有的 class 文件，使用相同 Kover runtime。测试专用 counter-store 边界让快照与计数更新串行。JVM 快照使用每进程文件请求及进程管理的 running/shutdown marker；Android 快照使用仅 debuggable、受系统 DUMP 权限保护的 receiver。Driver 关闭、force-stop 或容器删除前收集快照，包括失败和应用重启前。私有控制文件随夹具移除，导出的二进制报告与用例产物保留。桌面根据 launcher 文件选择当前 launch PID。Driver 强制退出可能留下旧 running marker；保留已导出快照，重启后不再请求退役进程。

全部所选 E2E 执行器在启动真实夹具前准备不可变应用产物。覆盖率后端镜像使用独立 `e2e-coverage` tag。测试镜像仍遵循 `distTar` → `copyTestDockerDistribution` → `buildTestDockerImage`。UI 测试期间，不从第二次 Gradle 调用重建共享 runtime 文件。Linux/macOS 桌面收集要求测试执行器与已启动应用共用夹具文件系统，与已有 launch/session/log 约定一致。

Wasm 覆盖率还需要宿主 Rust 工具链和 Cargo。固定的 `dev/wasmCoverage` 工具验证并重写 webpack 输出标明的准确 Kotlin development Wasm binary，添加不改变栈的 boolean global probe，不重新编号 function、import 或 type。分母只包含映射到已链接 executable operator 的仓库 Kotlin 源行。超出物理源文件的 compiler anchor（含空 EOF 行）保持 unmapped。被 dead-code elimination 删除、未映射、第三方和 JavaScript 代码不在分母内。Collector 观察 window 实例；Worker 实例需要独立收集通道。这是映射行覆盖率，不提供 source branch 指标，单独导出 LCOV 和 HTML，不与 Kover 合并。测试服务只在带覆盖率分发注入 collector；每个 document 和 module 实例都必须确认快照。显式边界快照确认后抑制多余 unload beacon，新实例或未确认实例仍用 beacon fallback，传输不完整仍报错。Chrome、Edge、Firefox 保留顺序、隔离 profile 矩阵。

每用例导出到原始 `build/test-results/<task>/artifacts/.../coverage`。根 `e2eCoverageReport` finalizer 只读取已完成文件，生成 `build/reports/e2eCoverage/index.html`，链接 JVM 汇总、各 JVM/Android 变体及 Wasm 报告；Android 与 JVM 字节码变体绝不合并成一个百分比。原始字节码身份与源指纹避免用已变源代码报告旧二进制。数据缺失、空或不完整会让报告任务失败，并在索引留下明确错误。普通单元 Kover 报告独立保留，不将其百分比加到 E2E 百分比。报告与所选 E2E 任务同次调用，以保留 runner 选择；独立报告任务读取所有 runner 产物目录，并拒绝混合构建。

### 单元与 E2E 并集

启用覆盖率后，`prepareE2eCoverageArtifacts` 在准备或启动不可变 E2E 产物前，先运行已有普通单元 Kover XML/HTML 任务。普通后端夹具保留新构建 `latest` 镜像，覆盖率夹具使用独立不可变上下文构建的 `e2e-coverage` 镜像。每次带覆盖率 E2E 还会 finalize `coverageReport`，写入 `build/reports/coverage/index.html`、`lcov.info` 和 `summary.json`。普通 `build/reports/kover` 与独立 `build/reports/e2eCoverage` 输出保持完整。用已完成 E2E 产物重新生成并集：

```shell
./gradlew coverageReport -Pe2e.coverage=true -Pserver.flavor=dev -Pserver.buildType=debug --console=plain
```

跨平台摘要按仓库相对源路径和可执行行号取并集。任一测试层命中的行只计一次；页面显示 unit hit、E2E hit、重叠及合并分子，分母为已报告 executable line 并集。共享源码在一个平台命中，不能证明所有平台覆盖。平台专用字节码、branch 和 instruction 指标查看独立报告。Wasm E2E 映射行通过 LCOV 参与；未插桩 Wasm 单元测试不贡献覆盖率。普通 Kover 测试选择不变。生产源目录之外的生成或外部源显式列出；源归属含糊、外来 LCOV 路径或 E2E 数据不完整都会让报告失败。不相加百分比或跨平台 branch counter。

集成覆盖已有 Android、桌面、CLI 和 Wasm E2E 执行器及其 JVM 后端容器。可选 iOS 项目在此流程没有 E2E runner 或 collector，未实现 Kotlin/Native 覆盖率。

平台受保护会话历史测试仍在 `app/composeApp/src/headlessTest/kotlin/platform_storage`，`platformStorageTest` source set 供 JVM 与 Android 设备共用。Android 宿主 Robolectric 排除此目录，因为 AndroidKeyStore 需要真实 OS provider。全部会话、签名和加密断言保留。设备初始化基类提供真实 instrumentation context，不安装模拟 keystore，也不改生产存储。

PDF 宿主测试使用 1 GiB worker 堆，容纳八个并发冷启动全尺寸 CJK 字体实例；并发、native 清理检查和字体断言不变。

## Release 警告诊断

Yarn lock 只在根构建通过 `lockFileDirectoryProperty`、`lockFileNameProperty` 配置。JS 使用 `kotlin-js-store/yarn.lock`，Wasm 使用 `kotlin-js-store/wasm/yarn.lock`。API 迁移不改 npm 依赖，无需重建锁文件。

两个 Compose 模块都通过 `withDeviceTestBuilder { sourceSetTreeName = "test" }` 配置 Android 设备测试。默认层次提供 `commonTest`，不要添加跨树 `dependsOn(commonTest)`。

将 `./gradlew assemble -Pserver.flavor=dev -Pserver.buildType=dev --warning-mode all --stacktrace --console=plain --max-workers=1` 作为一行执行，归因剩余 Gradle 弃用。Windows 单独运行 Detekt。保持警告开启，构建成功不能证明兼容 Gradle 10。Compose Hot Reload 的 `HotReloadUsageType` 警告来自插件，不是 Yarn lock 配置。

Panel ViewModel 创建需要管理者提供协程 scope。Android scope 跟随应用生命周期，iOS 使用 root composition scope，移除 controller 时取消工作。`createPanelUIViewModel` 调用方必须传入已有生命周期 scope。

当前依赖还报告较新 Material3 BottomSheet 弃用、Wasm interop opt-in 及固定 Skiko 源码归档诊断。与 Release source-tree/Yarn/API 迁移分开跟踪，不全局 suppress，也不编辑 `build/upstream` 生成副本。

固定插件下，`--stacktrace` 将 `Project.getProperties` 归因到 kotlinx-rpc（`RpcPluginConstKt.isInternalDevelopment`）。Gradle 11 的 `Configuration.setVisible` 来自 Kover、AGP、KSP、Detekt、JMH 和 Compose Hot Reload 配置代码，需要上游插件迁移；改应用源码或关闭诊断不能解决。

浏览器请求和媒体探针保留页面 Promise 构造函数：对原生 fetch 或 getUserMedia 结果链式处理，测试 gate 使用 `window.Promise`。Firefox 在独立 realm 执行 WebDriver 脚本，sandbox async wrapper 可能在观察操作完成前导致 Wasm Promise cast 失败。探针保留真实响应字节、分页 token、取消和 track。社区创建验收等待 compose 表单移除后再切区域，以观察提交成功，而非取消页面 scope。认证重启检查清除旧 readiness marker，等待真实用户数据恢复后，再解析随账户变化布局的控件。

`OnCommunityCreated` 将完整返回社区写入详情缓存和当前用户已加入社区集合（`JOINED`、空查询、无 target、`UNSPECIFIED` poster filter），让已加载的空社区列表在创建后更新，无需测试专用 Refresh。浏览器滚动辅助方法在可信 wheel 到达前保持 semantics overlay 隐藏；分派前提交隐藏 hit-test 区域，失败时保留 wheel 诊断。

私钥失败验收断言可见、本地化的 PEM 格式提示及重启后的访客状态。有效但未注册 key 先检查实际派生地址，再检查服务端拒绝；缺少浏览器专用 error description，不能证明原生验证成功。共享私钥输入也将显示地址发布为原生 accessible label。

Linux 凭据 E2E 故障注入现在选择不可用 libsecret 库，不是缺失 CLI。使用标准 Linux 套件；共享 launch helper 设置测试专用 library override，正向大值用例以有效且 PEM preamble 超过 16 KiB 的 key 登录，重启后恢复同一账户。迁移测试真实原生存储实现。

Android 文件名 selector 匹配完整 accessible filename，可带合并元数据。UiAutomator2 [UiExpressionParser](https://github.com/appium/appium-uiautomator2-server/blob/master/app/src/main/java/io/appium/uiautomator2/utils/UiExpressionParser.java) 仅去除参数引号并反转义引号，不要对 regex 反斜杠做 JSON escape。原生点击等待控件 displayed、enabled，且连续观察边界不变，再对当前元素点击一次。这保留正常动画设置和 deadline，避免抽屉切换中输入。Driver snapshot 和录像先于 driver 关闭完成；较长失败 bugreport 在关闭后收集，避免诊断使闲置 session 过期。

Android 视频保留 Media3 controller 默认可见时长。E2E 只在 PlayerView 暴露“Show player controls”时显示控制，再点真实 Pause，回 Home 前验证 paused 状态。不要盲目点击视频表面，可能隐藏已可见 controller。见上游 [PlayerView 控制约定](https://developer.android.com/reference/androidx/media3/ui/PlayerView#setControllerShowTimeoutMs(int))。附件页为 toolbar 和内容保留 safe drawing inset。

Media3 进度无障碍事件可使 UiAutomator 一直非 idle，直到控件自动隐藏；等待 idle 后再找 Pause 会错过可见 controller。Android 动画媒体交互 scope 只临时关闭 waitForIdleTimeout，失败时也恢复精确原值，并保留明确暂停断言和普通 deadline。遵循上游 [UiAutomator idle-wait 约定](https://github.com/appium/appium-uiautomator2-driver#poor-elements-interaction-performance)，不关闭动画、不扩大元素可见性。

Android 视频通过 AndroidView 使用页面管理的 Media3 PlayerView。导航和 PiP 转换可能重建 Surface、短暂黑屏；接受此瞬态，不引入 activity-root overlay 或手动 viewport 归属。最终 release 时 view 分离 MediaController。播放状态和命令仍在 MediaPlayerService。PiP 验收继续在进入前、小窗稳定后、恢复后及第二次进入验证解码 RGB 帧，持续黑屏仍失败。已移除 root-surface 实现的历史验收，不能证明此页面管理实现已验收。复现和上游归属见 [issue #117](https://github.com/storytellerF/A/issues/117)。

### Wasm 启动字体选择

App 与 panel 共用 client/composeCore/local-font-access 的 native HTML font gate。它使用浏览器系统 CSS 字体，让选择和权限说明在 Compose fallback 下载前可读。只移除 Noto SC 应用字体，保留 Material Symbols 图标字体。Compose 1.12.2 按需从 Google Fonts 下载缺字 Noto subset。

Origin 本地 `a.preferSystemFonts` 偏好为缺失、`false` 或 `true`。支持此 API 的浏览器必须首次明确选择。`false` 挂载使用 Compose 默认字体的正常内容；`true` 只表示意图，必须有 local-fonts 权限、匹配的已安装 CJK face、字体字节及成功 Compose resolver 预加载，才能挂载业务内容。显式检查 Skia 解码，因为 Compose blocking-font preload 在解码失败时可能静默回退默认 typeface。权限重置也使等待 Compose 确认的字体失效。拒绝、缺字体或解码错误保持 gate 可见，并提供明确 downloaded-font 出口。撤销权限卸载业务内容。不支持 API 的浏览器走默认路径，不覆盖保存意图。重载后仍要求用户激活的浏览器显示显式字体加载按钮。App 与 panel origin 的偏好和权限独立，不持久化本地字体字节。偏好存储被阻止时，明确选择在本次页面访问仍有效。

已有业务 E2E 在清除存储后、加载 Wasm 前，于静态同源 bootstrap 设置 `a.preferSystemFonts=false`。这是正常产品偏好，不是测试模式绕过。浏览器导航和独立 semantics 测试使用同样准备。字体 gate 场景故意从空偏好开始，覆盖三个浏览器；Chromium grant/deny 夹具控制真实权限，使用真实已安装字体。Granted 路径要求生产偏好列表中的已安装 CJK 字体，例如 Windows Microsoft YaHei 或 Linux Noto Sans CJK SC。Firefox 验证不支持路径。选择器通过已有显式测试 locale 保持英文。Gate 状态管理者通过 `:client:composeCore:testFontGateRuntime` 测试；app/panel FontGateTest 包含真实 gate 启动、持久化、pending/denied/granted 和撤销场景。`BrowserFontPreloadTest` 调用生产 Wasm decoder/preloader，拒绝空或无效字节。使用 `:client:composeCore:wasmJsBrowserTest --tests "*BrowserFontPreloadTest*"` 和正常 flavor/build-type 运行；直接 Gradle 浏览器运行要求 `CHROME_BIN`。

常规 `assemble` 与 Wasm Appium 开发分发分开 Gradle 调用。Kotlin development 和 production CompileSync 共用生成 npm package 目录；同一图同时请求两种 Webpack 模式会触发 Gradle 隐式依赖验证。启动浏览器套件前完成全部运行时编译。

### 社区主题字体

社区字体设置应用于主题 Markdown 和主题编辑器内容，包括直接链接、社区帖子、公开房间帖子、源码编辑、block 编辑和预览。控件、作者元数据和外层应用主题仍用应用字体。内容与代码使用独立 family；源码编辑器优先代码，其次内容。配置 family 缺失或无效时使用应用字体，Markdown 代码保留等宽默认。缺字使用平台 fallback，不提供可配置社区 fallback family。含已停用 fallback 字段的已有 JSON 仍可通过 commonJson 未知字段处理读取，后续写入省略这些字段。使用合成夹具验证实际 glyph advance，不能把 resolver preload 视为字体数据有效的证明。

Downloader 是应用管理的内部资源服务，目前用于社区字体。普通文件下载通过平台 URI handler 打开 URL。内部资源记录及暂停恢复仍位于文件浏览器 App downloads。全部平台 provider 返回同一服务。Android 前台通知观察应用任务数，不管理任务。自适应 UI 测试进程固定 English/US，使 selector 稳定；同时请求常规 JVM 和自适应任务时，后者在前者之后运行。字体夹具删除文件前等待进行中的 native 构造结束，包括取消后。
Guest 数据库将记录存入 `public-resources-v1:<server-identity>` 集合；磁盘和 OPFS 路径使用匹配的新 namespace、文件 ID 与元数据 revision。Server identity 从配置 HTTP root 派生，与活动账户无关，避免不同服务相同文件 ID 冲突。旧账户或 guest 下载集合和目录被忽略，不扫描、迁移、删除或重置数据库。Room 保持 version 2；成功处理 generation 序列化在 DownloadEntity.data，无实体或 schema 变更。

Downloader 管理注册、传输、处理和状态发布，FileEngine 提供平台文件 IO、归档提取及租约。每次 admission 将发起 Cookie context 和账户 identity 快照到请求管理的短期 client。关闭或切换账户不关闭该 client，也不取消应用任务。请求完成或取消才关闭；后续 resume admission 重新捕获当前账户。浏览器 HttpOnly Cookie 仍由浏览器管理；捕获的账户 header 会拒绝身份变化，不静默使用另一账户。因此退出登录可使已接纳的受保护读取失效。服务端撤销仍让受保护请求失败；共享服务没有保存 key 登录、修改重放或永久账户会话。

`download(file)` 注册或复用任务，不重试失败或暂停记录。`resume(...)` 是唯一重试命令，可复用完整源归档。`resume(FileInfo)` 提供同一已存 revision 的更新传输 URL；字体重试传入当前 `FileInfo`，不改变共享字体 identity。Revision 不匹配不能替换已存元数据。Resume 元数据在共享文件租约内解析，其他任务无法在查找与注册之间替换 revision。Admission 检查和注册在 task-registry mutex 下原子进行。Server/file key 正在 preparing、executing 或 stopping 时，重复 download/resume 立即返回，不捕获 client、不排队，即使 URL 不同。忽略的 download 返回 null，收集器继续观察已有数据库任务。不同文件在 registry mutex 外并发。Pause 等待 native write 和 worker 清理、发布 PAUSED 后，才重新开放 admission。注册取消传播到尚未完成的 admission；已注册工作归应用管理。失败在请求边界隔离，清理失败作为 suppressed exception 保留在原错误上。

字体观察器结合数据库快照与 registry activity，在任务清理期间让重试控件保持 Loading，随后才发布失败或暂停。构造所选成功 generation 前也发布 Loading，包括数据库跳过中间快照的情况。因此重试进行中显示进度，而非仍可点击的失败状态。

真实网络传输前，Downloader 检查 MinIO/S3 SigV4 X-Amz-Date 和 X-Amz-Expires。签名过期或到期格式错误时，通过已有认证 file-info API 刷新一次，请求任何源字节前核验文件 identity/revision。完整本地源无需刷新；无 SigV4 到期声明的 URL 保留行为。刷新失败，包括 guest 认证失败，仍是下载失败，使用应用字体，不影响帖子阅读。公开社区响应本就可提供 guest 可用字体 URL。

只有源 writer 关闭、提取成功后，才一起保存 PROCESSED 和新的单调递增 processedGeneration。共享文件租约覆盖快照重检、generation 递增、目录替换和终态发布，包括跨浏览器 tab/worker。数据库 Flow 是快照，不是事件日志，使用者可能错过全部中间重试状态。字体源选择 revision/path/成功 generation，忽略无关进度或行变化，用 mapLatest 在同一文件租约内重查数据库，再验证构造。构造失败发出失败状态并继续观察；同版本 resume 成功始终从新 generation 构造替代。全部平台必须检查重复 weight/style、元数据和 family。提取完成没有独立 manifest。

CommunityFontStore 每个公开文件 revision 注册一个 stateIn flow。全部页面和账户共用上游验证构造，接收已构建 family/resolver。没有独立 finished-bundle cache、pending-load cache 或 reload event bus。WhileSubscribed 在五秒停止宽限内保留上游，停止时立即重置 replay。Registry 按 collector 租用 entry，不驱逐活动订阅，宽限后删除并取消 idle entry；旧 idle timer 不能驱逐新订阅 entry。重试期间内容保持已显示 family，直到新成功快照替换。Android/JVM 后台构造，从 native 文件加载，不完整读取源 ByteArray；Wasm 构造仍在浏览器事件循环，归档处理放专用 worker。Skia family 预加载到 family 管理的 resolver。一个读者退订不会取消其他读者或下载任务。

缓存 identity 使用对象元数据，不用会过期的签名 URL。提取目录缺失或为空时从源重建，源缺失则重新下载。每次提取离开 PROCESSED 状态，在共享文件租约内替换 `<source>.extracted`，失败或取消清除部分输出。显式 resume 连非空部分提取也重建。仅目录非空不能授权字体读取。存储拒绝或配额错误显示为失败，不假定浏览器缓存可用。

Wasm ZIP 在 `app/composeApp/file-worker` 的专用 module Web Worker 处理。Worker 使用固定 zip.js native-stream 实现，直接读取 OPFS 源，验证路径、重名、声明及实际大小、CRC，关闭全部 output writer 后在内存返回文件元数据，不将整个归档传过页面 Wasm heap。页面在 Worker 成功后保存数据库完成，不将元数据持久化为第二完成标记。取消等待 worker 确认和 writer 关闭，再删除部分提取目录；每次操作后终止 worker。Worker 错误使处理失败，不回退主线程提取。本地 npm 源作为 Wasm install 和 webpack 任务输入；依赖变化后只重建 `kotlin-js-store/wasm/yarn.lock`。

浏览器和 native ZIP 最多允许 128 总 entry（含目录）、64 MiB 源和 80 MiB 展开数据。Native 流式处理时执行限制，打开输出前拒绝大小写不敏感的重复规范路径。Native 拒绝末尾为点或空格的路径组件，防止 Windows 别名。Native 输出以 CREATE_NEW 原子创建，已有目标（包括 DOS 短名别名）使处理失败，不截断。两个平台都限制真实展开输出，包括非字体文件，并在拒绝后删除部分目录。普通非归档下载流式写存储，不受 ZIP 限制。字体 bundle 最多 18 个 face，源 64 MiB，解码 face 80 MiB。
Android、JVM、Wasm 发布 family 前，拒绝重复 weight/style，比较文件名推断样式与原 OpenType OS/2 `usWeightClass`、`fsSelection`（italic/oblique）。全部 face 内部 family 必须相同，优先 Unicode `name` ID 16（typographic family），只有缺 ID 16 才回退 ID 1。Subfamily 是样式，不是 family 标识。名称选择独立于平台：Windows English、neutral Unicode，再到确定性 locale。必需元数据缺失、无效或含糊时，整个 bundle 通过已有字体失败和默认字体路径失败，不对 face 去重或跳过。Android 只从文件读取 SFNT 元数据表；Skia 在已有验证解析中读取原表。集合 entry 仍指向 face index 零。合成测试字体显式编码 weight、slant 和 typographic family，同时保留不同 glyph advance。

共享 stateIn replay 在有订阅或停止宽限期间，管理最近构建 FontFamily/resolver。框架和活动 renderer 可能保留更久。Native backing file 保持可供另一展示 resolver 使用；移除 idle flow 不得关闭内容仍引用的框架资源。Skia 公共 loader 对无效输入静默替换系统 face，因此框架加载前仍需显式验证，每次构造增加第二次解析。Wasm LoadedFont 保留 byte supplier，这些数组和 Skia native data 与 family 同寿命，不另设源字节缓存。ByteArray/JS/Skia 转换仍可能复制。浏览器字体保留按 server 最多跟踪 12 个成功 OPFS 版本，源与展开数据各有独立 128 MiB 预算。索引位于新 public server root，不读取旧 retention index。驱逐删除 revision 前，获取与传输和构造相同的 server/file 租约。Retention 只针对字体，不针对普通下载，系统字体权限字节仍独立。

Android native 字体加载和无效文件拒绝在设备执行 `:app:composeApp:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=fonts.AndroidCommunityFontTest`。保留正常 flavor/build-type 属性及设备租约，测试使用原生字体引擎。

使用正常 flavor/build-type 属性运行 `:app:composeApp:jvmTest --tests 'fonts.*'`、`:app:composeApp:adaptiveUiTest --tests 'device_based.Community*'` 和 `:app:composeApp:wasmJsBrowserTest --tests '*CommunityFontBrowserTest*'`。Store 测试改用连续观察及显式 resume，取代已停用 load/retry 和 cache-eviction 约定。JVM 覆盖共享构造、idle replay 释放、跳过中间快照、generation 更新、账户 client 关闭、server/旧 SQLite 集合隔离、完成顺序、磁盘缓存重建和修复、显式重试、Range、取消及真实 SQLite record identity。
浏览器覆盖真实 OPFS 二进制存储、重建 store、server/revision 隔离、retention、跨 store 和 worker 的 Web Locks、取消 extraction-worker 清理，以及真实 ZIP/font 解码。测试专用 worker entry 对真实 OPFS stream 加诊断，在成功 native write 后暂停，仅取消才释放。浏览器测试断言真实 abort 完成后才删除未发布输出，复用生产 worker 协议及 Kotlin 清理边界。第二测试 entry 观察仍 pending 的真实 native write Promise，再在同一 Worker microtask 阶段调用未改的生产取消 handler，验证取消后 native settlement、先关 writer 再删除，以及保留源归档，避免依赖 page-to-worker 往返抢在 native IO 完成前到达。页面发起取消另有覆盖。Pending 守卫拒绝已完成 write，Node 负例覆盖立即及 microtask 完成。生产入口不导入任何测试探针。
Worker 协议、归档验证和 writer 取消测试先运行 `npm --prefix app/composeApp/file-worker install --ignore-scripts --package-lock=false`，再运行 `npm --prefix app/composeApp/file-worker test`。Native ZIP 后端要求浏览器支持 `DecompressionStream("deflate-raw")`，验收包括 Chrome、Edge、Firefox。共享 Karma 配置识别 Edge `CHROME_BIN`，阻止 Windows 兼容层重启，使 Karma 管理真实浏览器并在退出后清理隔离 profile；否则 launcher 在重启浏览器仍活动时重试，可能删除活动 OPFS 数据。通过 `FIREFOX_BIN` 和 `-Pkotlin.js.browser.karma.browsers=firefox-headless` 添加 Firefox，保持共享 loader 配置完整。UI 测试验证 glyph metric、无效字体默认值，以及加载期间草稿和光标保留。

### 字体预设下载过滤

`deploy/dev-data/medias/files` 的 `*.download.yaml` 为每个输出 ZIP 选择一个 family。`NotoSans.zip` 只含 `NotoSansNerdFont`；独立配置覆盖 Sans Propo、SansM（regular/Mono/Propo）、Mono（regular/Mono/Propo）、Serif 和 Serif Propo，继续排除 Condensed。每个 family 包含上游可用的 Regular、Bold、Italic、BoldItalic。MapleMono 打包为 `MapleMono-NF-CN.zip`，使用同样四样式选择，不制造上游缺失样式。已有混合 Noto 或三样式 MapleMono 上传属于独立对象，应导入新输出并迁移社区引用，不在数据库元数据不变时覆盖已有对象。

`name` 选择输出文件名。可选 `sourceName` 选择共享原始下载缓存名，默认 `name`。全部 Noto 输出复用 `download/Noto.zip`，在 `download/processed` 保留独立文件。在 font provider 的 file preset 注册各配置路径，导入独立输出。环境专用预设账户信息留在仓库之外。

当前 Compose Markdown renderer 不解释 HTML/CSS `font-weight`；行内强调选择 italic/bold，标题字重来自应用字体。预设省略 Medium 等可选字重；引入 HTML weight 支持或其他作者可控字重机制后，补充对应 face。80 MiB 解码上限容纳 MapleMono 四种 normal/bold/italic face，同时源上限保持 64 MiB，各 OPFS retention 预算保持 128 MiB。

### 脚本命名参数接口

运行 `python scripts/test_scripts/run-shell-tests.py --suite named-options`，检查参数验证、调用方引号、产物选择，以及旧位置参数的拒绝行为。使用 `--tests <dotted-id> [<dotted-id> ...]` 选择 Shell 契约测试，不再接受裸测试 ID。进程所有权协议使用 `--status-file`、`--token`、`--timeout-ms`、`--parent-pid`，最后以 `--command` 传入子进程 argv；调用方与进程释放测试应同步维护。参数超过两个的 PowerShell helper 禁用位置绑定，调用方使用命名参数。
