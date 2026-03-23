package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.medMdc

class SkjonnsfastsettelseMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : SkjonnsfastsettelseMutationSchema {
    override fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: ApiSkjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        medMdc(
            MdcKey.VEDTAKSPERIODE_ID to skjonnsfastsettelse.vedtaksperiodeId.toString(),
            MdcKey.IDENTITETSNUMMER to skjonnsfastsettelse.fodselsnummer,
        ) {
            try {
                saksbehandlerMediator.håndter(
                    handlingFraApi = skjonnsfastsettelse,
                    saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                    brukerroller = env.graphQlContext.get(ContextValues.BRUKERROLLER),
                )
                byggRespons(true)
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke skjønnsfastsette sykepengegrunnlag"
                logg.error(feilmelding, e)
                byggFeilrespons(graphqlErrorException(500, feilmelding))
            }
        }
}
