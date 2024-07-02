package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.time.LocalDate

sealed class PåVent(val oppgaveId: Long) : Handling

class LeggPåVent(
    oppgaveId: Long,
    val frist: LocalDate,
    val skalTildeles: Boolean,
    val begrunnelse: String?,
    val notatTekst: String,
) : PåVent(oppgaveId) {
    override fun loggnavn(): String = "lagt_på_vent"

    override fun utførAv(saksbehandler: Saksbehandler) {}
}

class FjernPåVent(oppgaveId: Long) : PåVent(oppgaveId) {
    override fun loggnavn(): String = "fjern_på_vent"

    override fun utførAv(saksbehandler: Saksbehandler) {}
}
