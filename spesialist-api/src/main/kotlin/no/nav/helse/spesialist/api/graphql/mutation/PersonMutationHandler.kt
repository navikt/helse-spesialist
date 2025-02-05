package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.Personhåndterer

class PersonMutationHandler(
    private val personhåndterer: Personhåndterer,
) : PersonMutationSchema {
    override fun oppdaterPerson(fodselsnummer: String): DataFetcherResult<Boolean> {
        personhåndterer.oppdaterSnapshot(fodselsnummer)
        return DataFetcherResult.newResult<Boolean>().data(true).build()
    }
}
