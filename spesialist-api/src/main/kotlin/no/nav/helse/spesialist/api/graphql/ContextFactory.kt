package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.ktor.KtorGraphQLContextFactory
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGSGRUPPER
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

enum class ContextValues {
    TILGANGSGRUPPER,
    SAKSBEHANDLER,
}

class ContextFactory(
    private val tilgangsgrupper: Tilgangsgrupper,
) : KtorGraphQLContextFactory() {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        val principal =
            request.call.principal<JWTPrincipal>() ?: run {
                sikkerlogg.error("Ingen access_token for graphql-kall")
                return emptyMap<Any, Any>().toGraphQLContext()
            }
        val tilgangsgrupper = tilgangsgrupper.grupperFor(principal.getGrupper())
        return mapOf(
            TILGANGSGRUPPER to tilgangsgrupper,
            SAKSBEHANDLER to SaksbehandlerFraApi.fraOnBehalfOfToken(principal, this.tilgangsgrupper),
        ).toGraphQLContext()
    }
}

fun DataFetchingEnvironment.saksbehandler(): SaksbehandlerFraApi = graphQlContext.get(SAKSBEHANDLER)

private fun JWTPrincipal.getGrupper() = payload.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList()
