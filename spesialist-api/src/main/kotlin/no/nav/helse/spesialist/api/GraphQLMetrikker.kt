package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpMethod
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.callloging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.prometheus.client.Counter
import io.prometheus.client.Summary

val GraphQLMetrikker = createRouteScopedPlugin("GraphQLMetrikker") {
    onCallRespond { call ->
        // call.receive<JsonNode> gjør at ktor ikke klarer å serve GraphQLPlayground-htmlen...
        if (call.request.httpMethod == HttpMethod.Get) return@onCallRespond
        (call.receive<JsonNode>()["operationName"]?.textValue() ?: "ukjent").let { operationName ->
            val elapsed = call.processingTimeMillis()
            graphQLResponstider.labels(operationName).observe(elapsed.toDouble())
        }
    }
}

private val graphQLResponstider =
    Summary.build("graphql_responstider", "Måler responstider for GraphQL-kall")
        .labelNames("operationName")
        .register()


internal val auditLogTeller = Counter.build("auditlog_total", "Teller antall auditlogginnslag").register()