package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.medMdc

class PersonMutationHandler(
    private val personhåndterer: Personhåndterer,
) : PersonMutationSchema {
    override fun oppdaterPerson(fodselsnummer: String): DataFetcherResult<Boolean> =
        medMdc(MdcKey.IDENTITETSNUMMER to fodselsnummer) {
            personhåndterer.oppdaterPersondata(fodselsnummer)
            byggRespons(true)
        }
}
