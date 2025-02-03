package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Personhåndterer

class PersonMutationHandler(
    private val personhåndterer: Personhåndterer,
) : PersonMutationSchema {
    override suspend fun oppdaterPerson(fodselsnummer: String): DataFetcherResult<Boolean> =
        withContext(Dispatchers.IO) {
            personhåndterer.oppdaterSnapshot(fodselsnummer)
            DataFetcherResult.newResult<Boolean>().data(true).build()
        }
}
