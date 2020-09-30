package no.nav.helse.modell.command

import kotliquery.Session
import java.time.Duration
import java.util.*

class InvaliderSaksbehandlerOppgaveCommand(
    private val fødselsnummer: String,
    private val orgnummer: String,
    eventId: UUID
) : Command(eventId, null, Duration.ZERO) {
    override fun execute(session: Session): Resultat {
        session.invaliderSaksbehandleroppgaver(fødselsnummer, orgnummer)
        return Resultat.Ok.System
    }
}
