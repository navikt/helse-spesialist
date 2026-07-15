package no.nav.helse.spesialist.api.graphql

import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.receive
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import tools.jackson.databind.JsonNode

private val registry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

internal val GraphQLMetrikker =
    createRouteScopedPlugin("GraphQLMetrikker") {
        onCallRespond { call ->
            val operationName = call.receive<JsonNode>()["operationName"]?.textValue() ?: "ukjent"
            val elapsed = call.processingTimeMillis()
            graphQLResponstider
                .withRegistry(registry)
                .withTag("operationName", operationName)
                .record(elapsed.toDouble())
        }
    }

private val graphQLResponstider =
    DistributionSummary
        .builder("graphql_responstider")
        .description("Måler responstider for GraphQL-kall")
