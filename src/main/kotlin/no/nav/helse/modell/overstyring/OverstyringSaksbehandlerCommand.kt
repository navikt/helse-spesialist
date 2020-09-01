package no.nav.helse.modell.overstyring

import kotliquery.Session
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import no.nav.helse.modell.command.MacroCommand
import no.nav.helse.modell.saksbehandler.OpprettSaksbehandlerCommand
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Duration
import java.util.*

class OverstyringSaksbehandlerCommand(
    eventId: UUID,
    rapidsConnection: RapidsConnection,
    oid: UUID,
    navn: String,
    epost: String,
    override val fødselsnummer: String,
    override val orgnummer: String
) : MacroCommand(eventId, Duration.ofDays(1)) {
    override fun execute(session: Session): Resultat {
        return Resultat.Ok.System
    }

    override val vedtaksperiodeId: UUID? = null

    override fun toJson(): String {
        return "{}"
    }

    override val oppgaver: Set<Command> =
        setOf(OpprettSaksbehandlerCommand(eventId, oid, navn, epost, this), OverstyringCommand(eventId, this, rapidsConnection))
}
