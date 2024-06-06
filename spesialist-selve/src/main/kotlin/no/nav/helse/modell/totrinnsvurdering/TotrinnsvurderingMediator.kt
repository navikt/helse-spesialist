package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatRepository
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import java.util.UUID

class TotrinnsvurderingMediator(
    private val dao: TotrinnsvurderingDao,
    private val oppgaveDao: OppgaveDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatRepository: NotatRepository,
) : Totrinnsvurderinghåndterer {
    fun opprett(vedtaksperiodeId: UUID): TotrinnsvurderingOld = dao.opprett(vedtaksperiodeId)

    override fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ): Unit = dao.settBeslutter(oppgaveId, saksbehandlerOid)

    fun settAutomatiskRetur(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.let {
            dao.settErRetur(vedtaksperiodeId)

            lagrePeriodehistorikk(
                oppgaveId = it,
                saksbehandleroid = null,
                type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
            )
        }
    }

    override fun lagrePeriodehistorikk(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
        type: PeriodehistorikkType,
        notat: Pair<String, NotatType>?,
    ) {
        var notatId: Int? = null
        if (notat != null && saksbehandleroid != null) {
            val (tekst, notattype) = notat
            notatId = notatRepository.lagreForOppgaveId(oppgaveId, tekst, saksbehandleroid, notattype)?.toInt()
        }
        oppgaveDao.finnUtbetalingId(oppgaveId)?.also {
            periodehistorikkDao.lagre(type, saksbehandleroid, it, notatId)
        }
    }

    override fun erBeslutterOppgave(oppgaveId: Long): Boolean = hentAktiv(oppgaveId)?.erBeslutteroppgave() ?: false

    override fun erEgenOppgave(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
    ): Boolean = hentAktiv(oppgaveId)?.saksbehandler == saksbehandleroid

    fun ferdigstill(vedtaksperiodeId: UUID): Unit = dao.ferdigstill(vedtaksperiodeId)

    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld? = dao.hentAktiv(vedtaksperiodeId)

    private fun hentAktiv(oppgaveId: Long): TotrinnsvurderingOld? = dao.hentAktiv(oppgaveId)
}
