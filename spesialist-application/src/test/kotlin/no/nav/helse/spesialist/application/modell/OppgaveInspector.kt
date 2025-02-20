package no.nav.helse.spesialist.application.modell

import no.nav.helse.modell.oppgave.Oppgave

internal class OppgaveInspector private constructor(oppgave: Oppgave) {
    internal val tilstand = oppgave.tilstand

    internal companion object {
        internal fun oppgaveinspektør(oppgave: Oppgave, block: OppgaveInspector.() -> Unit) {
            val inspektør = OppgaveInspector(oppgave)
            inspektør.block()
        }
    }
}
