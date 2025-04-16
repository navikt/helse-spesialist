package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.logg.logg

class SkjonnsfastsettelseMutationHandler(private val saksbehandlerMediator: SaksbehandlerMediator) :
    SkjonnsfastsettelseMutationSchema {
    override fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: ApiSkjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
        return try {
            saksbehandlerMediator.håndter(skjonnsfastsettelse, saksbehandler)
            byggRespons(true)
        } catch (e: Exception) {
            val feilmelding = "Kunne ikke skjønnsfastsette sykepengegrunnlag"
            logg.error(feilmelding, e)
            byggFeilrespons(graphqlErrorException(500, feilmelding))
        }
    }
}
