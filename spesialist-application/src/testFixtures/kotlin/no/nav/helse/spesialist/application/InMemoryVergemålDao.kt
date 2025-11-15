package no.nav.helse.spesialist.application

import no.nav.helse.db.VergemålDao
import no.nav.helse.db.VergemålOgFremtidsfullmakt

class InMemoryVergemålDao : VergemålDao {
    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun harVergemål(fødselsnummer: String): Boolean? {
        TODO("Not yet implemented")
    }

    override fun harFullmakt(fødselsnummer: String): Boolean? {
        TODO("Not yet implemented")
    }
}
