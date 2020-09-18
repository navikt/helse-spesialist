package no.nav.helse.modell.tildeling

import kotliquery.Session
import no.nav.helse.modell.command.Command
import no.nav.helse.tildeling.ReservasjonDao
import no.nav.helse.tildeling.tildelOppgave
import java.time.Duration
import java.util.*

internal class TildelOppgaveCommand(
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    eventId: UUID,
    parent: Command
) : Command(eventId, parent, Duration.ZERO) {


    override fun execute(session: Session): Resultat {
        val reservasjon = reservasjonDao.hentReservasjonFor(fødselsnummer) ?: return Resultat.Ok.System
        session.tildelOppgave(eventId, reservasjon.saksbehandlerOid, reservasjon.gyldigTil)
        return Resultat.Ok.System
    }
}
