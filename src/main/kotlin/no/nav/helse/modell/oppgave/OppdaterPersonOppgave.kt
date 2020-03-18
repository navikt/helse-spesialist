package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppdaterPersonOppgave(
    private val spleisBehov: SpleisBehov,
    private val personDao: PersonDao
): Oppgave() {
    override val ferdigstilt: LocalDateTime? = null
    private val oppgaver: List<Oppgave> = listOf(
        HentNavnOppgave(),
        HentEnhetOppgave()
    )

    private inner class HentNavnOppgave: Oppgave() {
        override var ferdigstilt: LocalDateTime? = null

        override fun execute() {
            val sistOppdatert = personDao.navnSistOppdatert(spleisBehov.fødselsnummer.toLong())
            if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                spleisBehov.håndter(Behovtype.HentNavn)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentNavnLøsning) {
            personDao.setNavn(spleisBehov.fødselsnummer.toLong(), løsning.fornavn, løsning.mellomnavn, løsning.etternavn)
            ferdigstilt = LocalDateTime.now()
        }
    }

    private inner class HentEnhetOppgave : Oppgave() {
        override var ferdigstilt: LocalDateTime? = null

        override fun execute() {
            val sistOppdatert = personDao.enhetSistOppdatert(spleisBehov.fødselsnummer.toLong())
            if (sistOppdatert.plusDays(5) < LocalDate.now()) {
                spleisBehov.håndter(Behovtype.HentEnhet)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentEnhetLøsning) {
            personDao.oppdaterEnhet(spleisBehov.fødselsnummer.toLong(), løsning.enhetNr.toInt())
            ferdigstilt = LocalDateTime.now()
        }
    }


    override fun execute() {
        oppgaver.execute()
    }
}
