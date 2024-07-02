package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.InntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.TidslinjeOverstyring
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OverstyringMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(OverstyringMutation::class.java)
    }

    @Suppress("unused")
    suspend fun overstyrDager(
        overstyring: TidslinjeOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER.key)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(overstyring, saksbehandler) }
            } catch (e: Exception) {
                val kunneIkkeOverstyreError = kunneIkkeOverstyreError("dager")
                logg.error(kunneIkkeOverstyreError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>().error(kunneIkkeOverstyreError).build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    @Suppress("unused")
    suspend fun overstyrInntektOgRefusjon(
        overstyring: InntektOgRefusjonOverstyring,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER.key)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(overstyring, saksbehandler) }
            } catch (e: Exception) {
                val kunneIkkeOverstyreError = kunneIkkeOverstyreError("inntekt og refusjon")
                logg.error(kunneIkkeOverstyreError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>()
                    .error(kunneIkkeOverstyreError).build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    @Suppress("unused")
    suspend fun overstyrArbeidsforhold(
        overstyring: ArbeidsforholdOverstyringHandling,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER.key)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(overstyring, saksbehandler) }
            } catch (e: Exception) {
                val kunneIkkeOverstyreError = kunneIkkeOverstyreError("arbeidsforhold")
                logg.error(kunneIkkeOverstyreError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>().error(kunneIkkeOverstyreError)
                    .build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    private fun kunneIkkeOverstyreError(overstyring: String): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke overstyre $overstyring")
            .extensions(mapOf("code" to 500)).build()
}
