/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Application.configureMonitor() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        timers { call, exception ->
            tag("region", call.request.headers["regionId"] ?: "unspecified")
        }
        meterBinders =
            listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                JvmThreadMetrics(),
                FileDescriptorMetrics(),
                UptimeMetrics(),
            )
        registry = appMicrometerRegistry
    }
    routing {
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}
