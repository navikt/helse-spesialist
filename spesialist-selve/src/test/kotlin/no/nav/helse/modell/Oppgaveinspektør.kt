package no.nav.helse.modell

import java.util.UUID
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

class OppgaveInspektør private constructor(): OppgaveVisitor {
    lateinit var tilstand: Oppgave.Tilstand
    lateinit var egenskap: Egenskap
    var tildelt: Boolean = false
    var påVent: Boolean = false
    var tildeltTil: Saksbehandler? = null
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
    }

    companion object {
        fun inspektør(oppgave: Oppgave, block: OppgaveInspektør.() -> Unit) {
            val inspektør = OppgaveInspektør()
            oppgave.accept(inspektør)
            block(inspektør)
        }
    }
}
