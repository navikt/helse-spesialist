package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.LagtPåVentEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.time.LocalDate
import java.util.UUID

sealed class PåVent(
    val oppgaveId: Long,
) : Handling

class LeggPåVent(
    oppgaveId: Long,
    val fødselsnummer: String,
    val frist: LocalDate,
    val behandlingId: UUID,
    val skalTildeles: Boolean,
    val notatTekst: String?,
    val årsaker: List<PåVentÅrsak>,
) : PåVent(oppgaveId) {
    override fun loggnavn(): String = "lagt_på_vent"

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    internal fun byggEvent(
        oid: UUID,
        ident: String,
    ): LagtPåVentEvent =
        LagtPåVentEvent(
            fødselsnummer = fødselsnummer,
            oppgaveId = oppgaveId,
            behandlingId = behandlingId,
            skalTildeles = skalTildeles,
            frist = frist,
            notatTekst = notatTekst,
            årsaker = årsaker,
            saksbehandlerOid = oid,
            saksbehandlerIdent = ident,
        )
}

class FjernPåVent(
    oppgaveId: Long,
) : PåVent(oppgaveId) {
    override fun loggnavn(): String = "fjern_på_vent"

    override fun utførAv(saksbehandler: Saksbehandler) {}
}

class FjernPåVentUtenHistorikkinnslag(
    oppgaveId: Long,
) : PåVent(oppgaveId) {
    override fun loggnavn(): String = "fjern_på_vent_uten_historikkinnslag"

    override fun utførAv(saksbehandler: Saksbehandler) {}
}

data class PåVentÅrsak(
    val key: String,
    val årsak: String,
)
