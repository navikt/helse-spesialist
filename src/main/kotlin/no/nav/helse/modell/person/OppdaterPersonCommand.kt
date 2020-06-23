package no.nav.helse.modell.person

import kotliquery.Session
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.command.Command
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class OppdaterPersonCommand(
    private val personDao: PersonDao,
    private val fødselsnummer: String,
    eventId: UUID,
    parent: Command
) : Command(
    eventId = eventId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override val oppgaver: Set<Command> = setOf(
        HentPersoninfoCommand(),
        HentEnhetCommand(),
        HentInfotrygdutbetalingerCommand()
    )

    private inner class HentPersoninfoCommand : Command(
        eventId = eventId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(session: Session): Resultat {
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
        eventId = eventId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(session: Session): Resultat {
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

    private inner class HentInfotrygdutbetalingerCommand : Command(
        eventId = eventId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(session: Session): Resultat {
            val sistOppdatert = personDao.findITUtbetalingsperioderSistOppdatert(fødselsnummer.toLong())
            return if (sistOppdatert.plusDays(1) < LocalDate.now()) {
                Resultat.HarBehov(Behovtype.HentInfotrygdutbetalinger())
            } else {
                Resultat.Ok.System
            }
        }

        override fun fortsett(løsning: HentInfotrygdutbetalingerLøsning) {
            if (personDao.findInfotrygdutbetalinger(fødselsnummer.toLong()) != null) {
                personDao.updateInfotrygdutbetalinger(fødselsnummer.toLong(), løsning.utbetalinger)
            } else {
                val utbetalingRef = personDao.insertInfotrygdutbetalinger(løsning.utbetalinger)
                personDao.updateInfotrygdutbetalingerRef(fødselsnummer.toLong(), utbetalingRef)
            }
        }
    }

    override fun execute(session: Session) = Resultat.Ok.System
}
