package no.nav.helse.modell.totrinnsvurdering

import java.util.UUID
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDao.Totrinnsvurdering

class TotrinnsvurderingMediator(private val dao: TotrinnsvurderingDao) {
    fun opprett(vedtaksperiodeId: UUID): Totrinnsvurdering = dao.opprett(vedtaksperiodeId)
    fun settSaksbehandler(oppgaveId: Long, saksbehandlerOid: UUID): Unit =
        dao.settSaksbehandler(oppgaveId, saksbehandlerOid)

    fun settBeslutter(vedtaksperiodeId: UUID, saksbehandlerOid: UUID): Unit =
        dao.settBeslutter(vedtaksperiodeId, saksbehandlerOid)

    fun settBeslutter(oppgaveId: Long, saksbehandlerOid: UUID): Unit = dao.settBeslutter(oppgaveId, saksbehandlerOid)
    fun settErRetur(vedtaksperiodeId: UUID): Unit = dao.settErRetur(vedtaksperiodeId)
    fun settErRetur(oppgaveId: Long): Unit = dao.settErRetur(oppgaveId)
    fun settHåndtertRetur(oppgaveId: Long): Unit = dao.settHåndtertRetur(oppgaveId)
    fun ferdigstill(vedtaksperiodeId: UUID): Unit = dao.ferdigstill(vedtaksperiodeId)
    fun hentAktiv(vedtaksperiodeId: UUID): Totrinnsvurdering? = dao.hentAktiv(vedtaksperiodeId)
    fun hentAktiv(oppgaveId: Long): Totrinnsvurdering? = dao.hentAktiv(oppgaveId)
}
