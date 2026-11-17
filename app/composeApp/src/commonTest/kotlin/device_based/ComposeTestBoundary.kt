/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.room3.RoomDatabase
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.allStringResources
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString

/** Resource HTTP reads are external work and are not tracked by Compose's test clock. */
@OptIn(ExperimentalTestApi::class, ExperimentalResourceApi::class)
internal fun runAppComposeUiTest(block: suspend ComposeUiTest.() -> Unit): TestResult =
    runComposeUiTest {
        Res.allStringResources.values.forEach { getString(it) }
        CoreStrings.preload()
        block()
    }

/** Unlike synchronous waitUntil, this allows browser Promise callbacks to complete. */
@OptIn(ExperimentalTestApi::class)
internal suspend fun ComposeUiTest.awaitExternalCondition(timeoutMillis: Long = 1000, condition: () -> Boolean) {
    // A real dispatcher keeps this existing assertion budget independent of virtual test time.
    withContext(Dispatchers.Default) {
        withTimeout(timeoutMillis) {
            while (true) {
                awaitIdle()
                if (condition()) return@withTimeout
                delay(1)
            }
        }
    }
}

/** Room close spins on outstanding worker operations; suspend their cancellation before closing. */
internal suspend fun closeTestDatabase(database: RoomDatabase, pagingScope: CoroutineScope) {
    withContext(NonCancellable) {
        pagingScope.coroutineContext.job.cancelAndJoin()
        database.getCoroutineScope().coroutineContext.job.cancelAndJoin()
        database.close()
    }
}
