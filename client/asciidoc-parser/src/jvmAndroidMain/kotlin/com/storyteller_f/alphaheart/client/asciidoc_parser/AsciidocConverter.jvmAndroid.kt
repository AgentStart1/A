/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val ASCIIDOCTOR_SCRIPT_PATH = "files/asciidoctor.min.js"

@OptIn(ExperimentalUuidApi::class)
internal actual suspend fun buildPlatformAsciidocPreview(source: String): String {
    val converter = Res.readBytes(ASCIIDOCTOR_SCRIPT_PATH).decodeToString()
    val nonce = Uuid.random().toHexString()
    val escapedSource = source.replace("&", "&amp;").replace("<", "&lt;")
    val policy =
        "default-src 'none'; script-src 'nonce-$nonce'; style-src 'unsafe-inline'; " +
            "img-src https: http: data:; base-uri 'none'; form-action 'none'"
    val scripts =
        """
        <template id="source">$escapedSource</template>
        <script nonce="$nonce">$converter</script>
        <script nonce="$nonce">
          const output = document.getElementById('preview');
          try {
            const source = document.getElementById('source').content.textContent;
            const asciidoctor = module${'$'}build${'$'}asciidoctor_browser.default();
            output.innerHTML = asciidoctor.convert(source, {
              safe: 'safe',
              attributes: { showtitle: true }
            });
          } catch (error) {
            console.error(error);
            output.textContent = 'Unable to render AsciiDoc.';
          }
        </script>
        """.trimIndent()
    return wrapAsciidocPreview(
        documentHtml = "",
        head = """<meta http-equiv="Content-Security-Policy" content="$policy">""",
        scripts = scripts,
    )
}
