package no.nav.helse.spesialist.api.bootstrap

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationSendPipeline
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.logg.loggThrowable
import org.slf4j.Logger

private val meterRegistry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

private val ignoredPaths = listOf("/metrics", "/isalive", "/isready")

internal fun Application.requestResponseTracing(logger: Logger) {
    val httpRequestCounter =
        Counter
            .builder("http_requests_total")
            .description("Counts the http requests")
            .tags("method", "code")

    val httpRequestDuration =
        Timer
            .builder("http_request_duration_seconds")
            .description("Distribution of http request duration")
            .register(meterRegistry)

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            if (call.request.uri in ignoredPaths) return@intercept proceed()
            logger.info("incoming callId=${call.callId} method=${call.request.httpMethod.value} uri=${call.request.uri}")
            httpRequestDuration
                .wrap {
                    runBlocking {
                        proceed()
                    }
                }.run()
        } catch (err: Throwable) {
            loggThrowable("exception thrown during processing: ${err.message} callId=${call.callId} ", err)
            throw err
        }
    }

    sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
        val status =
            call.response.status() ?: (
                when (message) {
                    is OutgoingContent -> message.status
                    is HttpStatusCode -> message
                    else -> null
                } ?: HttpStatusCode.OK
            ).also { status ->
                call.response.status(status)
            }

        if (call.request.uri in ignoredPaths) return@intercept
        logger.info("responding with status=${status.value} callId=${call.callId} ")
        httpRequestCounter
            .withRegistry(meterRegistry)
            .withTag(call.request.httpMethod.value, "${status.value}")
            .increment()
    }
}
