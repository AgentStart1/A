/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.storyteller_f.a.app.pages.community.CommunityFontBundle
import com.storyteller_f.a.app.pages.community.CommunityFontReloaded
import com.storyteller_f.a.app.pages.community.CommunityFontStore
import com.storyteller_f.a.app.pages.community.fontFileKey
import com.storyteller_f.a.app.pages.community.observeCommunityFont
import com.storyteller_f.a.app.pages.topic.TopicComposeData
import com.storyteller_f.a.app.pages.topic.composeCommunityId
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CommunityFontStoreTest {
    private val fontFixture = CommunityFontFixture()

    @AfterTest
    fun cleanFontFiles() = runTest { fontFixture.close() }

    @Test
    fun sharesOneReadAcrossConcurrentConsumers() =
        runTest {
            var reads = 0
            val release = CompletableDeferred<Unit>()
            val store =
                fontFixture.createFontStore(this) {
                    reads++
                    release.await()
                    CommunityFontTestData.primary
                }
            val first = async { store.load(file(1)) }
            val second = async { store.load(file(1)) }
            release.complete(Unit)
            assertSame(assertNotNull(first.await()), second.await())
            assertEquals(1, reads)
        }

    @Test
    fun cancellingOneReaderKeepsTheSharedReadAlive() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val store =
                fontFixture.createFontStore(this) {
                    started.complete(Unit)
                    release.await()
                    CommunityFontTestData.primary
                }
            val cancelled = async(start = CoroutineStart.UNDISPATCHED) { store.load(file(1)) }
            started.await()
            val remaining = async { store.load(file(1)) }
            cancelled.cancel()
            release.complete(Unit)
            assertNotNull(remaining.await())
        }

    @Test
    fun fileRevisionInvalidatesTheCachedFamily() =
        runTest {
            var reads = 0
            val store =
                fontFixture.createFontStore(this) {
                    reads++
                    CommunityFontTestData.primary
                }
            val original = file(1)
            val first = store.load(original)
            assertSame(first, store.load(original.copy(favoriteId = 9)))
            assertSame(first, store.load(original.copy(url = "https://fonts.invalid/font.ttf?token=renewed")))
            val replacement = original.copy(lastModified = LocalDateTime(2025, 1, 2, 0, 0))
            assertNotSame(first, store.load(replacement))
            assertEquals(2, reads)
        }

    @Test
    fun separateSessionsNeverShareFontFamilies() =
        runTest {
            val first = fontFixture.createFontStore(this) { CommunityFontTestData.primary }
            val second = fontFixture.createFontStore(this) { CommunityFontTestData.code }
            val left = assertNotNull(first.load(file(1)))
            val right = assertNotNull(second.load(file(1)))
            assertNotSame(left, right)
            assertNotSame(left.loaded.family, right.loaded.family)
        }

    @Test
    fun aLaterIndependentReadCanRecoverAfterFailure() =
        runTest {
            var reads = 0
            val store =
                fontFixture.createFontStore(this) {
                    if (++reads == 1) error("Synthetic request failure")
                    CommunityFontTestData.primary
                }
            assertNull(store.load(file(1)))
            assertNotNull(store.load(file(1)))
            assertEquals(2, reads)
        }

    @Test
    fun observesInitialDataAndSuccessfulRetries() =
        runTest {
            val store = fontFixture.createFontStore(this) { CommunityFontTestData.primary }
            val observed = mutableListOf<CommunityFontBundle?>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                store.observeFont(file(1)).collect { observed += it }
            }
            assertNotNull(store.load(file(1)))
            runCurrent()
            assertNotNull(observed.single())
            val retried = assertNotNull(store.load(file(1), retry = true))
            runCurrent()
            assertEquals(2, observed.size)
            assertSame(retried, observed.last())
        }

    @Test
    fun unrelatedRetryDoesNotReloadTheObservedFont() =
        runTest {
            val store = fontFixture.createFontStore(this) { CommunityFontTestData.primary }
            val observed = mutableListOf<CommunityFontBundle?>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                store.observeFont(file(1)).collect { observed += it }
            }
            assertNotNull(store.load(file(1)))
            runCurrent()
            val initial = assertNotNull(observed.single())
            assertNotNull(store.load(file(2), retry = true))
            runCurrent()
            assertSame(initial, observed.single())
        }

    @Test
    fun cancellingObserverKeepsTheSharedReadAlive() =
        runTest {
            var reads = 0
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val store =
                fontFixture.createFontStore(this) {
                    reads++
                    started.complete(Unit)
                    release.await()
                    CommunityFontTestData.primary
                }
            val observer =
                backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    store.observeFont(file(1)).collect { error("Cancelled observer received a font") }
                }
            started.await()
            observer.cancelAndJoin()
            val remaining = async { store.load(file(1)) }
            release.complete(Unit)
            assertNotNull(remaining.await())
            assertEquals(1, reads)
        }

    @Test
    fun ordinaryLoadsAndEvictionsDoNotPublishReloads() =
        runTest {
            val store =
                CommunityFontStore(this, maxCachedFamilies = 1) { file, _ ->
                    fontFixture.testFontBundle(fontFileKey(file), file.name, CommunityFontTestData.primary)
                }
            val events = mutableListOf<CommunityFontReloaded>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { store.reloads.collect { events += it } }
            assertNotNull(store.load(file(1)))
            assertNotNull(store.load(file(2)))
            assertNotNull(store.load(file(1)))
            runCurrent()
            assertEquals(emptyList(), events)
        }

    @Test
    fun successfulRetryPublishesOnlyItsNewFontBundle() =
        runTest {
            val store = fontFixture.createFontStore(this) { CommunityFontTestData.primary }
            val events = mutableListOf<CommunityFontReloaded>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { store.reloads.collect { events += it } }
            assertNotNull(store.load(file(1)))
            val other = assertNotNull(store.load(file(2)))
            val retried = assertNotNull(store.load(file(1), retry = true))
            runCurrent()
            assertEquals(fontFileKey(file(1)), events.single().key)
            assertSame(retried, events.single().bundle)
            assertSame(retried, store.load(file(1)))
            assertSame(other, store.load(file(2)))
            assertEquals(1, events.size)
        }

    @Test
    fun failedRetryDoesNotPublishReloads() =
        runTest {
            var reads = 0
            val store =
                fontFixture.createFontStore(this) {
                    if (++reads > 1) error("Synthetic retry failure")
                    CommunityFontTestData.primary
                }
            val events = mutableListOf<CommunityFontReloaded>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { store.reloads.collect { events += it } }
            assertNotNull(store.load(file(1)))
            assertNull(store.load(file(1), retry = true))
            runCurrent()
            assertEquals(emptyList(), events)
        }

    @Test
    fun retriesQueueBehindAnExistingOrdinaryRead() =
        runTest {
            val initialStarted = CompletableDeferred<Unit>()
            val releaseInitial = CompletableDeferred<Unit>()
            val retryStarted = CompletableDeferred<Unit>()
            val releaseRetry = CompletableDeferred<Unit>()
            val requests = mutableListOf<Boolean>()
            val store =
                CommunityFontStore(this) { file, retry ->
                    requests += retry
                    if (retry) {
                        retryStarted.complete(Unit)
                        releaseRetry.await()
                    } else {
                        initialStarted.complete(Unit)
                        releaseInitial.await()
                    }
                    fontFixture.testFontBundle(
                        fontFileKey(file),
                        file.name,
                        if (retry) CommunityFontTestData.code else CommunityFontTestData.primary,
                    )
                }
            val events = mutableListOf<CommunityFontReloaded>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { store.reloads.collect { events += it } }
            val initial = async(start = CoroutineStart.UNDISPATCHED) { store.load(file(1)) }
            initialStarted.await()
            val first = async(start = CoroutineStart.UNDISPATCHED) { store.load(file(1), retry = true) }
            val second = async(start = CoroutineStart.UNDISPATCHED) { store.load(file(1), retry = true) }
            releaseInitial.complete(Unit)
            assertNotNull(initial.await())
            retryStarted.await()
            val joined = async(start = CoroutineStart.UNDISPATCHED) { store.load(file(1)) }
            releaseRetry.complete(Unit)
            val result = assertNotNull(first.await())
            assertSame(result, second.await())
            assertSame(result, joined.await())
            assertSame(result, store.load(file(1)))
            runCurrent()
            assertEquals(listOf(false, true), requests)
            assertSame(result, events.single().bundle)
            assertNotSame(initial.await()?.loaded?.family, result.loaded.family)
        }

    @Test
    fun cancellingRetryReaderStillPublishesItsCompletion() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val store =
                fontFixture.createFontStore(this) {
                    started.complete(Unit)
                    release.await()
                    CommunityFontTestData.primary
                }
            val events = mutableListOf<CommunityFontReloaded>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { store.reloads.collect { events += it } }
            val cancelled = async(start = CoroutineStart.UNDISPATCHED) { store.load(file(1), retry = true) }
            started.await()
            cancelled.cancelAndJoin()
            val remaining = async { store.load(file(1)) }
            release.complete(Unit)
            val bundle = assertNotNull(remaining.await())
            runCurrent()
            assertSame(bundle, events.single().bundle)
        }

    @Test
    fun retryObserverPublishesOneResultAndKeepsListening() =
        runTest {
            val requests = mutableListOf<Boolean>()
            val store =
                CommunityFontStore(this) { file, retry ->
                    requests += retry
                    fontFixture.testFontBundle(fontFileKey(file), file.name, CommunityFontTestData.primary)
                }
            val observed = mutableListOf<CommunityFontBundle?>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                store.observeFont(file(1), retry = true).collect { observed += it }
            }
            assertNotNull(store.load(file(1)))
            runCurrent()
            assertEquals(listOf(true), requests)
            assertEquals(1, observed.size)
            assertNotNull(observed.single())
            val refreshed = store.load(file(1), retry = true)
            runCurrent()
            assertEquals(listOf(true, true), requests)
            assertEquals(2, observed.size)
            assertSame(refreshed, observed.last())
        }

    @Test
    fun failedRetryObserverDoesNotStartAnOrdinaryLoad() =
        runTest {
            val requests = mutableListOf<Boolean>()
            val store =
                CommunityFontStore(this) { _, retry ->
                    requests += retry
                    error("Synthetic retry failure")
                }
            val observed = mutableListOf<CommunityFontBundle?>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                store.observeFont(file(1), retry = true).collect { observed += it }
            }
            runCurrent()
            assertEquals(listOf(true), requests)
            assertEquals(listOf<CommunityFontBundle?>(null), observed)
        }

    @Test
    fun cacheIdentityDoesNotExposeTheObjectUrl() {
        val key = fontFileKey(file(1).copy(url = "https://fonts.invalid/font.ttf?token=synthetic"))
        assertEquals(32, key.length)
    }

    @Test
    fun resolvesBothCommunityEditorRoutes() {
        assertEquals(2L, composeCommunityId(TopicComposeData.Community(2, 2L ob ObjectType.COMMUNITY)))
        assertEquals(3L, composeCommunityId(TopicComposeData.PublicRoom(1, 3, 1L ob ObjectType.ROOM)))
        assertNull(composeCommunityId(TopicComposeData.PrivateRoom(1, 1L ob ObjectType.ROOM)))
        assertNull(composeCommunityId(TopicComposeData.User(1, 1L ob ObjectType.USER)))
    }

    @Test
    fun concurrentObserversReceiveTheSameBuiltFamilyAndResolver() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(this) {
                    builds++
                    CommunityFontTestData.primary
                }
            val first = async { observeCommunityFont(file(1), store).first() }
            val second = async { observeCommunityFont(file(1), store).first() }
            val left = first.await()
            val right = second.await()
            assertNotNull(left.family)
            assertSame(left.family, right.family)
            assertSame(left.resolver, right.resolver)
            assertEquals(1, builds)
        }

    @Test
    fun faceBudgetEvictsFamiliesWithoutInvalidatingHeldObjects() =
        runTest {
            val store =
                CommunityFontStore(this, maxCachedFaces = 1) { file, _ ->
                    fontFixture.testFontBundle(fontFileKey(file), file.name, CommunityFontTestData.primary)
                }
            val first = assertNotNull(store.load(file(1)))
            val family = assertNotNull(first.loaded.family)
            val resolver = assertNotNull(first.loaded.resolver)
            assertNotNull(store.load(file(2)))
            assertNotSame(first, store.load(file(1)))
            assertSame(family, first.loaded.family)
            assertSame(resolver, first.loaded.resolver)
            val measured =
                TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr).measure(
                    AnnotatedString("A"),
                    TextStyle(fontFamily = family, fontSize = 100.sp),
                )
            assertEquals(30, measured.size.width)
        }

    @Test
    fun cancelledAccountDoesNotServeCachedFonts() =
        runTest {
            val owner = kotlinx.coroutines.CoroutineScope(coroutineContext + kotlinx.coroutines.Job())
            try {
                val store = fontFixture.createFontStore(owner) { CommunityFontTestData.primary }
                assertNotNull(store.load(file(1)))
                owner.coroutineContext[kotlinx.coroutines.Job]?.cancel()
                kotlin.test.assertFailsWith<kotlinx.coroutines.CancellationException> { store.load(file(1)) }
            } finally {
                owner.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            }
        }

    private fun file(id: Long): FileInfo =
        FileInfo.EMPTY.copy(
            id = id,
            name = "Regular.ttf",
            url = "https://fonts.invalid/$id",
        )
}
