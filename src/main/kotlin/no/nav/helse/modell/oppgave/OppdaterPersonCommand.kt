package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class OppdaterPersonCommand(
    private val personDao: PersonDao,
    private val fødselsnummer: String,
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override val oppgaver: Set<Command> = setOf(
        HentPersoninfoCommand(),
        HentEnhetCommand()
    )

    private inner class HentPersoninfoCommand : Command(
        behovId = behovId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(): Resultat {
            val sistOppdatert = personDao.findPersoninfoSistOppdatert(fødselsnummer.toLong())
            return if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                Resultat.HarBehov(Behovtype.HentPersoninfo)
            } else {
                Resultat.Ok.System
            }
        }

        override fun fortsett(løsning: HentPersoninfoLøsning) {
            personDao.updateNavn(
                fødselsnummer = fødselsnummer.toLong(),
                fornavn = løsning.fornavn,
                mellomnavn = løsning.mellomnavn,
                etternavn = løsning.etternavn
            )
        }
    }

    private inner class HentEnhetCommand : Command(
        behovId = behovId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(): Resultat {
            val sistOppdatert = personDao.findEnhetSistOppdatert(fødselsnummer.toLong())
            return if (sistOppdatert.plusDays(5) < LocalDate.now()) {
                Resultat.HarBehov(Behovtype.HentEnhet)
            } else {
                Resultat.Ok.System
            }
        }

        override fun fortsett(løsning: HentEnhetLøsning) {
            personDao.updateEnhet(fødselsnummer.toLong(), løsning.enhetNr.toInt())
        }
    }

    override fun execute() = Resultat.Ok.System
}
