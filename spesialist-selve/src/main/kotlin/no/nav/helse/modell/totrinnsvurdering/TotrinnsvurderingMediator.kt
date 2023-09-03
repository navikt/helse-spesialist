package no.nav.helse.modell.totrinnsvurdering

import java.util.UUID
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.slf4j.LoggerFactory

class TotrinnsvurderingMediator(
    private val dao: TotrinnsvurderingDao,
    private val oppgaveDao: OppgaveDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatMediator: NotatMediator,
) {
    private val log = LoggerFactory.getLogger("TotrinnsvurderingMediator")

    fun opprett(vedtaksperiodeId: UUID): Totrinnsvurdering = dao.opprett(vedtaksperiodeId)
    fun settSaksbehandler(oppgaveId: Long, saksbehandlerOid: UUID): Unit =
        dao.settSaksbehandler(oppgaveId, saksbehandlerOid)

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

    fun lagrePeriodehistorikk(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
        type: PeriodehistorikkType,
        notatId: Int? = null
    ) {
        oppgaveDao.finnUtbetalingId(oppgaveId)?.also {
            periodehistorikkDao.lagre(type, saksbehandleroid, it, notatId)
        }
    }

    fun settRetur(oppgaveId: Long, beslutterOid: UUID, notat: String) {
        dao.settErRetur(oppgaveId)

        dao.settBeslutter(oppgaveId, beslutterOid)
        val notatId = notatMediator.lagreForOppgaveId(oppgaveId, notat, beslutterOid, NotatType.Retur)

        lagrePeriodehistorikk(
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

    fun opprettFraLegacy(oppgaveId: Long): Totrinnsvurdering =
        opprett(oppgaveDao.finnVedtaksperiodeId(oppgaveId)).also {
            log.info("Opprettet totrinnsvurdering fra speil, {}", mapOf("oppgaveId" to oppgaveId, "vedtaksperiodeId" to it.vedtaksperiodeId))
        }
}
