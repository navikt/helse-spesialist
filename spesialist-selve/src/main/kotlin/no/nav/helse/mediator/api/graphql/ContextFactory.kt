@file:Suppress("DEPRECATION", "OverridingDeprecatedMember")

package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.request.*
import java.util.*

data class AuthorizedContext(val kanSeKode7: Boolean) : GraphQLContext

class ContextFactory(private val kode7Saksbehandlergruppe: UUID) :
    GraphQLContextFactory<AuthorizedContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any> =
        mapOf("kanSeKode7" to request.getGrupper().contains(kode7Saksbehandlergruppe))

    override suspend fun generateContext(request: ApplicationRequest): AuthorizedContext {
        return AuthorizedContext(request.getGrupper().contains(kode7Saksbehandlergruppe))
    }

}

private fun ApplicationRequest.getGrupper(): List<UUID> {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList()
}
