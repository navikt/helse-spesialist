package no.nav.helse.modell.person

import kotliquery.Session
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class OppdaterPersonCommand(
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
            val sistOppdatert = session.findPersoninfoSistOppdatert(fødselsnummer)
            return if (sistOppdatert.plusDays(14) < LocalDate.now()) {
                Resultat.HarBehov(Behovtype.HentPersoninfo)
            } else {
                Resultat.Ok.System
            }
        }

        override fun resume(session: Session, løsninger: Løsninger) {
            val løsning = løsninger.løsning<HentPersoninfoLøsning>()
            session.updatePersoninfo(
                fødselsnummer = fødselsnummer,
                fornavn = løsning.fornavn,
                mellomnavn = løsning.mellomnavn,
                etternavn = løsning.etternavn,
                fødselsdato = løsning.fødselsdato,
                kjønn = løsning.kjønn
            )
        }
    }

    private inner class HentEnhetCommand : Command(
        eventId = eventId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(session: Session): Resultat {
            val sistOppdatert = session.findEnhetSistOppdatert(fødselsnummer)
            return if (sistOppdatert.plusDays(5) < LocalDate.now()) {
                Resultat.HarBehov(Behovtype.HentEnhet)
            } else {
                Resultat.Ok.System
            }
        }

        override fun resume(session: Session, løsninger: Løsninger) {
            val løsning = løsninger.løsning<HentEnhetLøsning>()
            session.updateEnhet(fødselsnummer, løsning.enhetNr.toInt())
        }
    }

    private inner class HentInfotrygdutbetalingerCommand : Command(
        eventId = eventId,
        parent = this@OppdaterPersonCommand,
        timeout = Duration.ofHours(1)
    ) {
        override fun execute(session: Session): Resultat {
            val sistOppdatert = session.findITUtbetalingsperioderSistOppdatert(fødselsnummer)
            return if (sistOppdatert.plusDays(1) < LocalDate.now()) {
                Resultat.HarBehov(Behovtype.HentInfotrygdutbetalinger())
            } else {
                Resultat.Ok.System
            }
        }

        override fun resume(session: Session, løsninger: Løsninger) {
            val løsning = løsninger.løsning<HentInfotrygdutbetalingerLøsning>()
            if (session.findInfotrygdutbetalinger(fødselsnummer) != null) {
                session.updateInfotrygdutbetalinger(fødselsnummer, løsning.utbetalinger)
            } else {
                val utbetalingRef = session.insertInfotrygdutbetalinger(løsning.utbetalinger)
                session.updateInfotrygdutbetalingerRef(fødselsnummer, utbetalingRef)
            }
        }
    }

    override fun execute(session: Session) = Resultat.Ok.System
}
