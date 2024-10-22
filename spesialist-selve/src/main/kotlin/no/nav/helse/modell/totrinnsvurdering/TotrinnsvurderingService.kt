package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.db.HistorikkinnslagRepository
import no.nav.helse.db.NotatRepository
import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.periodehistorikk.NotatDto
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import java.util.UUID

class TotrinnsvurderingService(
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val historikkinnslagRepository: HistorikkinnslagRepository,
    private val notatRepository: NotatRepository,
) : Totrinnsvurderinghåndterer {
    fun finnEllerOpprettNy(vedtaksperiodeId: UUID): TotrinnsvurderingOld = totrinnsvurderingRepository.opprett(vedtaksperiodeId)

    override fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ): Unit = totrinnsvurderingRepository.settBeslutter(oppgaveId, saksbehandlerOid)

    fun settAutomatiskRetur(vedtaksperiodeId: UUID) {
        oppgaveRepository.finnIdForAktivOppgave(vedtaksperiodeId)?.let {
            totrinnsvurderingRepository.settErRetur(vedtaksperiodeId)

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
        oppgaveRepository.finnUtbetalingId(oppgaveId)?.also {
            historikkinnslagRepository.lagre(type, saksbehandleroid, it, notatId)
        }
    }

    override fun totrinnsvurderingRetur(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        notat: String,
    ) {
        val innslag = HistorikkinnslagDto.totrinnsvurderingRetur(notat = NotatDto(oppgaveId, notat), saksbehandlerFraApi.toDto())
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

    fun ferdigstill(vedtaksperiodeId: UUID): Unit = totrinnsvurderingRepository.ferdigstill(vedtaksperiodeId)

    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld? = totrinnsvurderingRepository.hentAktiv(vedtaksperiodeId)

    private fun hentAktiv(oppgaveId: Long): TotrinnsvurderingOld? = totrinnsvurderingRepository.hentAktiv(oppgaveId)

    private fun SaksbehandlerFraApi.toDto(): SaksbehandlerDto =
        SaksbehandlerDto(
            epostadresse = this.epost,
            oid = this.oid,
            navn = this.navn,
            ident = this.ident,
        )
}
