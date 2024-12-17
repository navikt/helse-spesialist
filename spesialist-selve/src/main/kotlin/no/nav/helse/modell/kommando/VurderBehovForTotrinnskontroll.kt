package no.nav.helse.modell.kommando

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.overstyring.OverstyringRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveService: OppgaveService,
    private val overstyringRepository: OverstyringRepository,
    private val totrinnsvurderingService: TotrinnsvurderingService,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderBehovForTotrinnskontroll::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val kreverTotrinnsvurdering = sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId)
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId)
        val overstyringer = finnOverstyringerMedType()
        sjekkMotNyttOppslag(overstyringer, kreverTotrinnsvurdering, vedtaksperiodeHarFerdigstiltOppgave)

        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || overstyringer.isNotEmpty()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
            val totrinnsvurdering = totrinnsvurderingService.finnEllerOpprettNy(vedtaksperiodeId)

            if (totrinnsvurdering.erBeslutteroppgave()) {
                totrinnsvurderingService.settAutomatiskRetur(vedtaksperiodeId)
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

    private fun sjekkMotNyttOppslag(
        overstyringer: List<OverstyringType>,
        kreverTotrinnsvurdering: Boolean,
        vedtaksperiodeHarFerdigstiltOppgave: Boolean,
    ) {
        val overstyringerFunnetMedNyttOppslag = finnOverstyringerNyttOppslag()
        loggDiffMellomOverstyringstyper(overstyringer, overstyringerFunnetMedNyttOppslag)
        if (overstyringer.isEmpty() && overstyringerFunnetMedNyttOppslag.isNotEmpty()) {
            sikkerlogg.info(
                "Fant overstyring(-er) kun ved nytt oppslag. Skulle perioden gått til totrinns?",
                kv("fødselsnummer", fødselsnummer),
                kv("kreverTotrinnsvurdering", kreverTotrinnsvurdering),
                kv("vedtaksperiodeHarFerdigstiltOppgave", vedtaksperiodeHarFerdigstiltOppgave),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("berørteVedtaksperioder", spleisVedtaksperioder.map { it.vedtaksperiodeId }),
            )
        }
    }

    private fun finnOverstyringerNyttOppslag(): List<OverstyringType> {
        val spleisVedtaksperiodeIder = spleisVedtaksperioder.map { it.vedtaksperiodeId }
        return overstyringRepository.finnOverstyringerMedTypeForVedtaksperioder(spleisVedtaksperiodeIder)
    }

    private fun loggDiffMellomOverstyringstyper(
        overstyringer: List<OverstyringType>,
        vedtaksperiodeOverstyringtyper: List<OverstyringType>,
    ): Unit =
        when {
            overstyringer.isEmpty() && vedtaksperiodeOverstyringtyper.isEmpty() -> Unit
            overstyringer.toSet() == vedtaksperiodeOverstyringtyper.toSet() ->
                logg.info("Finne overstyringer. Gammel og ny metode fant samme overstyring(er)")

            else ->
                logg.info("Finne overstyringer. Gammel fant: $overstyringer, ny fant: $vedtaksperiodeOverstyringtyper")
        }

    // Overstyringer og Revurderinger
    private fun finnOverstyringerMedType(): List<OverstyringType> {
        val vedtaksperiodeOverstyringtyper = overstyringRepository.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)
        if (vedtaksperiodeOverstyringtyper.isNotEmpty()) {
            logg.info(
                "Vedtaksperioden: $vedtaksperiodeId har blitt overstyrt eller revurdert med typer: $vedtaksperiodeOverstyringtyper",
            )
        }

        return vedtaksperiodeOverstyringtyper
    }
}
