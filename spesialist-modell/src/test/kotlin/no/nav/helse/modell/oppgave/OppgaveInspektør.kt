package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

internal class OppgaveInspektør private constructor(): OppgaveVisitor {
    internal lateinit var tilstand: Oppgave.Tilstand
    internal lateinit var egenskap: Egenskap
    internal var tildelt: Boolean = false
    internal var påVent: Boolean = false
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
        påVent: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {
        this.tilstand = tilstand
        this.tildelt = tildelt != null
        this.tildeltTil = tildelt
        this.påVent = påVent
        this.egenskap = egenskap
       this.egenskaper.addAll(egenskaper)
    }

    internal companion object {
        fun inspektør(oppgave: Oppgave, block: OppgaveInspektør.() -> Unit) {
            val inspektør = OppgaveInspektør()
            oppgave.accept(inspektør)
            block(inspektør)
        }
    }
}
