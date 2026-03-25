package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper

sealed class PåVent(
    val oppgaveId: Long,
) : Handling

class FjernPåVentUtenHistorikkinnslag(
    oppgaveId: Long,
) : PåVent(oppgaveId) {
    override fun loggnavn(): String = "fjern_på_vent_uten_historikkinnslag"

    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {}
}

data class PåVentÅrsak(
    val key: String,
    val årsak: String,
)
