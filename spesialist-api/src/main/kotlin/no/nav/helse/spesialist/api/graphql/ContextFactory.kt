package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.generator.extensions.toGraphQLContext
import com.expediagroup.graphql.server.ktor.KtorGraphQLContextFactory
import graphql.GraphQLContext
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.helse.spesialist.api.SaksbehandlerPrincipal
import no.nav.helse.spesialist.api.graphql.ContextValues.BRUKERROLLER
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGSGRUPPER
import no.nav.helse.spesialist.application.logg.loggErrorWithNoThrowable

enum class ContextValues {
    TILGANGSGRUPPER,
    BRUKERROLLER,
    SAKSBEHANDLER,
}

class ContextFactory : KtorGraphQLContextFactory() {
    override suspend fun generateContext(request: ApplicationRequest): GraphQLContext {
        val principal =
            request.call.principal<SaksbehandlerPrincipal>() ?: run {
                loggErrorWithNoThrowable("Ingen access_token for graphql-kall")
                return emptyMap<Any, Any>().toGraphQLContext()
            }

        return mapOf(
            TILGANGSGRUPPER to principal.tilgangsgrupper,
            SAKSBEHANDLER to principal.saksbehandler,
            BRUKERROLLER to principal.brukerroller,
        ).toGraphQLContext()
    }
}
