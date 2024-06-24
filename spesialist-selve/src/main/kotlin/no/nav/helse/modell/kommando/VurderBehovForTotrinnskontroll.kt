package no.nav.helse.modell.kommando

import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveService: OppgaveService,
    private val overstyringDao: OverstyringDao,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderBehovForTotrinnskontroll::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val kreverTotrinnsvurdering = sykefraværstilfelle.kreverTotrinnsvurdering(vedtaksperiodeId)
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId)
        val overstyringer = finnOverstyringerMedType()
        finnOverstyringer(overstyringer)

        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || overstyringer.isNotEmpty())
            {
                logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
                val totrinnsvurdering = totrinnsvurderingMediator.opprett(vedtaksperiodeId)

                if (totrinnsvurdering.erBeslutteroppgave()) {
                    totrinnsvurderingMediator.settAutomatiskRetur(vedtaksperiodeId)
                }
                val behandlendeSaksbehandlerOid = totrinnsvurdering.saksbehandler
                if (behandlendeSaksbehandlerOid != null) {
                    oppgaveService.reserverOppgave(
                        saksbehandleroid = behandlendeSaksbehandlerOid,
                        fødselsnummer = fødselsnummer,
                    )
                }
            }

        return true
    }

    private fun finnOverstyringer(overstyringer: List<OverstyringType>): List<OverstyringType> {
        val spleisVedtaksperiodeIder = spleisVedtaksperioder.map { it.vedtaksperiodeId }
        val vedtaksperiodeOverstyringtyper = overstyringDao.finnOverstyringerMedTypeForVedtaksperioder(spleisVedtaksperiodeIder)

        if (overstyringer.toSet() == vedtaksperiodeOverstyringtyper.toSet()) {
            logg.info("Finne overstyringer. Gammel og ny metode fant samme overstyring(er)")
        } else {
            logg.info("Finne overstyringer. Gammel fant: $overstyringer, ny fant: $vedtaksperiodeOverstyringtyper")
        }
        return vedtaksperiodeOverstyringtyper
    }

    // Overstyringer og Revurderinger
    private fun finnOverstyringerMedType(): List<OverstyringType> {
        val vedtaksperiodeOverstyringtyper = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId har blitt overstyrt eller revurdert med typer: $vedtaksperiodeOverstyringtyper")

        return vedtaksperiodeOverstyringtyper
    }
}
