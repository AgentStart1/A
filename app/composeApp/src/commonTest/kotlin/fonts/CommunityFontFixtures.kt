/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.common.write
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontBundle
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontPresentation
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontStore
import com.storyteller_f.alphaheart.app.pages.community.LoadedCommunityFont
import com.storyteller_f.alphaheart.app.pages.community.buildCommunityFont
import com.storyteller_f.alphaheart.app.pages.community.combineCommunityFonts
import com.storyteller_f.alphaheart.app.pages.community.createFontFace
import com.storyteller_f.alphaheart.app.pages.community.fontFileKey
import com.storyteller_f.shared.model.FileInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import no.synth.kmpzip.kotlinx.ZipOutputStream
import no.synth.kmpzip.zip.ZipEntry

/**
 * Native file fonts can be resolved again by another presentation resolver. Keep backing files
 * until the whole test finishes, including all collectors and renderers.
 */
internal class CommunityFontFixture {
    private val files = createFileEngine()
    private val mutex = Mutex()
    private val roots = mutableListOf<String>()
    private val builds = MutableStateFlow<Set<Job>>(emptySet())

    suspend fun testFontBundle(identity: String, name: String, bytes: ByteArray): CommunityFontBundle {
        val owner = checkNotNull(currentCoroutineContext()[Job])
        builds.update { it + owner }
        return try {
            buildFontBundle(identity, name, bytes)
        } finally {
            builds.update { it - owner }
        }
    }

    private suspend fun buildFontBundle(identity: String, name: String, bytes: ByteArray): CommunityFontBundle {
        val root = files.location("test-font-${kotlin.uuid.Uuid.random().toHexString()}")
        mutex.withLock { roots += root }
        val path = "$root/$name"
        files.write(path, bytes)
        val faces = listOf(createFontFace(path))
        val loaded =
            try {
                buildCommunityFont(identity, faces, files)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: IllegalStateException) {
                LoadedCommunityFont(null)
            }
        return CommunityFontBundle(faces, loaded)
    }

    fun createFontStore(scope: CoroutineScope, fetch: suspend (FileInfo) -> ByteArray): CommunityFontStore =
        createBundleStore(scope) { file -> testFontBundle(fontFileKey(file), file.name, fetch(file)) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun createBundleStore(scope: CoroutineScope, fetch: suspend (FileInfo) -> CommunityFontBundle): CommunityFontStore {
        val generations = MutableStateFlow(emptyMap<Long, Int>())
        return CommunityFontStore(scope, observe = { file ->
            generations.mapLatest { it[file.id] ?: 0 }.distinctUntilChanged().mapLatest {
                try {
                    CommunityFontState.Ready(fetch(file))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    CommunityFontState.Failed(failure)
                }
            }
        }) { file -> generations.update { it + (file.id to it.getOrElse(file.id) { 0 } + 1) } }
    }

    /** UI owners must be cancelled first; native file construction drains before its files are removed. */
    suspend fun close() {
        builds.value.joinAll()
        val paths = mutex.withLock { roots.toList().also { roots.clear() } }
        paths.forEach { files.delete(it) }
    }
}

internal fun buildTestCommunityFonts(
    content: CommunityFontBundle?,
    code: CommunityFontBundle?,
): CommunityFontPresentation =
    combineCommunityFonts(
        content?.loaded ?: LoadedCommunityFont(null),
        code?.loaded ?: LoadedCommunityFont(null),
    )

internal fun fontTestZip(vararg names: String): ByteArray =
    fontTestArchive(
        *names.map { name ->
            val face = createFontFace(name)
            val bytes =
                when {
                    face.weight.weight == 700 && face.style == androidx.compose.ui.text.font.FontStyle.Italic ->
                        CommunityFontTestData.primaryBoldItalic

                    face.weight.weight == 700 -> CommunityFontTestData.primaryBold

                    face.style == androidx.compose.ui.text.font.FontStyle.Italic -> CommunityFontTestData.primaryItalic

                    else -> CommunityFontTestData.primary
                }
            name to bytes
        }.toTypedArray(),
    )

internal fun fontTestArchive(vararg entries: Pair<String, ByteArray>): ByteArray {
    val output = Buffer()
    ZipOutputStream(output).use { archive ->
        entries.forEach { (name, bytes) ->
            archive.putNextEntry(ZipEntry(name))
            archive.write(bytes)
            archive.closeEntry()
        }
    }
    return output.readByteArray()
}

/** Compatibility helpers live only in tests; production observes continuously and retries through resume. */
internal suspend fun CommunityFontStore.load(file: FileInfo): CommunityFontBundle? =
    when (val state = observeFont(file).first { it !is CommunityFontState.Loading }) {
        is CommunityFontState.Ready -> state.bundle
        else -> null
    }

internal suspend fun CommunityFontDownload.load(file: FileInfo): CommunityFontBundle =
    when (val state = observe(file).first { it !is CommunityFontState.Loading }) {
        is CommunityFontState.Ready -> state.bundle
        is CommunityFontState.Failed -> throw state.failure
        CommunityFontState.Loading -> error("Unreachable")
    }
