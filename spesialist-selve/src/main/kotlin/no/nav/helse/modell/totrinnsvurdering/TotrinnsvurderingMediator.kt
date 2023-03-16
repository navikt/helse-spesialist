package no.nav.helse.modell.totrinnsvurdering

import java.util.UUID
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType

class TotrinnsvurderingMediator(
    private val dao: TotrinnsvurderingDao,
    private val oppgaveMediator: OppgaveMediator,
    private val notatMediator: NotatMediator,
) {
    fun opprett(vedtaksperiodeId: UUID): Totrinnsvurdering = dao.opprett(vedtaksperiodeId)
    fun settSaksbehandler(oppgaveId: Long, saksbehandlerOid: UUID): Unit =
        dao.settSaksbehandler(oppgaveId, saksbehandlerOid)

    fun settBeslutter(vedtaksperiodeId: UUID, saksbehandlerOid: UUID): Unit =
        dao.settBeslutter(vedtaksperiodeId, saksbehandlerOid)

    fun settAutomatiskRetur(vedtaksperiodeId: UUID) {
        oppgaveMediator.finnNyesteOppgaveId(vedtaksperiodeId)?.let {
            dao.settErRetur(vedtaksperiodeId)

            oppgaveMediator.lagrePeriodehistorikk(
                oppgaveId = it,
                saksbehandleroid = null,
                type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR
            )
        }
    }

    fun settRetur(oppgaveId: Long, beslutterOid: UUID, notat: String) {
        dao.settErRetur(oppgaveId)

        dao.settBeslutter(oppgaveId, beslutterOid)
        val notatId = notatMediator.lagreForOppgaveId(oppgaveId, notat, beslutterOid, NotatType.Retur)

        oppgaveMediator.lagrePeriodehistorikk(
            oppgaveId = oppgaveId,
            saksbehandleroid = beslutterOid,
            type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
            notatId = notatId?.toInt()
        )
    }

    fun settHåndtertRetur(oppgaveId: Long): Unit = dao.settHåndtertRetur(oppgaveId)
    fun ferdigstill(vedtaksperiodeId: UUID): Unit = dao.ferdigstill(vedtaksperiodeId)
    fun hentAktiv(vedtaksperiodeId: UUID): Totrinnsvurdering? = dao.hentAktiv(vedtaksperiodeId)
    fun hentAktiv(oppgaveId: Long): Totrinnsvurdering? = dao.hentAktiv(oppgaveId)
}
