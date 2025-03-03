package no.nav.helse.modell.oppgave

import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

internal class OppgaveInspektør private constructor(oppgave: Oppgave) {
    internal val tilstand = oppgave.tilstand
    internal val tildelt: Boolean = oppgave.tildeltTil != null
    internal val påVent: Boolean = oppgave.egenskaper.contains(Egenskap.PÅ_VENT)
    internal val tildeltTil: LegacySaksbehandler? = oppgave.tildeltTil
    internal val egenskaper = oppgave.egenskaper

    internal companion object {
        internal fun inspektør(oppgave: Oppgave, block: OppgaveInspektør.() -> Unit) {
            val inspektør = OppgaveInspektør(oppgave)

            block(inspektør)
        }
    }
}
