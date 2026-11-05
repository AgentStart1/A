# Project Development Rules

## Global Principles
- Use JDK 21. All modules must target Kotlin/JVM 21.
- Prefer KMP-shareable code. Handle platform differences with `expect`/`actual` or platform source directories.
- Keep the Gradle configuration cache enabled (`org.gradle.configuration-cache=true`) and avoid side effects or IO during configuration.
- Logging: use Napier in shared/app code. Initialize a logging tree only when needed for tests or debugging.
- Use the canonical imported-class Logback XML form documented in DEVELOPMENT.md.
- Static analysis: run Detekt with `config/detekt/detekt.yml`. Do not create or use Detekt baseline files; every active finding must be fixed in source code. Do not add or broaden suppressions to bypass findings. When Detekt rules conflict, disable only the conflicting rule or narrowly scoped configuration necessary to resolve the conflict.

- Diagnose Detekt type-resolution warnings with debug enabled and autoCorrect disabled; rerun the selected task because debug is not a cache input. Preserve KMP source coverage and active rules. Keep autoCorrect disabled by default; opt in with --auto-correct for a dedicated correction run. Do not force a single Gradle worker for Detekt. See DEVELOPMENT.md for known expect/actual and generated-serializer limitations.
- Detekt permits public library entities and public data classes only in the configured `api` and `shared.model` protocol/model scopes and the `client/core` request boundary; keep these exceptions narrowly scoped in `config/detekt/detekt.yml`.
- New Kotlin source files must start with the private-project license header configured by Detekt.
- Coverage: generate Kover reports when needed.
- Environment/secrets: `app/composeApp` and `panel/composeApp` inject values from `deploy/{flavor}.env` through BuildKonfig. `app/androidApp` reads the same env file for deep links. `app/androidApp` and `panel/androidApp` use flavor/buildType values to configure application IDs and build types. Always pass `-Pserver.flavor` and `-Pserver.buildType`.
- Commit messages: start with a verb in English or Chinese and include the module/scope, for example `client/core: add getAllUsers request`.
- Do not use Chinese method names.
- Do not use bare `InputStream`/`OutputStream`; use `BufferedInputStream`/`BufferedOutputStream`.
- Do not use `println` or `System.out.println` in code.
- When editing files, preserve the existing line endings.
- When adding dependencies, verify the correct group and artifact from GitHub or the upstream repository. If the dependency cannot be found, do not downgrade versions.

## Module Responsibilities
- **api**: Defines REST API endpoints, query/path models, and the admin `AdminApi` (`/admin/*`).
- **client/core**: Session management and request wrappers using unified `serviceCatching`.
- **client/composeCore**: Shared Compose UI components, media helpers, cross-platform UI utilities, and related actual implementations.
- **client/asciidoc-parser**: AsciiDoc preview HTML generation used by markdown rendering.
- **client/model-storage**: Abstract storage interfaces and collection identifiers.
- **client/room**: Room-based storage implementation and aggregation entry points.
- **result**: Business-independent KMP result container and error diagnostic contracts.
- **shared**: Cross-platform business models and utilities.
- **app/composeApp**: Shared user-facing Compose UI, actual implementations, and services.
- **app/androidApp** / **app/desktopApp** / **app/cliApp**: User-facing platform entry points.
- **panel/composeApp**: Shared Compose UI for the admin panel.
- **panel/androidApp** / **panel/desktopApp** / **panel/cliApp**: Admin panel platform entry points.
- **backend/***: Backend core functionality, storage, search, and related services.
- **cloud/***: Core service modules that integrate backend functionality.
- **bot/***: Bot modules.

## Code Style And Practices
- **Kotlin**: Do not expose mutable collections in public APIs. Prefer `data class`, `sealed interface`/`sealed class`, and `suspend` + `CustomResult<T, ScenarioError>`. Use verb-object naming for functions.
- **Compose**: Name `@Composable` functions in PascalCase. The general lowercase function-naming checks exclude `composeApp` source trees because Compose uses a different naming convention.
- **Architecture**: UI -> `SessionManager` request extensions -> API endpoint. Add local storage and paging when appropriate.
- **GlobalDialog vs GlobalTask**: Use `GlobalDialog` for user-initiated actions where success or failure is important to the user (e.g., sign-in, publishing content, profile updates). Use `GlobalTask` for user-initiated actions where failure can be tolerated but the operation must not be cancelled by scrolling or page switching (e.g., adding reactions, toggling favorites). When implementing client UI handlers, consider adding `GlobalDialog` or `GlobalTask` as fields in the handler/controller class.
- **Paging**: Store cursors consistently in `RemoteKeyStorage` (`PRE_COLLECTION`/`NEXT_COLLECTION`).
- **Logging**: Instrument external network requests through `serviceCatching`. Avoid noisy logging in hot paths.
- **Tests**: Write tests whenever possible. Client-side non-UI tests go under `src/headlessTest/kotlin`. Compose tests should follow https://kotlinlang.org/docs/multiplatform/compose-test.html. End-to-end tests use `scripts/test_scripts/build-and-test.sh --e2e`, which runs the Appium runner modules' explicit `appiumTest` tasks and CLI E2E tasks; no Gradle property is required.

## Static Checks
- After code changes, compile the affected modules and platform source sets with scoped Gradle tasks. Include downstream consumers when changing shared or protocol types; see DEVELOPMENT.md's Build verification section for task selection.
- For repository-wide compilation validation, use the existing `compileAllNoRelease` task.
- Do not use root `assemble` as a routine compilation check: it also builds unrelated modules and release artifacts, including Wasm production linking and optimization. Run artifact/release tasks only when explicitly requested or needed to verify affected packaging, optimization, signing or distribution behavior.
- When an Android artifact is needed for ordinary development or tests, select the affected Debug variant; do not build Benchmark/Release variants by default.
- After compilation checks, run `./gradlew detekt --console=plain` for static code style checks.
  - On Windows, run this through Git Bash.
- Detekt limits production function names to 50 characters. Test source sets (`src/test` and `src/*Test`) are exempt from the maximum function-name length so test names can describe behavior completely. Preserve the separately configured naming-style checks and keep production function names concise.

## Tests
- Actively test affected modules.
    * For small changes, use `./gradlew :module:test --console=plain`.
    * For the full regular unit test suite, use `./gradlew check --console=plain` (excluding device-dependent Compose and Appium tests).
    * For end-to-end tests, use `./scripts/test_scripts/build-and-test.sh --e2e --plain`; this runs Appium and CLI E2E suites.
    * For Compose common tests under `device_based`, use a real device with `./gradlew :module:connectedAndroidTest` or run `./gradlew :module:jvmTest`.
    * For Compose UI changes, run `./gradlew validateDebugScreenshotTest` for snapshot tests.
- Keep Compose screenshot tests context-free: do not inject app runtime `CompositionLocal` dependencies such as session managers, dialog controllers, or navigation contexts. Cover components that require those dependencies in another test layer.
- Tests are meant to reveal problems. If a test finds a problem, fix the problem instead of working around it in the test.
- Do not add extra test steps into a test case unless there is an actual dependency between the steps.
- Extract repeated test steps into helper methods.
- Android Appium tests require a real device or emulator and cannot run concurrently on the same device. The caller must keep the device-side lock leased for the entire `--e2e` Appium sequence and refresh its expiry until release; `build-and-test.sh` does not acquire or release that lock.
- `build-and-test.sh` only orchestrates device/UI and end-to-end tests through `--android`, `--desktop`, `--compose`, and `--e2e`; run ordinary unit tests directly with Gradle.
- Appium sources and runners belong to the independent `*Appium` modules under `src/appiumTest/kotlin`. They depend on built or installed application artifacts, not application implementation code.
- Appium `appiumTest` tasks must disable Gradle up-to-date reuse so every explicit E2E invocation drives the target again.
- Appium tests pass target/platform helpers to the typed `AppiumTestBase` constructor. Shared test flows are base extension functions that read its helpers and JUnit test name; concrete test methods pass only scenario options. Target helpers create sessions and platform helpers own launch, cleanup, and log collection.
- Cloud server integration tests and Appium E2E tests use PostgreSQL with dedicated filesystem RPC and Lucene RPC containers. Do not reintroduce H2, MinIO, or Elasticsearch into these test topologies.
- Keep filesystem and Lucene RPC services business-independent. Translate business search models in backend adapters; clients download object contents directly from filesystem HTTP rather than through server.
- Define filesystem and Lucene RPC contracts in their respective `services/*-protocol` modules. Service runtime code must not depend on `backend/core`, `shared`, or `api`; map business storage/search types in backend adapters. Keep service configuration independent of flavor env files.
- Lucene RPC uses explicit named fields with independent indexing, storage and DocValues settings, not business JSON payloads. Do not inject fixed ID fields or expose ID-specific RPC operations; adapters declare business identifiers, queries and sort fields. Keep `Lucene*Document` conversion classes in backend adapters.
- Gradle owns test image construction: `distTar` -> `copyTestDockerDistribution` -> `buildTestDockerImage`, using the root Dockerfile. Testcontainers only starts the resulting images. Keep image tasks in the shared convention plugin, and use the configured Docker-compatible backend rather than a separate test Dockerfile.
- Do not reintroduce the removed H2 service or Docker target; `r2dbc-h2` supports only file and memory protocols, not the remote database topology required here.

## Additional AI Collaboration Rules
- Deploy through `scripts/service_scripts/start-service-in-docker.sh`; compile deployment artifacts inside Docker, not on the host.
- Make the smallest necessary changes. Prefer extending `client/core` and `api`; do not casually change public models.
- Do not create files outside the repository. Do not expose sensitive values from `deploy/*.env` in logs, documentation, or commits. Source-transfer archives must also exclude `dev/preset/secrets`, credential files and private-key contents; inspect the final archive before transfer and preserve Git executable modes.
- For changes involving the configuration cache, avoid reading external environment state during Gradle configuration.
- Configure shared Kotlin/JS and Wasm Yarn root extensions only in the root build; subprojects must not override lockfile locations. Keep JS and Wasm lockfiles separate under `kotlin-js-store` and regenerate the affected lockfile after npm dependency changes.
- Keep existing tests compatible. If tests must be updated, explain the migration reason and steps.
- Save rule-related knowledge learned while coding or scanning the project to this file.

- Android KMP device tests must include `commonTest` when exercising shared Compose tests and declare their test-only host activity in `src/androidDeviceTest/AndroidManifest.xml`. Keep the device awake during UI testing and restore temporary power settings before releasing its lease.

## Troubleshooting Quick Reference
- Network errors: check `client/core/*Request.kt` `serviceCatching` logs and `AdminApi`/`CustomApi` routes.
- Paging errors: check how `RemoteKeyStorage`/`RemoteKeyRoomStorage` saves and reads cursors.
- Data not persisted: check `*RoomStorage.save`/`observeData` implementations and `commonJson` serialization.

- Desktop Appium suites must run sequentially on one desktop. Windows uses DesktopDriver native UIA (`javaSwing=false`) and a JVM-local Preferences factory; do not rely on java.util.prefs.userRoot to isolate Windows test state. Do not reintroduce the removed AWT agent/bytecode compatibility patch. Set app.protected.storage.dir inside the test runtime directory; the Preferences factory does not isolate protected-value files.
- Linux Appium must enable session accessibility and query every AT-SPI provider owned by the launched PID. Element handles must retain the D-Bus connection and stable object path; object paths alone can collide across providers. When a target disappears during navigation, return a stale-element error and re-locate the control; never fall back to cached coordinates for a disappeared target. Text input must pass and resolve the saved object path directly; never reselect by result index or fall back to another editable control.
- Shared Wasm browser test configuration belongs to the wasm-browser-test-interop convention. A later useKarma call replaces the framework configuration, so do not override it in consuming modules; track loader files as task inputs and preserve the configuration cache.

- JUnit host test initialization bases must be abstract so test discovery does not run the base itself. Keep the two conflicting Detekt abstract-class rule exceptions scoped to PlatformHeadlessTest files.

- Diagnose remote service forwarding with target/host application probes and the installed transport's polling capacity. Listener binding or successful TCP connect alone does not prove forwarding; do not report SSH bridging as a fix for direct-forward failures.
- For intermittent connection-refused/reset failures, compare guest health with concurrent host connection probes. Serial health or passing focused reruns alone does not establish cross-test state interference; distinguish per-listener backlog from total polling capacity.

- When an external Docker backend uses gvproxy over loopback TCP, Keep Docker automatic publish ranges aligned with explicit gvproxy rules, use gvproxy DNS for guest/containers, and coordinate both processes during recovery. Confirm the shared VM is idle before migration/restart. Never use SSH networking bridges, diagnostic DLL replacement, added connection retries, skipped tests or relaxed timeouts as a permanent forwarding fix.

- Preserve Git executable modes when copying Windows sources into an isolated Linux test checkout; ordinary Windows archive metadata can remove launcher execution permissions.
- Keep Android test app shutdown and reverse-port replacement in the fixture order documented in DEVELOPMENT.md; noReset must not leave an old app requesting removed host targets.

- Run unattended Linux desktop E2E through the isolated display runner documented in DEVELOPMENT.md. Do not use a zero-size/disconnected Xwayland session or change render settings, retries, skips, or timeouts to hide an unresponsive accessibility tree.

- Resumable upload sessions require real SHA256 and CRC32C values. Keep generic file-operation quota locks separate; never fill required digest fields with dummy values. OSS compose owns backend-specific verification; follow DEVELOPMENT.md for MinIO's verified-source CRC32C combination and filesystem's streaming SHA256 check.

- RPC adapters acquire a valid connection once per business operation. Pin upload/download sequences to that proxy, preserve caller cancellation, and never automatically replay failed operations. Recovery tests must reuse the same adapter across independent service restarts.

- Authentication uses Redis-backed opaque Cookie sessions. Never restore revoked/expired sessions by silently
  signing in with saved keys, replay mutations, or renew activity from background polling/WS heartbeats.
  Keep user and panel session namespaces separate and use protected native Cookie storage.

- Persist GUI private keys/account history through the protected-value boundary. Do not read or migrate old
  Settings history; new records must verify encrypted writes before reporting success. Never
  downgrade to plaintext when an OS secret store is unavailable. Browser private-key encryption does not
  replace the HttpOnly Cookie boundary. Run Windows Detekt separately from compilation; use Gradle's default worker capacity.

- Provide sign-in storage through backend/core's SigninSessionServiceFactory registration. Keep driver
  dependencies and the Redis implementation in backend/redis; cloud/runtime owns only the Ktor adapter.

- Keep host E2E runtime JARs immutable for the entire test process. Finish compilation before UI execution; do not rebuild shared fixtures from another command while tests run in the same checkout.

- Keep host-test fork increases scoped to the reviewed allowlist in root build.gradle.kts. Preserve shared-device/desktop serialization. Test image contexts must be isolated per module and immutable during each build; keep source-build shared writable cache locks. Detekt 2.0.0-alpha.6 Worker API cannot configure its process heap and fails Compose analysis; keep it disabled until that upstream limitation is resolved.

- Before expanding host-test concurrency, verify temporary paths, ports and persistent namespaces are isolated. A module with a pure test and Docker/native-model integration tests is not a pure-test module; do not expand its entire test task without reviewing those fixtures. Inactive modules excluded by settings.gradle.kts do not need concurrency configuration.

- Compare build concurrency using the same explicit tasks and rerun/cache policy. Two-module compilation, six-module compilation and full assemble timings are different workloads and cannot establish a parallelism speedup or regression.

- Verify VM state with sufficient read permissions before concluding it is stopped; sandbox probes can report inaccessible services. When applying skill templates to a legacy idle guest, normalize generated Linux scripts to LF and preserve the default Docker publish pool while restoring disjoint outbound ports. Verify the installed environment tooling independently of project configuration.

- Create upload and chunk staging files atomically with unique names per request; client filenames and upload IDs are not unique across processes. Keep body reception inside the cleanup boundary. Real RPC shutdown waits invoked from runTest must use a real dispatcher so virtual time cannot expire before IO completes; preserve the existing timeout duration.

- Linux AT-SPI can append comma-separated badge suffixes to accessible labels. Match whole labels within the merged name, not arbitrary substrings. Authentication E2E needs secret-tool and an unlocked native keyring on the isolated test D-Bus session with isolated data directories.

- Keep E2E selector text in English and configure test locales explicitly. Restore temporary app-language overrides; preserve product translations. Capture Android app locales before clearing data, because pm clear also removes the override.

- Never pop the last root navigation entry. Dismiss modals through platform back/Escape; do not make an entire dialog surface clickable solely for tests, because the Wasm overlay can intercept child controls.

- Account-menu confirmations must replace the current modal instead of nesting another Dialog inside it. Keep sign-out request ownership outside the dismissed modal; clear protected active-session history only after server sign-out succeeds.

- Keep private deployment host aliases, addresses, usernames, absolute host paths and session-specific verification records out of tracked files, commit messages and public PR text. Record reusable findings with generic environment names and placeholders.

- Preserve raw ML-DSA/ML-KEM key bodies and ciphertext layout during crypto changes.
  JVM KEM uses Java 21 `javax.crypto.KEM`; Android uses BC `KeyGenerator` with
  `withNoKdf()` because its SDK lacks that API. Keep cross-platform compatibility
  coverage in shared headless tests; do not silently introduce KDF or DER encoding changes.

- Shared Android device tests reuse headless tests through the test source-set tree;
  configure `withDeviceTestBuilder { sourceSetTreeName = "test" }` and provide
  a device-specific abstract PlatformHeadlessTest actual without Robolectric.

- Read-only users retain authentication and read access. Enforce write restrictions for HTTP mutations and WebSocket topic creation; notification-room recipients remain invited, with disabled message input.

- Snapshot PKCS#12 Base64 is secret configuration. Preserve existing key bytes during migration, validate supplied credentials without a file fallback or replacement-key generation, and keep decoded Base64 key material in memory.

- Keep crypto tests self-contained with synthetic vectors or generated keys; missing external preset directories must not silently pass a test. Legacy CryptoJvm ECIES/CBC and shared P256 encryption use different ciphertext formats, so verify each format independently unless an explicit compatibility contract exists.

- Adaptive Home uses one section pager across rail/bottom navigation. Measure shared lists and search
  results against their own content constraints, and preserve mounted reading state across resizes.
  Keep Home context out of shared lists. Run `:app:composeApp:adaptiveUiTest` for isolated layout/state
  fixtures; regular host tests continue excluding device-based UI tests.

- On Windows, run assembly, Detekt and screenshot/host UI verification sequentially when they share
  Android runtime JARs. R8 and preview renderers can hold those files open during another build.

- Room activity is an API projection, not a RoomInfo schema extension. Filter joined membership before
  grouped unread pagination; apply activity cursors to the per-room maximum message ID, not individual
  message rows. Preserve unread-room versus unread-message count meanings and existing private-topic
  decryption. See DEVELOPMENT.md for the data contract and integration checks.

- Activity previews must remain ephemeral and session-keyed. Room lists/search return activity with
  the same page; never add a second viewport/batch fetch or persist decrypted previews. Preserve local
  RoomInfo caching and route successful pin/read changes through the event bus. Total unread messages
  come from the backend's complete joined-room aggregate, never a client sum of loaded pages.

- Keep a permanent latest-edge anchor before conditional room loading rows, so new messages remain
  visible at the latest edge while history keeps its message-key anchor.
- Update room read markers from the maximum visible topic item key, not LazyColumn indices or the
  first displayed row; paging spacers and loading rows occupy indices. New messages can replace the
  visible topic without changing the scroll index.
- Verify an interactive, unlocked foreground desktop before Windows Appium keyboard tests. An accessible
  native tree does not prove OS input is available; do not bypass empty-editor failures with fake content.
- Refresh the Home unread navigation badge on read/message/membership events and lifecycle resume;
  keep badge labels localized and test their presence/absence alongside row counts.
- Keep read-marker upserts monotonic and atomic; history navigation and late requests must not rewind
  the stored marker. Verify both exact counts and the legacy unread-room aggregate.
- Native room-cell accessibility must preserve merged visible content. Room names must contribute
  to merged native descriptions when previews supply descriptions; keep browser-only labels separate.
  Resize Windows native test windows through their verified native handle and convert viewport
  dimensions for window DPI.

- Browser Appium previews need explicit text/action bridge nodes; keep the bridge disabled outside test
  mode and preserve native merged accessibility. Select equal-stack overlay nodes in foreground paint
  order so dialog controls win over identically labelled background controls.
- Measure actual browser content dimensions after responsive chrome settles. Native Firefox compact
  windows may be limited to 500px; keep strict viewport/draft assertions and document the tested sizes.

- Collection refresh must clean the requested named collection, never its default detail cache. Verify
  selected-list deletion while preserving detail IDs/aliases and unrelated lists using real SQLite.
  Keep host-test database scopes unique and temporary storage under the module build directory.

- Browser semantics must publish changed labels even when geometry is unchanged.
- Wasm E2E runs every scenario on Chrome, Edge and Firefox sequentially with isolated profiles.
  Prefer installed browsers through Selenium Manager discovery; keep per-browser binary overrides
  separate and never reuse a personal browser profile or skip an unavailable matrix entry.
- Async SQLite transaction finalizers must complete under cancellation so Room can recycle its pool
  permit. Close allocated connections/statements when cancellation prevents delivery; new operations
  still honor cancellation, and cleanup must not replay business writes.

- Treat server paging tokens as opaque and preserve server item order. Keep database offset keys
  separate from network tokens. Refresh through the existing Pager/RemoteMediator, retain the loaded
  range and placeholder counts, and atomically replace room rows before publishing preview state.
  Anchor-preserving mapped PagingSources calculate refresh keys from previous-generation pages
  without relying on per-instance caches. Verify multi-page event refresh with shuffled IDs and opaque tokens.

- Fingerprint deployed Wasm entry scripts and require cache validation for HTML. During
  upgrade acceptance, compare browser-loaded Wasm filenames with the image; a successful
  fresh HTTP probe does not prove an existing tab has loaded the new client.

- Isolated deployment checkouts must reconnect existing filesystem/Lucene bind mounts
  before startup. Reload BunkerWeb upstream resolution after container replacement;
  container health alone cannot establish correct domain routing or data continuity.

- Version Compose web resource URLs as well as the entry script. Cached translation
  files paired with new generated byte offsets can display unrelated strings or corrupted text.

- Pin external Dockerfile base images and build frontends by verified manifest digest.
  Treat transient Docker Hub failures with bounded retries before changing build configuration.

- RTC ICE candidates must remain queued until their peer remote description is ready. Keep peer
  queues independent, and guard completion cleanup against replacing a newer connection for the same user.

- Ktor RTC JVM shutdown must detach sender tracks before closing the connection and disposing
  local media. Remote JVM tracks are borrowed; video renderers must remove sinks before
  track disposal. Keep native negotiation tests on a real dispatcher, including cancellation cleanup.
- Android Ktor remote tracks are owned by native receivers. Remove renderer sinks before
  PeerConnection disposal and avoid separately disposing those receiver-owned tracks.

- Normalize browser RTC capture `JsException` at the platform boundary; it is not an `Exception`.
  DOM failures can lack JavaScript stacks, so preserve their message without retaining an unsafe
  JS cause. Verify missing-device errors exit the joining state and cancellation remains cancellation.

- Verify compression for cold Wasm client loads before diagnosing a blank page as RTC failure.
  Browser session fixtures must populate storage on a static same-origin bootstrap document
  before opening the app, so setup never aborts its first Wasm download with a reload.

- Check Wasm runtime dependency resolution as well as compile classpaths. Ktor 3.6.0's browser
  adapter retains the Wrappers 2026.1.9 ABI; newer transitive wrappers can remove inlined
  symbols while compilation still succeeds. Preserve the recorder's newer URL API when resolving
  primitive ABI compatibility. Exercise real browser constraint conversion,
  synthetic track capture, SDP creation and cleanup when changing RTC dependencies.

- Browser ICE events can contain an empty candidate string before the final null event. Treat
  this as an end-of-candidates marker at the RTC boundary; native Ktor adapters reject it.
  Preserve failures for non-empty invalid candidates and verify media still flows after the marker.

- Shared RTC dispatch must distinguish a recipient channel cancellation from caller cancellation.
  Check the caller context before propagating cancellation; one closed WebSocket must not
  cancel the global signaling or negotiation jobs.

- Verify browser WebRTC presentation with requestVideoFrameCallback presentedFrames. Firefox can
  report zero VideoPlaybackQuality totalVideoFrames for a MediaStream while actual frames
  are being presented; do not infer a frozen stream from that counter alone.

- Adaptive form fields must emit one layout root per item, including their dividers. Keep editor draft
  and mode state outside responsive width branches; measure breakpoints after pane padding.

- Resolve direct-provider Koog model names only by exact catalog IDs through modelsById();
  reject unlisted IDs and never add manual aliases, model substitutions or metadata fallbacks.
  OpenRouter is a router exception: accept nonblank IDs through LLModel without catalog lookup
  or guessed token limits, and let the remote API validate model support.

- OpenAI-compatible endpoints share only the HTTP protocol. Treat model IDs as opaque strings;
  never resolve them through the OpenAI catalog or infer OpenAI model metadata.

- Pair GoogleModels with native GoogleLLMClient and a native API root; Google definitions lack
  OpenAI endpoint capabilities. Verify request execution, not only model lookup or client construction.

- Use native Bedrock Converse for schema-based generation. DeepSeek JSON-object mode must receive
  the response schema in its system prompt because the native adapter omits it from the API payload.

- Bedrock API keys must take priority over ambient AWS signing credentials. Let the runtime SDK
  construct and own its default credential chain so credential requests inherit the configured region
  and service shutdown closes the credential cache and HTTP engine. Exercise the production factory
  in authentication, region propagation and resource-lifecycle tests.

- Appium artifacts belong to the case's raw `AppiumTestReport` directory. Let Gradle clear task outputs,
  preserve the original test failure during collection, and register private runtime paths for cleanup
  before raw result metadata is written. Link artifacts through static HTML viewers so server MIME defaults cannot force
  downloads. See DEVELOPMENT.md for the report entry point and isolation contract.

- Filesystem/Lucene use SLF4J directly, without Napier or shared/business dependencies. Keep their Logback
  provider, container-owned `LOG_PATH` directories and Appium file-log archives aligned; validate
  service startup records rather than treating nonempty provider-warning output as collected logs.

- Appium 11 does not transitively provide Selenium support utilities. Declare selenium-support explicitly
  when using Selenium waits; Appium elements are AppiumWebElement, not RemoteWebElement. Treat a missing
  page source as a diagnostic collection failure instead of writing an empty artifact.

- Keep server/Appium/CLI Testcontainers topology in dev/testInfrastructure. Callers own Ktor test-host,
  platform forwarding and business assertions; use explicit host versus container environments.
  Register resources before startup, export logs before Testcontainers removes failed containers,
  and preserve the original failure while completing cancellation-safe cleanup.

- Normalize Linux test-image shell scripts to LF in copyTestDockerDistribution, preserving source
  line endings so Windows checkouts cannot break container entrypoints. Track the filtering policy
  as a task input so existing Sync contexts are regenerated when the policy changes.

- JVM coroutine stack-trace recovery can copy exceptions across dispatchers; verify the original
  failure through its cause chain and check that cleanup failures remain suppressed on it.

- Initialize Edge with a browser-level DevTools blank target and close its isolated startup tabs
  before page-scoped WebDriver commands, which can wait on the external MSN new-tab navigation.
  A bare about:blank arg is parsed as a switch. Verify one blank target on fresh launch and restart;
  preserve normal browser windows, viewport checks and page-load timeouts.
- Current Windows Chrome can enforce a 504px content minimum. Browser activity resize tests use that explicit compact width across the matrix and retain exact viewport/draft assertions; see DEVELOPMENT.md.

- Keep raw Appium/CLI/server artifacts under build/test-results/<task>/artifacts/<class>/<test>;
  Appium adds its platform. The test-artifact-reports convention clears raw files before Test executes
  and finalizes each task with one reports/<task>/index.html. Do not scan or render reports inside cases.
  Keep report generation independent of live fixtures and serialize independent builds sharing outputs.
  Do not add hashes, counters, random suffixes or container startup retries. See DEVELOPMENT.md.

- E2E read completion must identify the expected persisted message as well as zero unread;
  optimistic local echoes can precede server persistence.

- Close the panel modal navigation drawer when authentication is cleared so sign-out or
  revocation exposes the sign-in page rather than leaving an anonymous navigation overlay. Keep
  its account actions outside the scrolling navigation list so compact windows expose sign-out.

- Prepare authentication/socket E2E community and room fixtures before application launch;
  cached pagers need not observe setup writes performed through another client after Home mounts.

- Desktop window discovery/focus must use remote Appium interfaces. Keep process/thread ownership, DPI-aware resizing and physical-pixel screenshot capture in dev/appium-windows-plugin on the Appium host; do not restore host User32/JNA calls. Remote diagnostics must never attach to a guest PID on the host. Keep environment-provider installation and private connection profiles outside the project. Project JSON declares artifact/service roles only; the external environment runner owns staging, forwards and private-session cleanup. Collect native page source and screenshots through Appium; keep test methods free of SSH/VM transport code. See DEVELOPMENT.md for the environment contract.

- Work VMs are test/runtime environments. Unless the user explicitly requests guest compilation, build applications, test code and dependencies on the host or the existing designated build environment, then stage prebuilt artifacts. Installing Appium/VMs or requesting E2E tests does not authorize guest builds, including native dependency install hooks. Do not silently fall back to guest compilation or enlarge VM resources for it.

- Run macOS desktop E2E only on a qualified hardware renderer. Do not restore local Skiko class replacements/frame caps or treat historical patched-runtime reports as current acceptance. Keep the original test budgets and assertions. See DEVELOPMENT.md#macos-e2e-pause-and-skiko-investigation for evidence and qualification criteria. Verify visible navigation, edited text, dialogs and resizing against native accessibility state; a rendered first frame, Metal library load or successful accessibility action alone is insufficient. Keep provider qualification outside the project and do not add unconditional bypass flags.
- Mac2 coordinate clicks must wait for visible, enabled controls with stable bounds during Compose navigation. For text containers, prefer their own uncovered area over a center point inside a nested interactive control; keep this geometry covered by unit tests.
- Desktop WebView requires the pinned Nucleus Tao runtime and native window entry points. Do not restore the removed Swing/Wry transform or embed SwingPanel in a Tao window. Keep video native lifetime in DesktopVideoHost and copied-frame rendering in the Compose adapter. Observe asynchronous sign-out completion before testing restart persistence; do not extend UI timeouts or remove post-restart assertions.

- Linux desktop E2E requires graphical Tao/X11 and AT-SPI runtime libraries. Window geometry commands must target the launched PID and reject ambiguous visible windows, never the focused desktop window. Preserve native screenshot failures as collection errors; see DEVELOPMENT.md for driver dependencies.

- Tao desktop windows must bind Escape to the composition's NavigationEventDispatcher with a lifecycle-owned NavigationEventInput, matching Compose's AWT mediator. Preserve overlay priority and disabled root navigation; do not replace dialog dismissal with test-only UI controls.
- Merged clickable activity/room cards must retain message previews and unread counts in their accessible description. Native providers may expose only the first merged contentDescription; provide a complete row description and match whole label parts in automation.

- Windows and Linux desktop E2E share the quoted JVM argument-file launch contract. Keep Linux Appium arguments as an array and honor its working directory. Remote environment runners translate paths and provide prebuilt target JDK/native dependencies; keep Gradle tests and backend fixtures on the host and provider connections outside the project.

- Declare desktop runtime dependencies and optional project setup hooks in dev/test-environments/desktop-requirements.json. Keep VM hardware, credentials and provider launch commands outside the project. Host preparation may build; guest initialization hooks remain limited to runtime configuration and prebuilt dependency installation.

- Return `CustomResult<T, ScenarioError>` with a scenario-owned sealed error interface. Convert errors explicitly
  when crossing scenarios; `FailureDetails` is only a diagnostic/terminal-consumer contract, never a business return type.
  Capture exceptions at explicit boundaries, propagate cancellation, and do not restore the removed Kotlin Result helpers.
- Endpoint server bindings are owned by cloud/runtime's `EndpointResultRouting.kt`; use its `invoke` extension
  so typed failures stay intact without passing through endpoint4k's Kotlin Result-based invocation overloads.

- Preserve typed failure categories through `fromError` conversions; do not infer authentication or permission
  failures solely from the diagnostic exception class. PDF tests use installed fonts and public HarfBuzz core/subset libraries (8.3.1+, 13.0+ for CFF2); never bundle PDF font binaries or register test fonts.

- PDF font selection prefers static weight faces; pin all remaining VF axes explicitly before OpenPDF embedding. Cache font bytes only and create document-owned OpenPDF objects. Discover installed SFNT/TTC metadata without loading unused font contents; preserve collection face indices. See DEVELOPMENT.md for installation and native-library setup.

- Read backup variable-font data only when a missing static weight needs it; unused fallback IO failures must not block available static faces.

- Keep verification worktrees outside directories named `build`; Detekt excludes source files by absolute `/build/` path.

- Convert pinned CFF2 instances to CFF1 with the public HarfBuzz downgrade flag before OpenPDF embedding; preserve subset flags and reject residual CFF2/variation tables.

- Native cancellation tests must synchronize from inside the real C call, keep callbacks reachable until face destruction, release gates in finally, and join cleanup on a real dispatcher. Distinguish a thrown cancellation from caller Job cancellation of an uninterruptible synchronous conversion.

- Multi-axis font regressions must hold other axes fixed and verify each requested axis through independent observable output; compare real outlines/metrics or reliable weight metadata, not only outputs from the same converter.

- PDF syntax highlights may overlap and leave an unhighlighted suffix; emit each source character once and retain the tail. Check rendered pages as well as extracted text after layout changes.

- Keep snapshot timestamp rendering independent of the host default timezone; PdfGenerationSpec carries UTC calendar values. Label the displayed UTC offset explicitly.

- Keep FunctionSignature.functionBodyExpressionWrapping=multiline while MultilineExpressionWrapping is active, so the two rules agree on multiline function bodies.

- Wasm media experiments must distinguish Host frame dispatch from actual Compose rendering
  and physical sound/display output. Keep synthetic fixtures and session metrics under build;
  do not infer a shared Skiko GPU context from snapshot API docs or replace the copy path
  with a DOM media/canvas overlay. See DEVELOPMENT.md for ownership and verification.

- Track local Wasm npm module implementation files as npm-install task inputs and refresh
  Yarn's copied file dependencies when those inputs change; package.json alone is insufficient.
  Verify the installed module and bundled Worker, not just Kotlin compilation.

- If screenshot reference PNGs are Git LFS pointers in an isolated worktree, restore the
  original LFS objects before validation. Never regenerate golden images to mask missing
  reference content.

- Match Skiko source/API investigation to the version actually resolved for Wasm (including
  the imported npm runtime metadata), not an arbitrary version found first in the Gradle cache.

- Wasm texture experiments use the source-built Skiko patch in dev/skikoMediaRuntime;
  keep its Kotlin API and published native runtime at the same verified upstream version.
  Never create a second DirectContext over Compose's WebGL context or transfer an adopted
  texture back to JavaScript ownership. Verify same-canvas uploads, GL state restoration,
  resize/context loss, and zero live frame/texture resources after disposal.

- dev/skikoMediaRuntime/patches contains the active 0.150.2 source overlays;
  upstream-patches contains unapplied forward-port candidates. Keep LICENSE/NOTICE
  and source pins with the local build; removing a single upstream workaround does
  not authorize dropping the renderer-context/lifecycle integration. See DEVELOPMENT.md.

- Emscripten external-texture registration must update both GL.textures[id] and
  texture.name; the latter is used by native glGet binding queries. Reset both on
  unregister and verify positive native handles while the texture is alive.

- When source-built Skiko invalidates several Wasm release links, diagnose compiler
  heap/GC pressure before treating a long build as a media defect. Bound Gradle workers
  for validation when concurrent links exhaust the shared Kotlin compiler heap; do not
  stop shared daemons, skip checks, or infer a speedup from different task workloads.

- Current KGP Wasm development and production graphs share the generated npm `kotlin`
  directory. Run their distribution tasks in separate Gradle invocations and test the
  copied immutable distribution, not the mutable package staging directory.

- Diagnose media cadence with source frame IDs and separate decoded/delivered, imported,
  submitted-draw and observed-canvas counts. A 30fps fixture cannot establish headroom;
  validate the actual drawing bounds and keep observer readback/CPU overhead explicit.
- Guarded uploads of fresh textures restore GL state exactly; do not add unconditional
  flush/full-context reset per frame, or reuse this exception to mutate adopted textures.
- Cull samples against the media clock before expensive raster conversion/readback and
  retain the post-transfer lateness check. Close skipped samples; never relax the budget
  or let repeated processing of expired frames build an unrecoverable live-playback debt.

- Normalize nullish framebuffer arguments in WebGL diagnostics: Emscripten can pass
  undefined for the default target. Verify tracked targets against actual bindings
  before attributing media draws to offscreen layers or the final canvas.

- Headed media cadence checks require foreground ownership and calibrated physical video bounds;
  a large emulated viewport alone does not prove on-screen visibility. Keep desktop-readback
  and tracing-off controls because observers can perturb captureStream.
- Correlate source frame IDs with AnimationFrame presentation feedback and desktop observations.
  Shared presentation timestamps and NO_UPDATE_DESIRED reporters are not independent video
  presentations or dropped video frames. Preserve 64-bit trace IDs without numeric rounding.
- Some Chromium builds return an empty Tracing.getCategories list. Select explicit traceConfig
  categories and validate GPU/compositor events; an empty filter can enable unrelated tracing.
  Namespace trace artifacts by the output dataset so probe runs cannot overwrite study evidence.

- When frames disappear before texture upload, trace VideoFrame.close call sites with an
  uploaded-frame WeakSet. Distinguish pending replacement from Host tick closures; do not
  infer a late-frame drop solely from an outer tick entry in the stack.

- Preserve GPU frame ownership under backpressure: a rejected offer remains Host-owned; only generation cancellation may supersede a pending renderer frame. Keep UI acceptance, canvas capture and desktop observation counters distinct; see DEVELOPMENT.md.

- Distinguish GPU surface canvases from PictureRecorder/RenderNode canvases when evaluating Canvas.recordingContext; direct SkiaLayer success does not establish Compose interop. When inspecting Wasm imports, skip accessor properties: enumerating GOT getters can mark weak imports required and cause diagnostic-only linker failures.

- Use captureStream() without an explicit request-rate timer for canvas frame-ID observation, and record the capture policy. A high timer rate is not a guarantee that every video update is sampled; never compare historical timer-gated datasets as if their observation policy were unchanged.

- Verify media network-failure behavior against the pinned decoder API, not only a mocked Worker. Preserve skipped-sample counters through EOF responses without counting EOF as a frame; exercise both RGBA and GPU wire paths.

- Keep Web Audio scheduling/starvation on render time and video/progress/end detection on output time. Output timestamp extrapolation must stop at suspended render progress and reset on seek; test reported queue latency without treating analyser onset as speaker output.

- Route Wasm RGBA bitmap-import failures through Host cleanup. Rethrow cancellation and check coroutine activity before reporting errors from a superseded effect; verify preview/playback failures leave Compose responsive and release media resources.

- Give published native media bitmaps a stable lifecycle owner; per-value DisposableEffect cleanup misses publications superseded before commit. Retain displayed/pending frames until commit, and verify explicit native cleanup with GC finalizers deferred during remove/source-replacement races.

- Android Kover coverage can use pre-DEX offline instrumentation with the matching offline runtime; do not infer runtime incompatibility from missing Gradle device-test integration. Use D8 compatible with the project Kotlin metadata, export from the app process before force-stop, and report against the matching original class files. Keep minimal probes distinct from full APK/E2E support.

- Measure Kotlin/Wasm coverage in the Wasm module, not only its JavaScript loader. Preserve WasmGC/EH and the browser-supported output features during binary instrumentation; enabling every Binaryen feature can introduce unsupported output encodings. Export counters before navigation/restart/quit, keep source-map/build identity and linked-code denominator explicit, and distinguish fixed-fixture probes from general Kotlin line/branch coverage.

- Offline E2E coverage is opt-in through `-Pe2e.coverage=true`. Finish all selected
  application/classpath/image preparation before live tests, snapshot before teardown
  and restart, and preserve collection failures. Keep JVM/Android bytecode variants
  separate; Wasm reports mapped linked source lines only. Match original bytecode,
  source fingerprints and browser build identities before merging artifacts.
- Kover 0.9.11's single-class Features instrumentation wrapper can assert on engine-
  filtered generated classes; use the official batch instrumentation API rather than
  swallowing that exception. Wasm rewrites invalidate sourceMappingURL/DWARF offsets;
  remove those stale custom sections from covered binaries.
- Keep Windows coverage launchers below cmd.exe's command-length limit using an
  ordered manifest classpath JAR. Keep native graphics bundles consistent; an
  unrelated EGL DLL on PATH can crash Tao before a coverage snapshot is possible.
- Desktop coverage must select the current launch PID after every restart. A driver
  force-quit can leave its old running marker behind; retain previously exported
  snapshots without requesting those retired processes again.

- Combined unit/E2E coverage unions repository source paths and executable lines;
  retain independent platform metrics. Run ordinary Kover tasks before immutable
  E2E preparation, and reject ambiguous source ownership instead of guessing.
- Coverage-enabled builds must still refresh ordinary backend test images. Keep
  latest and e2e-coverage image contexts separate so host tests never consume stale
  normal images while only instrumented images are rebuilt.
- Keep original CFF2 test fonts, OFL licenses and pinned checksums under openpdf test
  resources; generated subsets stay in memory/build outputs. OS-backed session
  storage tests belong to JVM/device source sets, not a fake Android host keystore.

- Wasm source maps can assign synthetic operators to an empty EOF line. Exclude
  anchors beyond physical source lines and retain unmapped barriers; never inherit
  the previous executable line across those ranges.

- Use Yarn lock Provider properties in the root build and explicit Gradle create/register
  APIs. Attribute remaining deprecations with --warning-mode all --stacktrace before
  claiming Gradle compatibility; preserve separate JS/Wasm lock locations.
- Panel view-model factories require an owner-provided scope; do not restore GlobalScope
  or opt into delicate APIs to hide missing lifecycle ownership.

- When inline-return raw strings make Indentation and StringTemplateIndent disagree,
  use an indented local value before returning it and verify the rendered content;
  do not alternate formatting or suppress either rule.
- API relations belong directly on scenario-specific wrappers around base models. Use TopicCreated
  for creation/WS, TopicReplyPreview for nested replies, TopicActivity for activity previews, and
  TopicDetail for full queries. Pin responses update the nested base without discarding relations.
  During alpha, do not add client database compatibility migrations for retired projection formats.
- Pin/unpin events carry identifiers and the returned pin flag only. Patch each current cached
  projection without persisting UI summaries or replacing unrelated fields.

- Track test contracts and platform boundaries in DEVELOPMENT.md separately from executed acceptance.
  Keep activity read/send, navigation, layout and draft-resize cases independently discoverable;
  restore temporary Android screen overrides after the dedicated wide-context case.

- Browser bridge callbacks can leave keyboard focus on the document body. Focus the real Compose
  canvas before sending Escape and verify modal dismissal; do not add a clickable dialog surface.

- Drive browser action bridges with real WebDriver pointer clicks. Synthetic JavaScript clicks do not
  provide user activation and can cancel Compose effects on Chromium. Focus the renderer before Escape.

- Browser injected-session readiness must wait for restored user info, not only account-instance
  creation. User info changes avatar/create placement. Wait for committed, stable semantics geometry
  before resolving pointer targets; preserve normal timeouts and do not retry actions.

- CLI terminal event delivery must preserve pasted input under backpressure. Keep the pinned,
  archive-verified Mosaic source rebuild until an upstream release passes the real multiline
  login and command tests; do not hide dropped events with input delays.
- Browser RTC capture owns every track in the returned stream until delivery. Disable unused
  media kinds and stop surplus or late tracks after cancellation; verify presented frames
  and track release independently of signaling connectivity.
- Batch member insertion must preserve each member's joinedTime. Invited notification
  recipients remain INVITED with no joined timestamp; joined creators retain their timestamp.
- Recommendation fixtures need another author's topic in a joined community. A user's own
  topics are deliberately excluded; wait for search visibility before driving the CLI list.
- RTC acquisition under NonCancellable must retain the caller context and check it between
  audio/video acquisition and before delivery. Returning on the same browser dispatcher does
  not perform the cancellation check that a dispatcher switch would perform.
- Browser test bridges must use clipped window bounds for controls inside scrolling panes;
  viewport bounds alone can place an offscreen menu target over a pinned action.
- CachedLoadingHandler requires onSave to publish into its observed data flow. Use
  SimpleLoadingHandler for ephemeral results instead of a null flow and an empty saver.

- Wasm test semantics must detach HTML controls when Compose nodes become unplaced, including
  retained lazy-list cache entries. Let real WebDriver wheel input reach the renderer beneath transparent test controls;
  match an unobscured interactive target and resolve editors independently from search fields.
- Browser uploaders belong to the account scope. Retain selected files only for pending work,
  release them after success or abort, and do not claim path-based resume after page reload.
  File-input fixtures must use the real FileKit change handler and shared upload pipeline.

- Room renames must refresh room and joined-member search documents; exclude invited or departed
  memberships and paginate member/user joins using the query's user-ID cursor. Search refresh
  assertions must use current indexed results and a fully visible row as the scroll anchor.

- Replace changed Lucene documents atomically with an adapter-declared query; plain append keeps
  stale keyword matches. Keep the service operation generic and roll back failed index writes.
- Own Ktor test applications explicitly and stop them on failures before container teardown.
  Preserve the original error and suppressed cleanup failures so one failed case cannot leave
  a signaling listener consuming a later case's frames.

- Browser E2E fetch/media probes must preserve the page's native Promise realm. Firefox WebDriver
  sandbox async functions can produce incompatible Promise values for Wasm casts; chain native results
  or construct page Promises. Preserve actual responses, AbortSignal and capture ownership.
- Wait for successful form exit before navigating away after submission. A modal hiding the submit
  control does not prove completion. Restart checks that expect authentication must await loaded user
  data and clear stale test readiness markers before restarting the process.

- Creation events must update the named collection observed by the visible list as well as its detail
  cache. Verify creation from an already-loaded empty collection without a manual refresh; submission
  completion alone does not prove cached lists receive the new record.

- Cross-platform E2E validation must assert visible localized results or actual derived values.
  Browser-only Appium bridge descriptions are not native selectors. Preserve displayed account
  addresses in native accessibility instead of replacing their values with machine labels.

- Linux protected values use libsecret binary APIs, not secret-tool stdin (which truncates at 8 KiB).
  Keep native calls on IO with per-operation GCancellable ownership and cancellation-safe argument
  lifetime. Preserve raw stored bytes and stable production names; scope explicit test storage roots
  on Linux as on macOS. Cover large Unicode values and unavailable libraries against a real keyring.

- Resolve uploaded attachment MIME types from FileInfo; keep Markdown object blocks free of duplicate
  contentType metadata and preserve legacy unknown-field decoding. Generate object JSON through the
  serializer so file names containing quotes remain valid.
- Keep License.name in generated AboutLibraries metadata; omitting it makes the runtime parser return
  an empty library list. Verify bundled dependencies and their license associations after export changes.
- Dependency removal must cover every consuming module and unused version-catalog entries. Regenerate
  bundled license metadata and verify the affected runtime classpaths before reporting cleanup complete.
- Native keychain failure tests must use a private temporary keychain and restore the original default,
  search list and interaction policy. Keep these OS-specific tests explicit and serialize account access.

- Native uploaded-file rows must merge the filename and visible metadata into their actionable
  accessibility label. Test file selectors match the complete filename boundary, including escaped
  punctuation, rather than an arbitrary substring. Refuse clicks on zero-size native elements.
- Mac2 text input must use native typeText for the complete string and verify the exact multiline
  value. Reserve the key-chord API for shortcuts; per-character XCTest key operations can overrun
  the transport timeout even after the visible text is complete.
- AppKit exposes Save/Cancel controls in both the file dialog and Touch Bar. Scope native picker
  actions to the save-panel accessibility identifier and its buttons, then verify panel dismissal.
- Collect native fatal-error dialogs before cleanup. Close only newly created reporters belonging
  to the test launch, rechecking process identity; refuse unexplained pre-existing dialogs.
- Browser PDF preview owners must abort pending requests and revoke their Blob URLs on disposal.
  Verify actual document bytes and rendered pages under the application's cross-origin isolation.

- Bind ZoomImage main-thread checks to the actual desktop window UI thread. Its Swing default
  does not recognize the native Tao event loop; retain rejection of worker-thread access.

- Browser object reads need GET/HEAD CORS preflights and readable missing-object responses;
  keep mutation preflights denied and RPC routes outside this public read boundary.
- Browser audio acceptance requires a working default output device. A trusted click and allowed
  autoplay do not prove that audio hardware is available; keep real playback-progress assertions
  and diagnose zero output channels separately from application input or decoding failures.

- Windows native file dialogs may run on a dedicated STA thread. Validate window ownership by the launched application process and visibility; do not require the main window thread ID when selecting owned foreground dialogs.

- UiAutomator2 selector string arguments unescape quotes only. Preserve regex backslashes instead of JSON-escaping the expression; test matching after selector argument parsing, including merged filename metadata. Close the Appium driver before collecting long Android bugreports.

- Reveal native player controls only when their accessibility state says they are hidden; an unconditional surface tap can hide an already-visible controller. Preserve platform default control visibility timing rather than shortening it below normal touch/accessibility interaction time.

- Native Media3 progress updates can keep UiAutomator busy until its controls disappear. Disable UiAutomator idle waiting only during animated media interaction, restore the prior setting with cancellation/failure-safe cleanup, and preserve explicit state assertions and deadlines.

- Android native clicks wait for visible, enabled controls with stable bounds before issuing one click. Presence alone does not establish that a navigation drawer is ready for input; do not retry the click or alter animation settings to hide a missed action.

- Keep Android PlayerView inside its page component; do not introduce an activity-root video overlay to eliminate transient Surface black frames. Brief black frames during navigation/PiP transitions are accepted. Detach the controller on final view release. Preserve PiP E2E checks for decoded frames after transitions settle, including re-entry; do not accept persistent black output or replace pixel assertions with system state alone.

- Linux/X11 portal dialogs require a live parent XID and may belong to another process. Verify native transient ownership before admitting their AT-SPI nodes; reject ambiguous/shared portal windows, retain D-Bus/object-path identities, and keep portal activation on the same isolated accessibility bus as the test application.

- Filter `tasks.names` before obtaining individual `tasks.named` providers for aggregate dependencies.
  Keep excluded variants unrealized; see DEVELOPMENT.md for the AGP Compose Preview constraint.

- Browser protected-storage regressions must call the compiled Wasm production store. Use the isolated browser runner for per-case origin storage isolation; never extract JavaScript from Kotlin source strings.
- Karma Edge launches on Windows must disable compatibility-shim relaunch so the launcher can own and terminate the browser. Reject missing, skipped or duplicate cases in isolated browser contract runs.
- Wasm browser-test helpers should pass arguments through typed external interfaces; parameters used only inside js string literals trigger Detekt UnusedParameter findings.

- Wasm media verify suites use node:test; preserve independent child-case reporting and dependent playback/recovery flows. Run the browser matrix through wasm-media-host npm test:browser; keep benchmarks standalone.

- Wasm media reports write to isolated raw run/batch/suite directories first; collect completed runs into immutable build/reports/wasm-media snapshots only after all child processes close. Never write live reports into a shared collected directory.

- Wasm verification fixtures must bound diagnostics and native close operations separately. Defer cleanup errors until the case outcome is known; preserve original failures, record cleanup errors separately, and fail cleanup-only errors. Register every acquired site/driver/page/browser before subsequent setup can fail.

- Browser verification must own native termination capabilities, not just race close promises against timers. Use the supervised suite entry for owned-container fallback, preserve independent trace-finalization budgets, and verify forced cleanup with active child processes while preserving unrelated processes.

- Every browser verify module must await its root runCase. The worker completion notification marks the end of that awaited flow; do not replace it with unawaited top-level test registrations.

- Verification completion requires an empty owned Job/subreaper container, confirmed root exit and bounded pipe closure. Keep the container owner alive across root exit; never infer descendant release from PID exit or process-group kill. Test detached descendants and root-first exits with inherited pipes.

- Verification JUnit must record abnormal worker exits as lifecycle failures even when reporter output is complete; retain completed test cases alongside the process failure.

- Shell tool regressions use `python scripts/test_scripts/run-shell-tests.py`; keep each independent contract/parameter row discoverable and reportable. Execute real scripts in per-case fixture repositories, parse argv/YAML precisely, and never write or glob-clean the working checkout's deployment env files. See DEVELOPMENT.md for dependencies and report paths.

- Shell regression argv transport must preserve the production shebang/explicit interpreter. Share `scripts/tool_scripts/process_container.py` with browser verification; require empty-container and owner-exit acknowledgement before fixture removal, and sanitize XML only at the report serialization boundary while retaining raw diagnostics.

- Track fixture release with a per-command process lease registered before launch, not by catching selected exception types. Require all leases to be released before deletion, including when caller failure and cleanup failure occur together. Verify Shell compatibility entries as original bytes/executables and keep their LF rules in `.gitattributes`.

- Finalize process termination and control-pipe closure under one exception-priority boundary. Attempt all cleanup steps, preserve the original caller failure, retain every cleanup error, and test real disconnected buffered pipes rather than only BytesIO stubs.

- Bind Wasm page stacks through the shared browser-history controller, using root-scoped nested IDs.
  App Back must traverse existing History API entries so browser Forward remains usable; preserve the
  last root route, current URL, and unrelated history state. Keep modal dismissal in the platform
  dispatcher and keep drafts/credentials out of navigation history.
  Use discriminator-safe route encoding because destination payloads can contain a `type` field.

- Parent navigation return positions must follow the latest descendant snapshot. Keep responsive
  file navigation and its pager mounted across rail/bottom layouts; browser restores must not be
  echoed back as new pager navigation. Verify resizing and history restoration during a drag.

- Gate authenticated file previews before creating their detail ViewModels, including restored routes.
  Navigation actions must check the current session at click time so sign-in and sign-out take effect.
  Protected-action prompts share one sign-in confirmation; explicit sign-in buttons navigate directly.

- Compose blocking-font preloading can silently fall back after decoding errors. Strictly decode browser-local
  font bytes before accepting preload completion, and invalidate pending confirmation when permission resets.

- Wasm font preference is intent, not permission. Mount business content only after explicit opt-out
  or successful authorized font preloading. Keep the startup gate readable with native HTML fonts.
  Prepare ordinary browser E2E font preferences on the static same-origin bootstrap after clearing
  storage; font-flow tests must exercise the gate itself without a test-mode bypass.

- Build production Wasm distributions and Appium development distributions in separate Gradle
  invocations; their CompileSync tasks share generated npm package directories.

- Community fonts belong to topic/editor content, not the enclosing application theme. Reuse native
  Downloader disk caches; Wasm uploaded-font caching is separate from system-font permission access.
  Community settings expose content/code fonts only; leave missing-glyph fallback to the platform.
  Key uploaded-font caches by object version rather than expiring signed URLs. Verify real glyph metrics
  because resolver preload may silently resolve an invalid font to a default.
- Download records are unique per file. Use file identity rather than second-resolution timestamps for
  their IDs, so simultaneous downloads cannot replace one another's records.
- Publish download PROCESSED only after file writers close and complete extraction output is committed.
  Observe persisted completion instead of adding a second download-completion API. Keep platform storage
  behind DownloadFileStore and serialize account/file transfer, extraction and reads; browser Web Locks
  must also cover other tabs/workers. Preserve legacy record IDs when updating existing files.
- Font cache fills and evictions must not trigger presentation reloads. Publish successful explicit
  retries as per-file data updates, and retain displayed families independently of cache residency.
  Expose per-file font flows to content; cancelling a subscriber must not cancel account-owned shared reads.

- Wasm Default dispatching stays on the browser event loop. Keep ZIP processing in the dedicated
  file worker, pass OPFS paths and in-memory file metadata rather than Kotlin/Skia objects, and await worker
  cancellation acknowledgement before deleting unpublished output or releasing download locks.
- Windows Edge Karma runs must prevent compatibility-layer relaunch so Karma owns the actual
  browser process; otherwise launcher retries can delete a still-running browser's OPFS profile.
  Keep browser launcher setup in the shared Wasm test convention and track configuration inputs.
- Browser Worker cancellation tests must synchronize at observed native OPFS phases (a pending
  Promise or a completed write), rather than before Worker startup. Keep instrumentation in test-only
  entries, reuse production protocol/cleanup, and deliver cancellation before releasing a paused write.
  Retain page-initiated cancellation coverage alongside worker-local cancellation at the pending phase.
- Download task ownership must include queued requests and pause publication. Pause invalidates
  earlier admissions and waits for cleanup before later work runs. Merge repeated resume intents
  without dropping an explicit resume behind an ordinary registration. Handle ordinary failures at
  the owned request boundary; distinguish explicit operation pause from caller/account cancellation.

- Font preset ZIPs must contain a single family. Preserve available regular/bold/italic faces,
  exclude Condensed, and use sourceName to share raw downloads across separately filtered outputs.
  Recheck author-controlled Markdown weight support before removing or restoring extra weights.

- Keep font fixture builders in test source sets. Exercise ZIP loading through the production
  Downloader/storage/worker pipeline instead of retaining a separate production decoder only for tests.

- Community font caches own built FontFamily/resolver objects; native platforms load local files.
  Keep Wasm framework-retained bytes distinct from a separate source-byte cache. Extraction completion
  belongs to the download record under the account/file lock; never add a second extraction manifest.
  Keep native backing files available for resolution through another presentation resolver; owned-cache
  eviction must not close framework resources still referenced by content.

- Validate community font bundles before publication on every platform: reject duplicate weight/style pairs,
  compare filename styles with original OS/2 metadata, and compare typographic family (name ID 16,
  otherwise ID 1). Never compare style names or overridden platform attributes, deduplicate, or skip failed faces.
