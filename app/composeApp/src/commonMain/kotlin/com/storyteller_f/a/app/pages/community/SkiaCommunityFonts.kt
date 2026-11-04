/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalTextApi::class)
internal suspend fun buildSkiaCommunityFont(
    bundle: CommunityFontBundle?,
    resolver: FontFamily.Resolver,
    createFamily: (CommunityFontBundle) -> FontFamily,
): LoadedCommunityFont {
    if (bundle == null) return LoadedCommunityFont(null)
    return try {
        val family = createFamily(bundle)
        resolver.preload(family)
        LoadedCommunityFont(family, resolver)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (fatal: Error) {
        throw fatal
    } catch (failure: Throwable) {
        // JS native font decoding errors can lack a safe JS stack; retain only their message.
        Napier.w { "Unable to decode a community font: ${failure.message.orEmpty()}" }
        LoadedCommunityFont(null)
    }
}
