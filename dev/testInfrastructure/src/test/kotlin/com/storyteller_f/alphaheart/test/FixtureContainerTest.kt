/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FixtureContainerTest {
    @Test
    fun exportLogsBeforeFailedStartupRemoval() =
        runBlocking {
            val logs =
                createTestArtifactDirectory(
                    testArtifactRoot(),
                    "FixtureContainerTest",
                    "exportLogsBeforeFailedStartupRemoval",
                )
            recordTestArtifacts(logs, "FixtureContainerTest", "exportLogsBeforeFailedStartupRemoval") {
                val container =
                    FixtureContainer(DockerImageName.parse("redis:7.4-alpine")).apply {
                        withExposedPorts(6379)
                        waitingFor(
                            Wait.forLogMessage(
                                "never-ready-fixture-marker",
                                1,
                            ).withStartupTimeout(Duration.ofSeconds(2)),
                        )
                    }
                assertFailsWith<ContainerLaunchException> {
                    withFixtureResources(logs) { resources -> resources.start("redis", container) }
                }
                assertTrue(logs.resolve("redis.log").readText().contains("Ready to accept connections"))
                assertTrue(container.containerId == null)
            }
        }
}
