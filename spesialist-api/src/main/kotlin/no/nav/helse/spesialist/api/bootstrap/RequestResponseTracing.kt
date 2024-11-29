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
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import org.slf4j.Logger

private val ignoredPaths = listOf("/metrics", "/isalive", "/isready")

internal fun Application.requestResponseTracing(logger: Logger) {
    val httpRequestCounter =
        Counter.build(
            "http_requests_total",
            "Counts the http requests",
        )
            .labelNames("method", "code")
            .register()

    val httpRequestDuration =
        Histogram.build(
            "http_request_duration_seconds",
            "Distribution of http request duration",
        )
            .register()

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            if (call.request.uri in ignoredPaths) return@intercept proceed()
            logger.info("incoming callId=${call.callId} method=${call.request.httpMethod.value} uri=${call.request.uri}")
            httpRequestDuration.startTimer().use {
                proceed()
            }
        } catch (err: Throwable) {
            logger.error("exception thrown during processing: ${err.message} callId=${call.callId} ", err)
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
        httpRequestCounter.labels(call.request.httpMethod.value, "${status.value}").inc()
    }
}
