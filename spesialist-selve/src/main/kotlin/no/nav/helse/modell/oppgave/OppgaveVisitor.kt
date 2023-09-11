package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingVisitor
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavetype

interface OppgaveVisitor: TotrinnsvurderingVisitor {
    fun visitOppgave(
        id: Long,
        type: Oppgavetype,
        tilstand: Oppgave.Tilstand,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        hendelseId: UUID,
        ferdigstiltAvOid: UUID?,
        ferdigstiltAvIdent: String?,
        egenskaper: List<Oppgavetype>,
        tildelt: Saksbehandler?,
        påVent: Boolean,
        totrinnsvurdering: Totrinnsvurdering?
    ) {}
}