package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

class AnnulleringMutationHandler(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
) : AnnulleringMutationSchema {
    override suspend fun annuller(
        annullering: ApiAnnulleringData,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: SaksbehandlerFraApi = env.graphQlContext.get(ContextValues.SAKSBEHANDLER)

            saksbehandlerhåndterer.håndter(annullering, saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }
}
