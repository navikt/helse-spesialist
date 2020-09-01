package no.nav.helse.modell.saksbehandler

import kotliquery.Session
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.util.*

internal class OpprettSaksbehandlerCommand (
    eventId: UUID,
    val oid: UUID,
    val navn: String,
    val epost: String,
    parent: Command
) : Command(eventId, parent, Duration.ZERO) {

    override fun execute(session: Session): Resultat {
        session.persisterSaksbehandler(oid, navn, epost)

        return Resultat.Ok.System
    }
}
