/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.fontFileKey
import com.storyteller_f.alphaheart.app.pages.community.observeCommunityFont
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.pages.topic.composeCommunityId
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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
    fun sharesOneConstructionAcrossConcurrentConsumers() =
        runTest {
            var builds = 0
            val release = CompletableDeferred<Unit>()
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    builds++
                    release.await()
                    CommunityFontTestData.primary
                }
            val first = async { store.load(file(1)) }
            val second = async { store.load(file(1)) }
            runCurrent()
            release.complete(Unit)
            assertSame(assertNotNull(first.await()), second.await())
            assertEquals(1, builds)
        }

    @Test
    fun cancellingOneReaderPreservesOtherReaders() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    started.complete(Unit)
                    release.await()
                    CommunityFontTestData.primary
                }
            val cancelled = async { store.load(file(1)) }
            started.await()
            val remaining = async { store.load(file(1)) }
            runCurrent()
            cancelled.cancelAndJoin()
            release.complete(Unit)
            assertNotNull(remaining.await())
        }

    @Test
    fun revisionChangesConstructANewFamily() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    builds++
                    CommunityFontTestData.primary
                }
            val original = file(1)
            val first = store.load(original)
            assertSame(first, store.load(original.copy(favoriteId = 9, url = "https://fonts.invalid/renewed")))
            assertNotSame(first, store.load(original.copy(lastModified = LocalDateTime(2026, 1, 2, 0, 0))))
            assertEquals(2, builds)
        }

    @Test
    fun failedConstructionKeepsListeningForExplicitResume() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    if (++builds == 1) error("Synthetic decode failure")
                    CommunityFontTestData.primary
                }
            val states = mutableListOf<CommunityFontState>()
            backgroundScope.launch { store.observeFont(file(1)).collect { states += it } }
            runCurrent()
            assertNull(store.load(file(1)))
            assertEquals(1, builds)
            store.retry(file(1))
            val ready = store.observeFont(file(1)).first { it is CommunityFontState.Ready }
            assertSame((ready as CommunityFontState.Ready).bundle, store.load(file(1)))
            assertEquals(2, builds)
            assertEquals(1, states.filterIsInstance<CommunityFontState.Failed>().size)
        }

    @Test
    fun unrelatedResumeDoesNotRebuildAnActiveFont() =
        runTest {
            val counts = mutableMapOf<Long, Int>()
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    counts[it.id] = counts.getOrElse(it.id) { 0 } + 1
                    CommunityFontTestData.primary
                }
            val observer = backgroundScope.launch { store.observeFont(file(1)).collect {} }
            val first = assertNotNull(store.load(file(1)))
            store.retry(file(2))
            runCurrent()
            assertSame(first, store.load(file(1)))
            assertEquals(mapOf(1L to 1), counts)
            observer.cancelAndJoin()
        }

    @Test
    fun idleFlowsReleaseReplayAndAreReconstructed() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    builds++
                    CommunityFontTestData.primary
                }
            val first = assertNotNull(store.load(file(1)))
            advanceTimeBy(5_001)
            runCurrent()
            assertNotSame(first, store.load(file(1)))
            assertEquals(2, builds)
            assertNotNull(first.loaded.family)
        }

    @Test
    fun activeSubscriptionsAreNeverEvictedByOtherFamilies() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    builds++
                    CommunityFontTestData.primary
                }
            val observer = backgroundScope.launch { store.observeFont(file(1)).collect {} }
            val first = assertNotNull(store.load(file(1)))
            for (id in 2L..16L) assertNotNull(store.load(file(id)))
            advanceTimeBy(6_000)
            runCurrent()
            assertSame(first, store.load(file(1)))
            assertEquals(16, builds)
            observer.cancelAndJoin()
        }

    @Test
    fun aPreviousIdleTimerCannotEvictANewerGracePeriod() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    builds++
                    CommunityFontTestData.primary
                }
            val first = store.load(file(1))
            advanceTimeBy(4_000)
            assertSame(first, store.load(file(1)))
            advanceTimeBy(1_001)
            runCurrent()
            assertSame(first, store.load(file(1)))
            assertEquals(1, builds)
        }

    @Test
    fun concurrentObserversShareTheBuiltFamilyAndResolver() =
        runTest {
            var builds = 0
            val store =
                fontFixture.createFontStore(backgroundScope) {
                    builds++
                    CommunityFontTestData.primary
                }
            val first = async { observeCommunityFont(file(1), store).first { it.family != null } }
            val second = async { observeCommunityFont(file(1), store).first { it.family != null } }
            val left = first.await()
            val right = second.await()
            assertSame(left.family, right.family)
            assertSame(left.resolver, right.resolver)
            assertEquals(1, builds)
        }

    @Test
    fun cacheIdentityDoesNotExposeTheObjectUrl() {
        assertEquals(32, fontFileKey(file(1).copy(url = "https://fonts.invalid/font.ttf?token=synthetic")).length)
    }

    @Test
    fun resolvesBothCommunityEditorRoutes() {
        assertEquals(2L, composeCommunityId(TopicComposeData.Community(2, 2L ob ObjectType.COMMUNITY)))
        assertEquals(3L, composeCommunityId(TopicComposeData.PublicRoom(1, 3, 1L ob ObjectType.ROOM)))
        assertNull(composeCommunityId(TopicComposeData.PrivateRoom(1, 1L ob ObjectType.ROOM)))
        assertNull(composeCommunityId(TopicComposeData.User(1, 1L ob ObjectType.USER)))
    }

    private fun file(id: Long): FileInfo =
        FileInfo.EMPTY.copy(
            id = id,
            name = "Regular.ttf",
            url = "https://fonts.invalid/$id",
        )
}
