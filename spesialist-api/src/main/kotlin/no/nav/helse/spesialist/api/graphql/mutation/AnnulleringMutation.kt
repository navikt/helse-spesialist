package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.schema.AnnulleringData
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandlingFraApi

class AnnulleringMutation(
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
) : Mutation {
    @Suppress("unused")
    suspend fun annuller(
        annullering: AnnulleringData,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            val saksbehandler: Lazy<SaksbehandlerFraApi> = env.graphQlContext.get(ContextValues.SAKSBEHANDLER.key)
            val annulleringHandling =
                AnnulleringHandlingFraApi(
                    aktørId = annullering.aktorId,
                    fødselsnummer = annullering.fodselsnummer,
                    organisasjonsnummer = annullering.organisasjonsnummer ?: "",
                    fagsystemId = annullering.fagsystemId,
                    utbetalingId = annullering.utbetalingId,
                    begrunnelser = annullering.begrunnelser,
                    kommentar = annullering.kommentar,
                )

            saksbehandlerhåndterer.håndter(annulleringHandling, saksbehandler.value)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }
}
