package no.nav.helse.modell.vedtak

import kotliquery.Session
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.util.*

internal class SaksbehandlerGodkjenningCommand(
    eventId: UUID,
    parent: Command
) : Command(
    eventId = eventId,
    parent = parent,
    timeout = Duration.ofDays(14)
) {
    private var resultat: Resultat = Resultat.TrengerSaksbehandlerInput
    override fun execute(session: Session) = resultat
    override fun resume(session: Session, løsninger: Løsninger) {
        val løsning = løsninger.løsning<SaksbehandlerLøsning>()
        resultat = Resultat.Ok.Løst(
            ferdigstiltAv = løsning.epostadresse,
            oid = løsning.oid,
            løsning = mapOf(
                "Godkjenning" to mapOf(
                    "godkjent" to løsning.godkjent,
                    "saksbehandlerIdent" to løsning.saksbehandlerIdent,
                    "godkjenttidspunkt" to løsning.godkjenttidspunkt,
                    "årsak" to løsning.årsak,
                    "begrunnelser" to løsning.begrunnelser,
                    "kommentar" to løsning.kommentar
                )
            )
        )
    }
}
