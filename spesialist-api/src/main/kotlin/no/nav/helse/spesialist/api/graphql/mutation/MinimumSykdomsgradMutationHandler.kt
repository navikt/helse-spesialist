package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad
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
        if (minimumSykdomsgrad.perioderVurdertOk.isEmpty() && minimumSykdomsgrad.perioderVurdertIkkeOk.isEmpty()) {
            return byggFeilrespons(graphqlErrorException(400, "Mangler vurderte perioder"))
        }

        return try {
            saksbehandlerMediator.håndter(
                handlingFraApi = minimumSykdomsgrad,
                saksbehandlerFraApi = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
                tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
            )
            byggRespons(true)
        } catch (e: Exception) {
            val kunneIkkeVurdereMinimumSykdomsgradError =
                graphqlErrorException(500, "Kunne ikke vurdere minimum sykdomsgrad")
            logg.error(kunneIkkeVurdereMinimumSykdomsgradError.message, e)
            byggFeilrespons(kunneIkkeVurdereMinimumSykdomsgradError)
        }
    }
}
