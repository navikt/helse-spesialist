package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPersoninfo

class InMemoryPersoninfoHenter(
    var personinfo: Personinfo? = lagPersoninfo(),
) : PersoninfoHenter {
    var feil: Exception? = null

    override fun hentPersoninfo(identitetsnummer: Identitetsnummer): Personinfo? {
        feil?.let { throw it }
        return personinfo
    }
}
