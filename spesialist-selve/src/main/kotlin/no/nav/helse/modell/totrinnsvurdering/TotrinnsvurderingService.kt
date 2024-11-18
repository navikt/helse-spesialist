package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.db.DialogDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.NotatDto
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import java.util.UUID

class TotrinnsvurderingService(
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
    private val oppgaveDao: OppgaveDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val dialogDao: DialogDao,
) : Totrinnsvurderinghåndterer {
    fun finnEllerOpprettNy(vedtaksperiodeId: UUID): TotrinnsvurderingOld = totrinnsvurderingDao.opprett(vedtaksperiodeId)

    override fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) = totrinnsvurderingDao.settBeslutter(oppgaveId, saksbehandlerOid)

    fun settAutomatiskRetur(vedtaksperiodeId: UUID) {
        oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId)?.let {
            totrinnsvurderingDao.settErRetur(vedtaksperiodeId)
            val innslag = HistorikkinnslagDto.totrinnsvurderingAutomatiskRetur()
            periodehistorikkDao.lagreMedOppgaveId(innslag, it)
        }
    }

    override fun totrinnsvurderingRetur(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        notattekst: String,
    ) {
        val dialogRef = dialogDao.lagre()
        val innslag =
            HistorikkinnslagDto.totrinnsvurderingRetur(
                notattekst = notattekst,
                notat = NotatDto(oppgaveId, notattekst),
                saksbehandler = saksbehandlerFraApi.toDto(),
                dialogRef = dialogRef,
            )
        periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    override fun avventerTotrinnsvurdering(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        val innslag = HistorikkinnslagDto.avventerTotrinnsvurdering(saksbehandlerFraApi.toDto())
        periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
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
