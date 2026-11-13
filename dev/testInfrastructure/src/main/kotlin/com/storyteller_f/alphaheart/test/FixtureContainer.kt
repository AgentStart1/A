/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

internal interface FixtureLogSource {
    var exportBeforeStop: () -> Unit
}

internal class FixtureContainer(image: DockerImageName) :
    GenericContainer<FixtureContainer>(image),
    FixtureLogSource {
    override var exportBeforeStop: () -> Unit = {}

    override fun stop() {
        // GenericContainer also calls stop when readiness fails, before start returns to our resource owner.
        try {
            if (containerId != null) exportBeforeStop()
        } finally {
            super.stop()
        }
    }
}

internal class FixturePostgres :
    PostgreSQLContainer<FixturePostgres>("pgvector/pgvector:pg16"),
    FixtureLogSource {
    override var exportBeforeStop: () -> Unit = {}

    override fun stop() {
        try {
            if (containerId != null) exportBeforeStop()
        } finally {
            super.stop()
        }
    }
}
