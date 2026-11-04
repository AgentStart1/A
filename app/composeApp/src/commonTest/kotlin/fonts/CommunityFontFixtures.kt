/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.a.app.pages.community.CommunityFontBundle
import com.storyteller_f.a.app.pages.community.CommunityFontPresentation
import com.storyteller_f.a.app.pages.community.buildCommunityFont
import com.storyteller_f.a.app.pages.community.combineCommunityFonts
import com.storyteller_f.a.app.pages.community.createFontFace
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import no.synth.kmpzip.kotlinx.ZipOutputStream
import no.synth.kmpzip.zip.ZipEntry

internal fun testFontBundle(identity: String, name: String, bytes: ByteArray): CommunityFontBundle =
    CommunityFontBundle(identity, listOf(createFontFace(name, bytes)))

internal suspend fun buildTestCommunityFonts(
    content: CommunityFontBundle?,
    code: CommunityFontBundle?,
): CommunityFontPresentation = combineCommunityFonts(buildCommunityFont(content), buildCommunityFont(code))

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
