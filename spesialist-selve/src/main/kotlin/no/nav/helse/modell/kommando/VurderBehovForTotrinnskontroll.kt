package no.nav.helse.modell.kommando

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.TotrinnsvurderingRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.OverstyringType
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val oppgaveService: OppgaveService,
    private val overstyringDao: OverstyringDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
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

            val totrinnsvurdering = totrinnsvurderingRepository.finnTotrinnsvurdering(fødselsnummer) ?: Totrinnsvurdering.ny(vedtaksperiodeId)

            if (totrinnsvurdering.erBeslutteroppgave) {
                // Kan man finne behandlingsId i stedet for å finne det med oppgaveId?
                oppgaveService.finnIdForAktivOppgave(vedtaksperiodeId)?.let {
                    totrinnsvurdering.settRetur()
                    val innslag = Historikkinnslag.totrinnsvurderingAutomatiskRetur()
                    periodehistorikkDao.lagreMedOppgaveId(innslag, it)
                }
            }
            totrinnsvurderingRepository.lagre(totrinnsvurdering, fødselsnummer)

            val behandlendeSaksbehandlerOid = totrinnsvurdering.saksbehandler?.oid
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
        if (overstyringerFunnetMedNyttOppslag.isEmpty() && overstyringer.isNotEmpty()) {
            sikkerlogg.info(
                "Gammelt oppslag fant overstyring(-er) som nytt oppslag ikke fant. Skulle perioden gått til totrinns?",
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
        return overstyringDao.finnOverstyringerMedTypeForVedtaksperioder(spleisVedtaksperiodeIder)
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
        val vedtaksperiodeOverstyringtyper = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)
        if (vedtaksperiodeOverstyringtyper.isNotEmpty()) {
            logg.info(
                "Vedtaksperioden: $vedtaksperiodeId har blitt overstyrt eller revurdert med typer: $vedtaksperiodeOverstyringtyper",
            )
        }

        return vedtaksperiodeOverstyringtyper
    }
}
