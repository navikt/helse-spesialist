package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.LocalDate
import java.time.LocalDateTime

internal class OppdaterPersonCommand(
    private val spleisBehov: SpleisBehov,
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
            val sistOppdatert = personDao.personinfoSistOppdatert(spleisBehov.fødselsnummer.toLong())
            if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                spleisBehov.håndter(Behovtype.HentPersoninfo)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentPersoninfoLøsning) {
            personDao.setNavn(spleisBehov.fødselsnummer.toLong(), løsning.fornavn, løsning.mellomnavn, løsning.etternavn)
            personDao.oppdaterEgenskap(spleisBehov.fødselsnummer.toLong(), løsning.egenskap)
            ferdigstilt = LocalDateTime.now()
        }
    }

    private inner class HentEnhetCommand : Command() {
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
        if (oppgaver.all { it.ferdigstilt != null }) {
            ferdigstilt = LocalDateTime.now()
        }
    }
}
