package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.db.HistorikkinnslagRepository
import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.NotatDto
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import java.util.UUID

class TotrinnsvurderingService(
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
    private val oppgaveRepository: OppgaveRepository,
    private val historikkinnslagRepository: HistorikkinnslagRepository,
) : Totrinnsvurderinghåndterer {
    fun finnEllerOpprettNy(vedtaksperiodeId: UUID): TotrinnsvurderingOld = totrinnsvurderingDao.opprett(vedtaksperiodeId)

    override fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) = totrinnsvurderingDao.settBeslutter(oppgaveId, saksbehandlerOid)

    fun settAutomatiskRetur(vedtaksperiodeId: UUID) {
        oppgaveRepository.finnIdForAktivOppgave(vedtaksperiodeId)?.let {
            totrinnsvurderingDao.settErRetur(vedtaksperiodeId)
            val innslag = HistorikkinnslagDto.totrinnsvurderingAutomatiskRetur()
            historikkinnslagRepository.lagre(innslag, it)
        }
    }

    override fun totrinnsvurderingRetur(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        notat: String,
    ) {
        val innslag =
            HistorikkinnslagDto.totrinnsvurderingRetur(notat = NotatDto(oppgaveId, notat), saksbehandlerFraApi.toDto())
        historikkinnslagRepository.lagre(innslag, oppgaveId)
    }

    override fun avventerTotrinnsvurdering(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val innslag = HistorikkinnslagDto.avventerTotrinnsvurdering(saksbehandlerFraApi.toDto())
        historikkinnslagRepository.lagre(innslag, oppgaveId)
    }

    override fun erBeslutterOppgave(oppgaveId: Long): Boolean = hentAktiv(oppgaveId)?.erBeslutteroppgave() ?: false

    override fun erEgenOppgave(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
    ): Boolean = hentAktiv(oppgaveId)?.saksbehandler == saksbehandleroid

    fun ferdigstill(vedtaksperiodeId: UUID) = totrinnsvurderingDao.ferdigstill(vedtaksperiodeId)

    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld? = totrinnsvurderingDao.hentAktiv(vedtaksperiodeId)

    private fun hentAktiv(oppgaveId: Long): TotrinnsvurderingOld? = totrinnsvurderingDao.hentAktiv(oppgaveId)

    private fun SaksbehandlerFraApi.toDto(): SaksbehandlerDto =
        SaksbehandlerDto(
            epostadresse = this.epost,
            oid = this.oid,
            navn = this.navn,
            ident = this.ident,
        )
}
