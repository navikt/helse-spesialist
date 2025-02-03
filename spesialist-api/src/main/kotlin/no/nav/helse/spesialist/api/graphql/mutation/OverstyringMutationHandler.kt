package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OverstyringMutationHandler(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : OverstyringMutationSchema {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(OverstyringMutationHandler::class.java)
    }

    override suspend fun overstyrDager(
        overstyring: ApiTidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(overstyring, saksbehandler) }
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke overstyre dager"
                logg.error(feilmelding, e)
                return@withContext lagFeilrespons(feilmelding)
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    override suspend fun overstyrInntektOgRefusjon(
        overstyring: ApiInntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(overstyring, saksbehandler) }
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke overstyre inntekt og refusjon"
                logg.error(feilmelding, e)
                return@withContext lagFeilrespons(feilmelding)
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    override suspend fun overstyrArbeidsforhold(
        overstyring: ApiArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(overstyring, saksbehandler) }
            } catch (e: Exception) {
                val feilmelding = "Kunne ikke overstyre arbeidsforhold"
                logg.error(feilmelding, e)
                return@withContext lagFeilrespons(feilmelding)
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    private fun lagFeilrespons(feilmelding: String): DataFetcherResult<Boolean> =
        DataFetcherResult.newResult<Boolean>().error(
            GraphqlErrorException.newErrorException().message(feilmelding).extensions(mapOf("code" to 500)).build(),
        ).data(false).build()
}
