package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.helse.spesialist.api.rest.withSaksbehandlerIdentMdc
import no.nav.helse.spesialist.application.logg.teamLogs
import java.time.Duration

fun Routing.graphQLRoute(graphQLPlugin: GraphQL) {
    route("graphql") {
        authenticate("oidc") {
            install(GraphQLMetrikker)
            install(GraphQLCallLogging)
            post {
                val start = System.nanoTime()
                withSaksbehandlerIdentMdc(call) {
                    val result =
                        checkNotNull<GraphQLServerResponse>(graphQLPlugin.server.execute(call.request)) { "Kall mot GraphQL server feilet" }

                    if (result is GraphQLResponse<*>) {
                        result.errors.takeUnless { it.isNullOrEmpty() }?.let {
                            teamLogs.warn("GraphQL-respons inneholder feil: ${it.joinToString()}")
                        }
                    }

                    val tidBrukt = Duration.ofNanos(System.nanoTime() - start)
                    teamLogs.trace("Kall behandlet etter ${tidBrukt.toMillis()} ms")
                    call.respond<GraphQLServerResponse>(result)
                }
            }
        }
    }
}
