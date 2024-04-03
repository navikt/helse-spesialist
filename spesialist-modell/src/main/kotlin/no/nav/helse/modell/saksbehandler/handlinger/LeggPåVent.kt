package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.time.LocalDate

class LeggPåVent(
    private val oppgaveId: Long,
    private val frist: LocalDate,
    private val skalTildeles: Boolean,
    private val begrunnelse: String?,
) : PåVent {
    override fun loggnavn(): String = "lagt_på_vent"

    override fun oppgaveId(): Long = oppgaveId

    override fun frist(): LocalDate = frist

    override fun skalTildeles(): Boolean = skalTildeles

    override fun begrunnelse(): String? = begrunnelse

    override fun utførAv(saksbehandler: Saksbehandler) {}
}
