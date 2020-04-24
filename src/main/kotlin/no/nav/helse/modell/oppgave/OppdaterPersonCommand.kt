package no.nav.helse.modell.oppgave

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

internal class OppdaterPersonCommand(
    private val spleisbehov: Spleisbehov,
    private val personDao: PersonDao,
    private val fødselsnummer: String,
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    initiellStatus = Oppgavestatus.AvventerSystem,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override val oppgaver: Set<Command> = setOf(
        HentPersoninfoCommand(),
        HentEnhetCommand()
    )

    private inner class HentPersoninfoCommand : Command(
        behovId = behovId,
        initiellStatus = Oppgavestatus.AvventerSystem,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute() {
            val sistOppdatert = personDao.findPersoninfoSistOppdatert(fødselsnummer.toLong())
            if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                spleisbehov.håndter(Behovtype.HentPersoninfo)
            } else {
                ferdigstillSystem()
            }
        }

        override fun fortsett(løsning: HentPersoninfoLøsning) {
            personDao.updateNavn(
                fødselsnummer = fødselsnummer.toLong(),
                fornavn = løsning.fornavn,
                mellomnavn = løsning.mellomnavn,
                etternavn = løsning.etternavn
            )
            ferdigstillSystem()
        }
    }

    private inner class HentEnhetCommand : Command(
        behovId = behovId,
        initiellStatus = Oppgavestatus.AvventerSystem,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute() {
            val sistOppdatert = personDao.findEnhetSistOppdatert(fødselsnummer.toLong())
            if (sistOppdatert.plusDays(5) < LocalDate.now()) {
                spleisbehov.håndter(Behovtype.HentEnhet)
            } else {
                ferdigstillSystem()
            }
        }

        override fun fortsett(løsning: HentEnhetLøsning) {
            personDao.updateEnhet(fødselsnummer.toLong(), løsning.enhetNr.toInt())
            ferdigstillSystem()
        }
    }

    override fun execute() {
        oppgaver.forEach { it.execute() }
        if (oppgaver.none { it.trengerExecute() }) {
            ferdigstillSystem()
        }
    }
}
