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
import no.nav.helse.spesialist.api.graphql.schema.Skjonnsfastsettelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SkjonnsfastsettelseMutation(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : Mutation {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(SkjonnsfastsettelseMutation::class.java)
    }

    @Suppress("unused")
    suspend fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: Skjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
            try {
                withContext(Dispatchers.IO) { saksbehandlerhåndterer.håndter(skjonnsfastsettelse, saksbehandler) }
            } catch (e: Exception) {
                val kunneIkkeSkjønnsfastsetteSykepengegrunnlagError = kunneIkkeSkjønnsfastsetteSykepengegrunnlagError()
                logg.error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError.message, e)
                return@withContext DataFetcherResult.newResult<Boolean>()
                    .error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError)
                    .build()
            }
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }

    private fun kunneIkkeSkjønnsfastsetteSykepengegrunnlagError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke skjønnsfastsette sykepengegrunnlag")
            .extensions(mapOf("code" to 500)).build()
}
