package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.medMdc

class OverstyringMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : OverstyringMutationSchema {
    override fun overstyrDager(
        overstyring: ApiTidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        medMdc(
            MdcKey.VEDTAKSPERIODE_ID to overstyring.vedtaksperiodeId.toString(),
            MdcKey.IDENTITETSNUMMER to overstyring.fodselsnummer,
        ) {
            try {
                saksbehandlerMediator.håndter(
                    handlingFraApi = overstyring,
                    saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                    brukerroller = env.graphQlContext.get(ContextValues.BRUKERROLLER),
                )
                byggRespons(true)
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke overstyre dager"
                logg.error(feilmelding, e)
                lagFeilrespons(feilmelding)
            }
        }

    override fun overstyrInntektOgRefusjon(
        overstyring: ApiInntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        medMdc(
            MdcKey.VEDTAKSPERIODE_ID to overstyring.vedtaksperiodeId.toString(),
            MdcKey.IDENTITETSNUMMER to overstyring.fodselsnummer,
        ) {
            try {
                saksbehandlerMediator.håndter(
                    handlingFraApi = overstyring,
                    saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                    brukerroller = env.graphQlContext.get(ContextValues.BRUKERROLLER),
                )
                byggRespons(true)
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke overstyre inntekt og refusjon"
                logg.error(feilmelding, e)
                lagFeilrespons(feilmelding)
            }
        }

    override fun overstyrArbeidsforhold(
        overstyring: ApiArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> =
        medMdc(
            MdcKey.VEDTAKSPERIODE_ID to overstyring.vedtaksperiodeId.toString(),
            MdcKey.IDENTITETSNUMMER to overstyring.fodselsnummer,
        ) {
            try {
                saksbehandlerMediator.håndter(
                    handlingFraApi = overstyring,
                    saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                    brukerroller = env.graphQlContext.get(ContextValues.BRUKERROLLER),
                )
                byggRespons(true)
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke overstyre arbeidsforhold"
                logg.error(feilmelding, e)
                lagFeilrespons(feilmelding)
            }
        }

    private fun lagFeilrespons(feilmelding: String): DataFetcherResult<Boolean?> = byggFeilrespons(graphqlErrorException(500, feilmelding))
}
