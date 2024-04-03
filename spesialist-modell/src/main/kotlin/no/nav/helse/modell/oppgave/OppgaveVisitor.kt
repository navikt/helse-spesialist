package no.nav.helse.modell.oppgave

import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingVisitor
import java.util.UUID

interface OppgaveVisitor : TotrinnsvurderingVisitor {
    fun visitOppgave(
        id: Long,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Egenskap>,
        tildelt: Saksbehandler?,
        kanAvvises: Boolean,
        totrinnsvurdering: Totrinnsvurdering?,
    ) {}
}
