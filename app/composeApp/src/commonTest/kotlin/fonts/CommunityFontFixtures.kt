/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.a.app.common.createDownloadFileStore
import com.storyteller_f.a.app.common.write
import com.storyteller_f.a.app.pages.community.CommunityFontBundle
import com.storyteller_f.a.app.pages.community.CommunityFontPresentation
import com.storyteller_f.a.app.pages.community.CommunityFontStore
import com.storyteller_f.a.app.pages.community.LoadedCommunityFont
import com.storyteller_f.a.app.pages.community.buildCommunityFont
import com.storyteller_f.a.app.pages.community.combineCommunityFonts
import com.storyteller_f.a.app.pages.community.createFontFace
import com.storyteller_f.a.app.pages.community.fontFileKey
import com.storyteller_f.shared.model.FileInfo
import kotlinx.coroutines.CoroutineScope
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
    private val files = createDownloadFileStore()
    private val mutex = Mutex()
    private val roots = mutableListOf<String>()

    suspend fun testFontBundle(identity: String, name: String, bytes: ByteArray): CommunityFontBundle {
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
        CommunityFontStore(scope) { file, _ -> testFontBundle(fontFileKey(file), file.name, fetch(file)) }

    suspend fun close() {
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

internal fun fontTestZip(vararg names: String): ByteArray {
    val output = Buffer()
    ZipOutputStream(output).use { archive ->
        names.forEach { name ->
            archive.putNextEntry(ZipEntry(name))
            archive.write(CommunityFontTestData.primary)
            archive.closeEntry()
        }
    }
    return output.readByteArray()
}
