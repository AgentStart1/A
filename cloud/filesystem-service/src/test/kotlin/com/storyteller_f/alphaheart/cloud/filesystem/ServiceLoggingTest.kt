/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.status.Status
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ServiceLoggingTest {
    @Test
    fun writeServiceLogsToConfiguredDirectory() {
        assertIs<LoggerContext>(LoggerFactory.getILoggerFactory())
        val parent = File("build/test/service-logging").also { it.mkdirs() }
        val directory = Files.createTempDirectory(parent.toPath(), "logs-").toFile()
        val context = LoggerContext()
        try {
            context.mdcAdapter = LogbackMDCAdapter()
            context.putProperty("LOG_PATH", directory.canonicalPath)
            val configuration = JoranConfigurator().also { it.context = context }
            configuration.doConfigure(requireNotNull(javaClass.getResource("/logback.xml")))
            val root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            assertIs<ConsoleAppender<ILoggingEvent>>(root.getAppender("STDOUT")).setOutputStream(stdout)
            assertIs<ConsoleAppender<ILoggingEvent>>(root.getAppender("STDERR")).setOutputStream(stderr)
            val logger = context.getLogger("service-logging-test")
            logger.info("synthetic service event")
            logger.warn("synthetic service warning")
            logger.error("synthetic service failure", IllegalStateException("synthetic cause"))
            assertTrue(stdout.toString().contains("synthetic service event"))
            assertFalse(stdout.toString().contains("synthetic service warning"))
            assertFalse(stdout.toString().contains("synthetic service failure"))
            assertFalse(stderr.toString().contains("synthetic service event"))
            assertTrue(stderr.toString().contains("synthetic service warning"))
            assertTrue(stderr.toString().contains("synthetic service failure"))
            assertTrue(stderr.toString().contains("synthetic cause"))
            assertTrue(context.statusManager.copyOfStatusList.none { it.level == Status.ERROR })
            val log = directory.resolve("filesystem.log").readText()
            assertTrue(log.contains("synthetic service event"))
            assertTrue(log.contains("INFO"))
            assertTrue(log.contains("WARN"))
            assertTrue(log.contains("ERROR"))
            assertTrue(log.contains("synthetic cause"))
        } finally {
            context.stop()
            directory.deleteRecursively()
        }
    }
}
