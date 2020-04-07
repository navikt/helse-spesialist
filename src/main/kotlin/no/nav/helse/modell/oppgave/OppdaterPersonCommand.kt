package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class OppdaterPersonCommand(
    private val spleisbehov: Spleisbehov,
    private val personDao: PersonDao,
    private val fødselsnummer: String,
    behovId: UUID,
    ferdigstilt: LocalDateTime? = null
) : Command(behovId, ferdigstilt, Løsningstype.System) {
    override val oppgaver: List<Command> = listOf(
        HentPersoninfoCommand(),
        HentEnhetCommand()
    )

    private inner class HentPersoninfoCommand(ferdigstilt: LocalDateTime? = null) : Command(behovId, ferdigstilt, Løsningstype.System) {
        override fun execute() {
            val sistOppdatert = personDao.findPersoninfoSistOppdatert(fødselsnummer.toLong())
            if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                spleisbehov.håndter(Behovtype.HentPersoninfo)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentPersoninfoLøsning) {
            personDao.updateNavn(
                fødselsnummer = fødselsnummer.toLong(),
                fornavn = løsning.fornavn,
                mellomnavn = løsning.mellomnavn,
                etternavn = løsning.etternavn
            )
            ferdigstilt = LocalDateTime.now()
        }
    }

    private inner class HentEnhetCommand(ferdigstilt: LocalDateTime? = null) : Command(behovId, ferdigstilt, Løsningstype.System) {
        override fun execute() {
            val sistOppdatert = personDao.findEnhetSistOppdatert(fødselsnummer.toLong())
            if (sistOppdatert.plusDays(5) < LocalDate.now()) {
                spleisbehov.håndter(Behovtype.HentEnhet)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentEnhetLøsning) {
            personDao.updateEnhet(fødselsnummer.toLong(), løsning.enhetNr.toInt())
            ferdigstilt = LocalDateTime.now()
        }
    }


    override fun execute() {
        oppgaver.forEach { it.execute() }
        if (oppgaver.none { it.trengerExecute() }) {
            ferdigstilt = LocalDateTime.now()
        }
    }
}
