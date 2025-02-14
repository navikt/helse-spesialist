package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.EgenAnsattDao
import java.time.LocalDateTime

class EgenAnsattløsning(
    private val opprettet: LocalDateTime,
    private val fødselsnummer: String,
    private val erEgenAnsatt: Boolean,
) {
    internal fun lagre(egenAnsattDao: EgenAnsattDao) {
        egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, opprettet)
    }
}
