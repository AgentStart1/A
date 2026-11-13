/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class CustomSLF4JServiceProvider : SLF4JServiceProvider {
    override fun getLoggerFactory(): ILoggerFactory = CustomLoggerFactory()

    override fun getMarkerFactory(): IMarkerFactory = BasicMarkerFactory()

    override fun getMDCAdapter(): MDCAdapter = BasicMDCAdapter()

    override fun getRequestedApiVersion(): String = "2.0.0"

    override fun initialize() = Unit
}

class CustomLoggerFactory : ILoggerFactory {
    override fun getLogger(name: String?): Logger = CustomSlf4jLogger(name ?: "Default")
}

class CustomSlf4jLogger(private val customName: String) : AbstractLogger() {
    override fun isTraceEnabled(): Boolean = false

    override fun isTraceEnabled(marker: Marker?): Boolean = false

    override fun isDebugEnabled(): Boolean = true

    override fun isDebugEnabled(marker: Marker?): Boolean = true

    override fun isInfoEnabled(): Boolean = true

    override fun isInfoEnabled(marker: Marker?): Boolean = true

    override fun isWarnEnabled(): Boolean = true

    override fun isWarnEnabled(marker: Marker?): Boolean = true

    override fun isErrorEnabled(): Boolean = true

    override fun isErrorEnabled(marker: Marker?): Boolean = true

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level?,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any>?,
        throwable: Throwable?,
    ) {
        level ?: return
        messagePattern ?: return
        if (!arguments.isNullOrEmpty()) {
            Napier.w(tag = customName) {
                "argument is not empty $arguments"
            }
        }
        Napier.log(getLevel(level), customName, throwable, messagePattern)
    }

    private fun getLevel(level: Level): LogLevel =
        when (level) {
            Level.ERROR -> LogLevel.ERROR
            Level.WARN -> LogLevel.WARNING
            Level.INFO -> LogLevel.INFO
            Level.DEBUG -> LogLevel.DEBUG
            Level.TRACE -> LogLevel.VERBOSE
        }
}
