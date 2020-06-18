package no.nav.helse.modell.risiko

import kotliquery.Session
import no.nav.helse.modell.command.Command
import java.time.Duration
import java.util.*

class VurderRisikoCommand(eventId: UUID, parent: Command, val vedtaksperiodeId: UUID) :
    Command(behovId = eventId, parent = parent, timeout = Duration.ofHours(1)) {
    override fun execute(session: Session): Resultat {
        val risikovurdering = session.hentRisikovurderingForVedtaksperiode(vedtaksperiodeId)

        if (risikovurdering == null || risikovurdering.ufullstendig) {
            return Resultat.HarBehov()
        }

        // TODO: insert warnings

        return Resultat.Ok.System
    }

}
