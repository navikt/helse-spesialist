package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.ktor.KtorGraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
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
) : KtorGraphQLContextFactory() {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        val principal =
            request.call.principal<JWTPrincipal>() ?: run {
                sikkerlogg.error("Ingen access_token for graphql-kall")
                return emptyMap<Any, Any>().toGraphQLContext()
            }
        return mapOf(
            TILGANGER to
                SaksbehandlerTilganger(
                    gruppetilganger = principal.getGrupper(),
                    kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                    beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
                    skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
                ),
            SAKSBEHANDLER to SaksbehandlerFraApi.fraOnBehalfOfToken(principal),
        ).toGraphQLContext()
    }
}

private fun JWTPrincipal.getGrupper() = payload.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList()
