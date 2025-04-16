package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.saksbehandler
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OverstyringMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : OverstyringMutationSchema {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(OverstyringMutationHandler::class.java)
    }

    override fun overstyrDager(
        overstyring: ApiTidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> {
        val saksbehandler = env.saksbehandler()
        return try {
            saksbehandlerMediator.håndter(overstyring, saksbehandler)
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
    ): DataFetcherResult<Boolean?> {
        val saksbehandler: SaksbehandlerFraApi = env.saksbehandler()
        return try {
            saksbehandlerMediator.håndter(overstyring, saksbehandler)
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
    ): DataFetcherResult<Boolean?> {
        val saksbehandler: SaksbehandlerFraApi = env.saksbehandler()
        return try {
            saksbehandlerMediator.håndter(overstyring, saksbehandler)
            byggRespons(true)
        } catch (e: Exception) {
            val feilmelding = "Kunne ikke overstyre arbeidsforhold"
            logg.error(feilmelding, e)
            lagFeilrespons(feilmelding)
        }
    }

    private fun lagFeilrespons(feilmelding: String): DataFetcherResult<Boolean?> = byggFeilrespons(graphqlErrorException(500, feilmelding))
}
