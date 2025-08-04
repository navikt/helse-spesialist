package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MinimumSykdomsgradMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : MinimumSykdomsgradMutationSchema {
    private companion object {
        private val logg: Logger = LoggerFactory.getLogger(MinimumSykdomsgradMutation::class.java)
    }

    override fun minimumSykdomsgrad(
        minimumSykdomsgrad: ApiMinimumSykdomsgrad,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?> {
        val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(SAKSBEHANDLER)
        if (minimumSykdomsgrad.perioderVurdertOk.isEmpty() && minimumSykdomsgrad.perioderVurdertIkkeOk.isEmpty()) {
            return byggFeilrespons(graphqlErrorException(400, "Mangler vurderte perioder"))
        }

        return try {
            saksbehandlerMediator.håndter(minimumSykdomsgrad, saksbehandler)
            byggRespons(true)
        } catch (e: Exception) {
            val kunneIkkeVurdereMinimumSykdomsgradError =
                graphqlErrorException(500, "Kunne ikke vurdere minimum sykdomsgrad")
            logg.error(kunneIkkeVurdereMinimumSykdomsgradError.message, e)
            byggFeilrespons(kunneIkkeVurdereMinimumSykdomsgradError)
        }
    }
}
