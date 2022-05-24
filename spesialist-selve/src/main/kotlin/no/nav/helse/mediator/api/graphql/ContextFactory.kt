package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import java.util.UUID

data class AuthorizedContext(val kanSeKode7: Boolean) : GraphQLContext

class ContextFactory(
    private val kode7Saksbehandlergruppe: UUID,
    private val skjermedePersonerGruppeId: UUID
) : GraphQLContextFactory<AuthorizedContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any> =
        mapOf(
            "kanSeKode7" to request.getGrupper().contains(kode7Saksbehandlergruppe),
            "kanSeSkjermedePersoner" to request.getGrupper().contains(skjermedePersonerGruppeId),
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
