package no.nav.helse.spesialist.application

import no.nav.helse.db.api.EgenAnsattApiDao

class InMemoryEgenAnsattApiDao : EgenAnsattApiDao {
    override fun erEgenAnsatt(fødselsnummer: String): Boolean? {
        TODO("Not yet implemented")
    }
}
