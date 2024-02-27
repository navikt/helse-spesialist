package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

internal class OppgaveInspector private constructor() : OppgaveVisitor {
    internal lateinit var tilstand: Oppgave.Tilstand
    internal lateinit var egenskap: Egenskap
    internal var tildeltTil: Saksbehandler? = null
    internal val egenskaper = mutableListOf<Egenskap>()
    override fun visitOppgave(
        id: Long,
        egenskap: Egenskap,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Egenskap>,
        tildelt: Saksbehandler?,
        kanAvvises: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {
        this.tilstand = tilstand
        this.tildeltTil = tildelt
        this.egenskap = egenskap
        this.egenskaper.addAll(egenskaper)
    }

    internal companion object {
        internal fun oppgaveinspektør(oppgave: Oppgave, block: OppgaveInspector.() -> Unit) {
            val inspektør = OppgaveInspector()
            oppgave.accept(inspektør)
            block(inspektør)
        }
    }
}
