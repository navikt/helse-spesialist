package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MinimumSykdomsgradMutationHandler(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) :
    MinimumSykdomsgradMutationSchema {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(MinimumSykdomsgradMutation::class.java)
    }

    override fun minimumSykdomsgrad(
        minimumSykdomsgrad: ApiMinimumSykdomsgrad,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
        if (minimumSykdomsgrad.perioderVurdertOk.isEmpty() && minimumSykdomsgrad.perioderVurdertIkkeOk.isEmpty()) {
            return lagErrorRespons(manglerVurdertePerioderError())
        }

        return try {
            saksbehandlerhåndterer.håndter(minimumSykdomsgrad, saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        } catch (e: Exception) {
            val kunneIkkeVurdereMinimumSykdomsgradError = kunneIkkeVurdereMinimumSykdomsgradError()
            logg.error(kunneIkkeVurdereMinimumSykdomsgradError.message, e)
            lagErrorRespons(kunneIkkeVurdereMinimumSykdomsgradError)
        }
    }

    private fun manglerVurdertePerioderError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Mangler vurderte perioder")
            .extensions(mapOf("code" to 400)).build()

    private fun kunneIkkeVurdereMinimumSykdomsgradError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Kunne ikke vurdere minimum sykdomsgrad")
            .extensions(mapOf("code" to 500)).build()

    private fun lagErrorRespons(error: GraphQLError): DataFetcherResult<Boolean> =
        DataFetcherResult.newResult<Boolean>()
            .error(error)
            .data(false)
            .build()
}
