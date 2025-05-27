package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.graphql.byggRespons

class PersonMutationHandler(
    private val personhåndterer: Personhåndterer,
) : PersonMutationSchema {
    override fun oppdaterPerson(fodselsnummer: String): DataFetcherResult<Boolean> {
        personhåndterer.oppdaterPersondata(fodselsnummer)
        return byggRespons(true)
    }
}
