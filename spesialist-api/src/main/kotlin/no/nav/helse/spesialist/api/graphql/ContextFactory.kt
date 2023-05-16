@file:Suppress("DEPRECATION")

package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.server.execution.GraphQLContextFactory
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDER_EPOST
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_IDENT
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_NAVN
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER_OID
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

data class AuthorizedContext(val kanSeKode7: Boolean) : GraphQLContext

enum class ContextValues(val key: String) {
    TILGANGER("tilganger"),
    SAKSBEHANDLER_NAVN("saksbehandlerNavn"),
    SAKSBEHANDER_EPOST("saksbehanderEpost"),
    SAKSBEHANDLER_OID("saksbehandlerOid"),
    SAKSBEHANDLER_IDENT("saksbehandlerIdent"),
}

class ContextFactory(
    private val kode7Saksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
    private val riskSaksbehandlergruppe: UUID,
    private val saksbehandlereMedTilgangTilStikkprøve: List<String>
) : GraphQLContextFactory<AuthorizedContext, ApplicationRequest> {

    override suspend fun generateContextMap(request: ApplicationRequest): Map<String, Any> =
        mapOf(
            TILGANGER.key to SaksbehandlerTilganger(
                gruppetilganger = request.getGrupper(),
                saksbehandlerIdent = request.getSaksbehandlerIdent(),
                kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
                riskSaksbehandlergruppe = riskSaksbehandlergruppe,
                beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
                skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
                saksbehandlereMedTilgangTilStikkprøve = saksbehandlereMedTilgangTilStikkprøve
            ),
            SAKSBEHANDLER_NAVN.key to request.getSaksbehandlerName(),
            SAKSBEHANDER_EPOST.key to request.getSaksbehanderEpost(),
            SAKSBEHANDLER_OID.key to request.getSaksbehandlerOid(),
            SAKSBEHANDLER_IDENT.key to request.getSaksbehandlerIdent()
        )

    @Deprecated("The generic context object is deprecated in favor of the context map")
    override suspend fun generateContext(request: ApplicationRequest): AuthorizedContext {
        return AuthorizedContext(request.getGrupper().contains(kode7Saksbehandlergruppe))
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

private fun ApplicationRequest.getSaksbehandlerName(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("name")?.asString() ?: ""
}

private fun ApplicationRequest.getSaksbehanderEpost(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("preferred_username")?.asString() ?: ""
}

private fun ApplicationRequest.getSaksbehandlerOid(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("oid")?.asString() ?: ""
}

private fun ApplicationRequest.getSaksbehandlerIdent(): String {
    val accessToken = call.principal<JWTPrincipal>()
    return accessToken?.payload?.getClaim("NAVident")?.asString() ?: ""
}
