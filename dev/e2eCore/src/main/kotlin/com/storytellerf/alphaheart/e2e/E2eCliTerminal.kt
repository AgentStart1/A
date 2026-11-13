/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.e2e

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.storyteller_f.alphaheart.test.E2eCoverage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private const val TRANSCRIPT_LIMIT = 24_000
private const val TERMINAL_COLUMNS = 120
private const val TERMINAL_ROWS = 40
private const val EXIT_POLL_INTERVAL_MILLIS = 50L

/** Creates terminals that run an installed CLI distribution against an E2E backend. */
class E2eCliTerminalFactory(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    /** Execute [block] with a terminal and close the process when the block completes. */
    suspend fun <T> useTerminal(
        installDirProperty: String,
        ports: E2ePorts,
        scope: CoroutineScope,
        block: suspend (E2eCliTerminal) -> T,
    ): T {
        val terminal =
            E2eCliTerminal.start(
                installDirProperty = installDirProperty,
                ports = ports,
                scope = scope,
                ioDispatcher = ioDispatcher,
            )
        return withE2eTerminalCleanup(
            cleanup = { failure -> closeTerminal(terminal, ports, installDirProperty, failure) },
            block = { block(terminal) },
        )
    }

    private suspend fun closeTerminal(
        terminal: E2eCliTerminal,
        ports: E2ePorts,
        installDirProperty: String,
        primaryFailure: Throwable?,
    ) {
        withContext(NonCancellable) {
            val failures = mutableListOf<Throwable>()
            try {
                try {
                    terminal.writeObservations(checkNotNull(ports.artifactDirectory))
                } catch (failure: Exception) {
                    failures.add(failure)
                }
                try {
                    if (E2eCoverage.enabled) {
                        val directory = checkNotNull(ports.artifactDirectory)
                        val targetName = if (installDirProperty.contains("panel")) "panel" else "app"
                        E2eCoverage.collectJvm(
                            directory.resolve("coverage/control/cli"),
                            directory.resolve("coverage/cli"),
                            ":$targetName:cliApp",
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    failures.add(failure)
                }
            } finally {
                closeTerminalProcess(terminal)?.let(failures::add)
            }
            failures.firstOrNull()?.let { failure ->
                failures.drop(1).forEach(failure::addSuppressed)
                primaryFailure?.addSuppressed(failure) ?: throw failure
            }
        }
    }

    private suspend fun closeTerminalProcess(terminal: E2eCliTerminal): Throwable? =
        try {
            terminal.close()
            null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            failure
        }
}

/** Like use, with an inline cleanup lambda so the owner can perform cancellation-safe suspending cleanup. */
internal inline fun <T> withE2eTerminalCleanup(cleanup: (Throwable?) -> Unit, block: () -> T): T {
    var primaryFailure: Throwable? = null
    return try {
        block()
    } catch (failure: Throwable) {
        primaryFailure = failure
        throw failure
    } finally {
        cleanup(primaryFailure)
    }
}

/** A PTY-backed installed CLI process that can receive input and assert terminal output. */
class E2eCliTerminal private constructor(
    private val process: PtyProcess,
    private val input: BufferedInputStream,
    private val output: BufferedOutputStream,
    private val outputChunks: Channel<String>,
    private val readerJob: Job,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val transcript = StringBuilder()
    private var searchStart = 0
    private val observations = mutableListOf<String>()

    /** Send a line of input to the CLI. */
    suspend fun sendLine(line: String) {
        observations.add("Submitted input: ${line.length} characters")
        withContext(ioDispatcher) {
            output.write(line.encodeToByteArray())
            output.write(process.enterKeyCode.toInt())
            output.flush()
        }
    }

    /** Wait until [expected] appears in the terminal transcript. */
    suspend fun awaitText(expected: String) {
        val isFound =
            withTimeoutOrNull(90.seconds) {
                waitForText(expected)
            } == true
        observations.add("Observed: $expected; matched=$isFound")
        assertTrue(isFound, "CLI did not render '$expected'.\n${recentTranscript()}")
    }

    /** Wait for the CLI process to exit. */
    suspend fun awaitExit() {
        val hasExited =
            withTimeoutOrNull(10.seconds) {
                while (process.isAlive) {
                    delay(EXIT_POLL_INTERVAL_MILLIS)
                }
                true
            } == true
        assertTrue(hasExited, "CLI did not exit after selecting 0.\n${recentTranscript()}")
    }

    private suspend fun waitForText(expected: String): Boolean {
        while (true) {
            val matchIndex = transcript.indexOf(expected, searchStart)
            if (matchIndex >= 0) {
                searchStart = matchIndex + expected.length
                return true
            }
            val chunk = outputChunks.receiveCatching().getOrNull()
            checkNotNull(chunk) { "CLI exited before rendering '$expected'.\n${recentTranscript()}" }
            transcript.append(chunk)
        }
    }

    internal fun writeObservations(directory: File) {
        directory.resolve("terminal-observations.log").writeText(observations.joinToString("\n"))
    }

    internal suspend fun close() {
        withContext(ioDispatcher) {
            if (process.isAlive) process.destroyForcibly()
            input.close()
            output.close()
        }
        readerJob.cancelAndJoin()
    }

    private fun recentTranscript(): String = transcript.takeLast(TRANSCRIPT_LIMIT).toString()

    /** Creates terminal processes for the E2E test driver. */
    companion object {
        /** Start an installed CLI process and attach an output reader to [scope]. */
        suspend fun start(
            installDirProperty: String,
            ports: E2ePorts,
            scope: CoroutineScope,
            ioDispatcher: CoroutineDispatcher,
        ): E2eCliTerminal {
            val installDir = File(checkNotNull(System.getProperty(installDirProperty)))
            val executable = File(installDir, executableName())
            check(executable.isFile) {
                "CLI executable not found: ${executable.absolutePath}"
            }
            val process =
                withContext(ioDispatcher) {
                    PtyProcessBuilder(executableCommand(executable))
                        .setDirectory(installDir.absolutePath)
                        .setEnvironment(cliEnvironment(ports))
                        .setInitialColumns(TERMINAL_COLUMNS)
                        .setInitialRows(TERMINAL_ROWS)
                        .setRedirectErrorStream(true)
                        .start()
                }
            val input = BufferedInputStream(process.inputStream)
            val outputChunks = Channel<String>(Channel.UNLIMITED)
            val readerJob =
                scope.launch(ioDispatcher) {
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    try {
                        while (true) {
                            val count = input.read(buffer)
                            if (count <= 0) break
                            outputChunks.send(buffer.decodeToString(endIndex = count))
                        }
                    } finally {
                        outputChunks.close()
                    }
                }
            return E2eCliTerminal(
                process = process,
                input = input,
                output = BufferedOutputStream(process.outputStream),
                outputChunks = outputChunks,
                readerJob = readerJob,
                ioDispatcher = ioDispatcher,
            )
        }

        private fun executableName(): String =
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                "bin/cliApp.bat"
            } else {
                "bin/cliApp"
            }

        private fun executableCommand(executable: File): Array<String> =
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                arrayOf("cmd.exe", "/c", executable.absolutePath)
            } else {
                arrayOf(executable.absolutePath)
            }

        private fun cliEnvironment(ports: E2ePorts): Map<String, String> {
            val environment = System.getenv().toMutableMap()
            with(environment) {
                put("TERM", "xterm-256color")
                put("SERVER_URL", "http://127.0.0.1:${ports.server}")
                put("WS_SERVER_URL", "ws://127.0.0.1:${ports.ws}")
                if (E2eCoverage.enabled) {
                    put(
                        "E2E_COVERAGE_DIRECTORY",
                        checkNotNull(ports.artifactDirectory).resolve("coverage/control/cli").absolutePath,
                    )
                }
            }
            return environment
        }
    }
}
