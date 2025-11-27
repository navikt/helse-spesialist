package no.nav.helse.spesialist.api.graphql

import com.auth0.jwt.interfaces.Payload
import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.ktor.KtorGraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGSGRUPPER
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

enum class ContextValues {
    TILGANGSGRUPPER,
    SAKSBEHANDLER,
}

class ContextFactory(
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
) : KtorGraphQLContextFactory() {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        val jwt =
            (
                request.call.principal<JWTPrincipal>() ?: this.run {
                    sikkerlogg.error("Ingen access_token for graphql-kall")
                    return emptyMap<Any, Any>().toGraphQLContext()
                }
            ).payload

        return mapOf(
            TILGANGSGRUPPER to tilgangsgruppeUuider.grupperFor(jwt.gruppeUuider()),
            SAKSBEHANDLER to jwt.tilSaksbehandler(),
        ).toGraphQLContext()
    }

    companion object {
        fun Payload.gruppeUuider(): List<UUID> =
            getClaim("groups")
                ?.asList(String::class.java)
                ?.map(UUID::fromString)
                .orEmpty()

        fun Payload.tilSaksbehandler(): Saksbehandler =
            Saksbehandler(
                id =
                    SaksbehandlerOid(
                        value =
                            getClaim("oid")
                                .asString()
                                .let(UUID::fromString),
                    ),
                navn = getClaim("name").asString(),
                epost = getClaim("preferred_username").asString(),
                ident = getClaim("NAVident").asString(),
            )
    }
}
