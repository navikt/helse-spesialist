package no.nav.helse.modell.oppgave

import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto

internal class OppgaveInspector private constructor(oppgaveDto: OppgaveDto) {
    internal val tilstand = oppgaveDto.tilstand
    internal val tildeltTil: SaksbehandlerDto? = oppgaveDto.tildeltTil
    internal val egenskaper = oppgaveDto.egenskaper

    internal companion object {
        internal fun oppgaveinspektør(oppgave: Oppgave, block: OppgaveInspector.() -> Unit) {
            val inspektør = OppgaveInspector(oppgave.toDto())
            inspektør.block()
        }
    }
}
