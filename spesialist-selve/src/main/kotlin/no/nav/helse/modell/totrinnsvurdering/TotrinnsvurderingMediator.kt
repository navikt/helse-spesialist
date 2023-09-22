package no.nav.helse.modell.totrinnsvurdering

import java.util.UUID
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType

class TotrinnsvurderingMediator(
    private val dao: TotrinnsvurderingDao,
    private val oppgaveDao: OppgaveDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatMediator: NotatMediator,
) : Totrinnsvurderinghåndterer {
    fun opprett(vedtaksperiodeId: UUID): TotrinnsvurderingOld = dao.opprett(vedtaksperiodeId)
    fun settBeslutter(vedtaksperiodeId: UUID, saksbehandlerOid: UUID): Unit =
        dao.settBeslutter(vedtaksperiodeId, saksbehandlerOid)

    fun settAutomatiskRetur(vedtaksperiodeId: UUID) {
        oppgaveDao.finnNyesteOppgaveId(vedtaksperiodeId)?.let {
            dao.settErRetur(vedtaksperiodeId)

            lagrePeriodehistorikk(
                oppgaveId = it,
                saksbehandleroid = null,
                type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR
            )
        }
    }

    override fun lagrePeriodehistorikk(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
        type: PeriodehistorikkType,
        notat: Pair<String, NotatType>?
    ) {
        var notatId: Int? = null
        if (notat != null && saksbehandleroid != null) {
            val (tekst, notattype) = notat
            notatId = notatMediator.lagreForOppgaveId(oppgaveId, tekst, saksbehandleroid, notattype)?.toInt()
        }
        oppgaveDao.finnUtbetalingId(oppgaveId)?.also {
            periodehistorikkDao.lagre(type, saksbehandleroid, it, notatId)
        }
    }

    fun ferdigstill(vedtaksperiodeId: UUID): Unit = dao.ferdigstill(vedtaksperiodeId)
    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld? = dao.hentAktiv(vedtaksperiodeId)
    fun hentAktiv(oppgaveId: Long): TotrinnsvurderingOld? = dao.hentAktiv(oppgaveId)
}
