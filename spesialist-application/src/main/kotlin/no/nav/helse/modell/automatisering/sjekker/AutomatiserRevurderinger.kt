package no.nav.helse.modell.automatisering.sjekker

import no.nav.helse.modell.automatisering.AutomatiseringValidering
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.spesialist.application.logg.teamLogs
import java.util.UUID

internal class AutomatiserRevurderinger(
    private val utbetaling: Utbetaling,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
) : AutomatiseringValidering {
    override fun erAutomatiserbar() =
        !utbetaling.erRevurdering() ||
            (utbetaling.refusjonstype() != Refusjonstype.NEGATIVT_BELØP).also {
                if (it) {
                    teamLogs.info(
                        "Revurdering av $vedtaksperiodeId (person $fødselsnummer) har ikke et negativt beløp, og er godkjent for automatisering",
                    )
                }
            }

    override fun årsakTilIkkeAutomatiserbar() = "Utbetalingen er revurdering med negativt beløp"
}
