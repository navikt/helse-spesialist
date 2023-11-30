package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import no.nav.helse.modell.saksbehandler.Saksbehandler

class FjernPåVent(
    private val oppgaveId: Long,
    ) : PåVent {
    override fun loggnavn(): String = "fjern_på_vent"

    override fun oppgaveId(): Long = oppgaveId
    override fun frist(): LocalDate? = null
    override fun skalTildeles(): Boolean? = null
    override fun begrunnelse(): String? = null

    override fun utførAv(saksbehandler: Saksbehandler) {}
}