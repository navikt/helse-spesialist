package no.nav.helse.modell.oppgave

import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto

internal class OppgaveInspektør private constructor(oppgaveDto: OppgaveDto) {
    internal val tilstand = oppgaveDto.tilstand
    internal val tildelt: Boolean = oppgaveDto.tildeltTil != null
    internal val påVent: Boolean = oppgaveDto.egenskaper.contains(EgenskapDto.PÅ_VENT)
    internal val tildeltTil: SaksbehandlerDto? = oppgaveDto.tildeltTil
    internal val egenskaper = oppgaveDto.egenskaper

    internal companion object {
        internal fun inspektør(oppgave: Oppgave, block: OppgaveInspektør.() -> Unit) {
            val inspektør = OppgaveInspektør(oppgave.toDto())

            block(inspektør)
        }
    }
}
