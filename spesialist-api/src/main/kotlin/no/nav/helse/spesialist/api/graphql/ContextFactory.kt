
package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import com.expediagroup.graphql.server.types.GraphQLRequest
import graphql.GraphQLContext
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receiveText
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

enum class ContextValues {
    TILGANGER,
    SAKSBEHANDLER,
}

class ContextFactory(
    private val kode7Saksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
) : GraphQLContextFactory<ApplicationRequest> {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext =
        if (isIntrospectionRequest(request)) {
            emptyMap<Any, Any>().toGraphQLContext()
        } else {
            mapOf(
                TILGANGER to
                    SaksbehandlerTilganger(
                        gruppetilganger = request.getGrupper(),
                        kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                        beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
                        skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
                    ),
                SAKSBEHANDLER to request.saksbehandler(),
            ).toGraphQLContext()
        }

    private suspend fun isIntrospectionRequest(request: ApplicationRequest): Boolean {
        val graphQLRequest = objectMapper.readValue(request.call.receiveText(), GraphQLRequest::class.java)
        return (graphQLRequest.operationName == "IntrospectionQuery" || graphQLRequest.query.contains("query IntrospectionQuery"))
    }
}

private fun ApplicationRequest.getGrupper(): List<UUID> {
    val accessToken = call.principal<JWTPrincipal>()
    if (accessToken == null) {
        sikkerlogg.error("Ingen access_token for graphql-kall")
        return emptyList()
    }
    return accessToken.payload.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList()
}

private fun ApplicationRequest.saksbehandler(): SaksbehandlerFraApi {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.let(SaksbehandlerFraApi::fraOnBehalfOfToken)
        ?: throw IllegalStateException("Forventer å finne saksbehandler i access token")
}
