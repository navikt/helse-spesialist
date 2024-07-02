
package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDER_EPOST
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_IDENT
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_NAVN
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

enum class ContextValues(val key: String) {
    TILGANGER("tilganger"),
    SAKSBEHANDLER_NAVN("saksbehandlerNavn"),
    SAKSBEHANDER_EPOST("saksbehanderEpost"),
    SAKSBEHANDLER_IDENT("saksbehandlerIdent"),
    SAKSBEHANDLER("saksbehandler"),
}

class ContextFactory(
    private val kode7Saksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
) : GraphQLContextFactory<ApplicationRequest> {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext =
        mapOf(
            TILGANGER.key to
                SaksbehandlerTilganger(
                    gruppetilganger = request.getGrupper(),
                    kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                    beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
                    skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
                ),
            SAKSBEHANDLER_NAVN.key to request.getSaksbehandlerName(),
            SAKSBEHANDER_EPOST.key to request.getSaksbehanderEpost(),
            SAKSBEHANDLER_IDENT.key to request.getSaksbehandlerIdent(),
            SAKSBEHANDLER.key to request.saksbehandler(),
        ).toGraphQLContext()
}

private fun ApplicationRequest.getGrupper(): List<UUID> {
    val accessToken = call.principal<JWTPrincipal>()
    if (accessToken == null) {
        sikkerlogg.error("Ingen access_token for graphql-kall")
        return emptyList()
    }
    return accessToken.payload.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList()
}

private fun ApplicationRequest.getSaksbehandlerName(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("name")?.asString() ?: ""
}

private fun ApplicationRequest.getSaksbehanderEpost(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("preferred_username")?.asString() ?: ""
}

private fun ApplicationRequest.getSaksbehandlerIdent(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("NAVident")?.asString() ?: ""
}

private fun ApplicationRequest.saksbehandler(): Lazy<SaksbehandlerFraApi> {
    val accessToken = call.principal<JWTPrincipal>()
    return lazy {
        accessToken?.let(SaksbehandlerFraApi::fraOnBehalfOfToken)
            ?: throw IllegalStateException("Forventer å finne saksbehandler i access token")
    }
}
