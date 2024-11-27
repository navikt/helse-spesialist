package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpMethod
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.callloging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

private val registry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

val GraphQLMetrikker =
    createRouteScopedPlugin("GraphQLMetrikker") {
        onCallRespond { call ->
            // call.receive<JsonNode> gjør at ktor ikke klarer å serve GraphQLPlayground-htmlen...
            if (call.request.httpMethod == HttpMethod.Get) return@onCallRespond
            (call.receive<JsonNode>()["operationName"]?.textValue() ?: "ukjent").let { operationName ->
                val elapsed = call.processingTimeMillis()
                graphQLResponstider
                    .withRegistry(registry)
                    .withTags(operationName)
                    .record(elapsed.toDouble())
            }
        }
    }

private val graphQLResponstider =
    DistributionSummary
        .builder("graphql_responstider")
        .description("Måler responstider for GraphQL-kall")

internal val auditLogTeller =
    Counter
        .builder("auditlog_total")
        .description("Teller antall auditlogginnslag")
        .register(registry)
