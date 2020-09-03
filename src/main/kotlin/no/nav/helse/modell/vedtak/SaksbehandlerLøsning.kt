package no.nav.helse.modell.vedtak

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerLøsning(
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val oid: UUID,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?
) {
    fun ferdigstillOppgave(oppgaveDao: OppgaveDao, eventId: UUID, oppgavetype: String, løsning: JsonMessage) {
        oppgaveDao.updateOppgave(
            eventId = eventId,
            oppgavetype = oppgavetype,
            oppgavestatus = Oppgavestatus.Ferdigstilt,
            ferdigstiltAv = epostadresse,
            oid = oid
        )
        løsning["@løsning"] = mapOf(
            "Godkjenning" to mapOf(
                "godkjent" to godkjent,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "årsak" to årsak,
                "begrunnelser" to begrunnelser,
                "kommentar" to kommentar
            ))
    }
}
