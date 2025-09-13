package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.ktor.KtorGraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGSGRUPPER
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
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
        val jwt =
            (
                request.call.principal<JWTPrincipal>() ?: this.run {
                    sikkerlogg.error("Ingen access_token for graphql-kall")
                    return emptyMap<Any, Any>().toGraphQLContext()
                }
            ).payload

        return mapOf(
            TILGANGSGRUPPER to
                this.tilgangsgrupper.grupperFor(
                    jwt
                        .getClaim("groups")
                        ?.asList(String::class.java)
                        ?.map(UUID::fromString)
                        .orEmpty(),
                ),
            SAKSBEHANDLER to
                Saksbehandler(
                    id =
                        SaksbehandlerOid(
                            value =
                                jwt
                                    .getClaim("oid")
                                    .asString()
                                    .let(UUID::fromString),
                        ),
                    navn = jwt.getClaim("name").asString(),
                    epost = jwt.getClaim("preferred_username").asString(),
                    ident = jwt.getClaim("NAVident").asString(),
                ),
        ).toGraphQLContext()
    }
}
