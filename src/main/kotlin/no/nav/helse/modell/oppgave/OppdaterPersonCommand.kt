package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppdaterPersonCommand(
    private val spleisbehov: Spleisbehov,
    private val personDao: PersonDao
): Command() {
    override var ferdigstilt: LocalDateTime? = null
    override val oppgaver: List<Command> = listOf(
        HentPersoninfoCommand(),
        HentEnhetCommand()
    )

    private inner class HentPersoninfoCommand: Command() {
        override var ferdigstilt: LocalDateTime? = null

        override fun execute() {
            val sistOppdatert = personDao.findPersoninfoSistOppdatert(spleisbehov.fødselsnummer.toLong())
            if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                spleisbehov.håndter(Behovtype.HentPersoninfo)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentPersoninfoLøsning) {
            personDao.updateNavn(spleisbehov.fødselsnummer.toLong(), løsning.fornavn, løsning.mellomnavn, løsning.etternavn)
            personDao.updateEgenskap(spleisbehov.fødselsnummer.toLong(), løsning.egenskap)
            ferdigstilt = LocalDateTime.now()
        }
    }

    private inner class HentEnhetCommand : Command() {
        override var ferdigstilt: LocalDateTime? = null

        override fun execute() {
            val sistOppdatert = personDao.findEnhetSistOppdatert(spleisbehov.fødselsnummer.toLong())
            if (sistOppdatert.plusDays(5) < LocalDate.now()) {
                spleisbehov.håndter(Behovtype.HentEnhet)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentEnhetLøsning) {
            personDao.updateEnhet(spleisbehov.fødselsnummer.toLong(), løsning.enhetNr.toInt())
            ferdigstilt = LocalDateTime.now()
        }
    }


    override fun execute() {
        oppgaver.forEach { it.execute() }
        if (oppgaver.all { it.ferdigstilt != null }) {
            ferdigstilt = LocalDateTime.now()
        }
    }
}
