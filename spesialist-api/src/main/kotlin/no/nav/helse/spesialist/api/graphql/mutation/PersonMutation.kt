package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Personhåndterer

class PersonMutation(
    private val personhåndterer: Personhåndterer,
) : Mutation {

    @Suppress("unused")
    suspend fun oppdaterPerson(
        fodselsnummer: String
    ): DataFetcherResult<Boolean> = withContext(Dispatchers.IO) {
        personhåndterer.oppdaterSnapshot(fodselsnummer)
        DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}