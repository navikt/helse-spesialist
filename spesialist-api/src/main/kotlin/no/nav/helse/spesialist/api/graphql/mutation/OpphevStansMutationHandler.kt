package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OpphevStans

class OpphevStansMutationHandler(private val saksbehandlerhåndterer: Saksbehandlerhåndterer) : OpphevStansMutationSchema {
    override suspend fun opphevStans(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(ContextValues.SAKSBEHANDLER)
            saksbehandlerhåndterer.håndter(OpphevStans(fodselsnummer, begrunnelse), saksbehandler)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }
}
