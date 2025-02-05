package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SkjonnsfastsettelseMutationHandler(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) :
    SkjonnsfastsettelseMutationSchema {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(SkjonnsfastsettelseMutationHandler::class.java)
    }

    override fun skjonnsfastsettSykepengegrunnlag(
        skjonnsfastsettelse: ApiSkjonnsfastsettelse,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
        return try {
            saksbehandlerhåndterer.håndter(skjonnsfastsettelse, saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        } catch (e: Exception) {
            val kunneIkkeSkjønnsfastsetteSykepengegrunnlagError = kunneIkkeSkjønnsfastsetteSykepengegrunnlagError()
            logg.error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError.message, e)
            DataFetcherResult.newResult<Boolean>()
                .error(kunneIkkeSkjønnsfastsetteSykepengegrunnlagError)
                .data(false)
                .build()
        }
    }

    private fun kunneIkkeSkjønnsfastsetteSykepengegrunnlagError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke skjønnsfastsette sykepengegrunnlag")
            .extensions(mapOf("code" to 500)).build()
}
