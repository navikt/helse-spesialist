package no.nav.helse.spesialist.application.modell

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.oppgave.OppgaveDto

internal class OppgaveInspector private constructor(oppgaveDto: OppgaveDto) {
    internal val tilstand = oppgaveDto.tilstand

    internal companion object {
        internal fun oppgaveinspektør(oppgave: Oppgave, block: OppgaveInspector.() -> Unit) {
            val inspektør = OppgaveInspector(oppgave.toDto())
            inspektør.block()
        }
    }
}
