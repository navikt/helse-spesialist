package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData

class AnnulleringMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : AnnulleringMutationSchema {
    override fun annuller(
        annullering: ApiAnnulleringData,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> {
        saksbehandlerMediator.håndter(
            handlingFraApi = annullering,
            saksbehandler = env.graphQlContext.get(ContextValues.SAKSBEHANDLER),
            tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
        )

        return byggRespons(true)
    }
}
