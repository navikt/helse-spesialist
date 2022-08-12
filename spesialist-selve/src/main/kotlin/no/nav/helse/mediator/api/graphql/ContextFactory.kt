package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger

data class AuthorizedContext(val kanSeKode7: Boolean) : GraphQLContext

class ContextFactory(
    private val kode7Saksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
    private val riskSaksbehandlergruppe: UUID,
) : GraphQLContextFactory<AuthorizedContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any> =
        mapOf(
            "tilganger" to SaksbehandlerTilganger(
                gruppetilganger = request.getGrupper(),
                kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                riskSaksbehandlergruppe = riskSaksbehandlergruppe,
                beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
                skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe
            ),
            "saksbehandlerNavn" to request.getSaksbehandlerName()
        )

    @Deprecated("The generic context object is deprecated in favor of the context map")
    override suspend fun generateContext(request: ApplicationRequest): AuthorizedContext {
        return AuthorizedContext(request.getGrupper().contains(kode7Saksbehandlergruppe))
    }

}

private fun ApplicationRequest.getGrupper(): List<UUID> {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList()
}

private fun ApplicationRequest.getSaksbehandlerName(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("name")?.asString() ?: ""
}
