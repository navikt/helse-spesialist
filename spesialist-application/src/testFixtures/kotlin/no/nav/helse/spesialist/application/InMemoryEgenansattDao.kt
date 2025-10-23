package no.nav.helse.spesialist.application

import no.nav.helse.db.EgenAnsattDao
import java.time.LocalDateTime

class InMemoryEgenansattDao: EgenAnsattDao {
    private val personer = mutableMapOf<String, Boolean>()

    override fun erEgenAnsatt(fødselsnummer: String): Boolean? {
        return personer[fødselsnummer]
    }

    override fun lagre(fødselsnummer: String, erEgenAnsatt: Boolean, opprettet: LocalDateTime) {
        personer[fødselsnummer] = erEgenAnsatt
    }
}
