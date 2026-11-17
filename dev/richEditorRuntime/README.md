# Rich Editor Markdown compatibility

This included build compiles the exact published `com.mohamedrejeb.richeditor:richeditor-compose:1.2.1`
sources. Sources are downloaded and verified only during task execution, then extracted below `build/upstream`;
the upstream library is not vendored into tracked Kotlin source. The Apache 2.0 license is retained in `LICENSE`.
The release source archives contain no NOTICE file; local modification attribution is in `NOTICE`.

The only source transformation extends the existing Markdown text case to include `GFMTokenTypes.EQUALS`
and the complete `GFMElementTypes.INLINE_MATH` source. Otherwise an equals leaf contributes no text and inline
math loses its dollar delimiters. Formula source remains editable text, with its delimiters, rather than being
reclassified as an inline code span. The upstream rich editor and serializer, including bold, italic, code,
undo/history and platform clipboard implementations, are retained.

Official coordinates and dependencies were checked against Maven Central 1.2.1 Gradle module metadata.
Compose runtime/foundation/material/material3 and Markdown use the existing workspace catalog versions
(1.13.0-beta02 and Markdown 0.7.18), matching the consuming application. The latter exposes the real
`GFMTokenTypes.EQUALS` token, verified by parser-node identity; no token-name string comparisons are used.
Ksoup remains the upstream published 0.6.0 dependency (the only version present in the current resolution cache).
Common/iOS, Android, desktop and Wasm source archive hashes are explicit task inputs in `build.gradle.kts`.
The 88 common source files are byte-identical across those published source archives; each platform contributes
its own four actual implementation files. No platform stub replaces the original implementations.
The build uses the workspace Kotlin/Compose plugins and JDK/JVM 21. iOS follows the existing `target.ios=true` gate.
Root composite substitution applies only to this library's root and Android/desktop/Wasm artifacts.

After any upstream upgrade, review the archive hashes and exact traversal anchor before changing either.
Run the included build's `desktopTest` and `wasmJsBrowserTest`, the application's
`device_based.TopicComposePageBlockEditorTest`, and affected platform compile tasks. Keep all existing assertions.

The application also compares structural rich documents before emitting edits: initial import, focus and selection
must not rewrite untouched Markdown, while text and style mutations still propagate. This boundary is necessary
independently of the parser fix because Markdown serializers may normalize valid syntax and whitespace.
