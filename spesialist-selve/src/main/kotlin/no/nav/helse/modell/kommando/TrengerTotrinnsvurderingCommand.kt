package no.nav.helse.modell.kommando

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_BO_1
import no.nav.helse.modell.varsel.Varselkode.SB_BO_2
import no.nav.helse.modell.varsel.Varselkode.SB_BO_3
import no.nav.helse.modell.varsel.Varselkode.SB_BO_4
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.spesialist.api.oppgave.BESLUTTEROPPGAVE_PREFIX
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.slf4j.LoggerFactory

internal class TrengerTotrinnsvurderingCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveMediator: OppgaveMediator,
    private val overstyringDao: OverstyringDao,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator,
    private val varselRepository: VarselRepository,
    private val generasjonRepository: GenerasjonRepository,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(TrengerTotrinnsvurderingCommand::class.java)
    }

    private fun formaterTekst(årsaker: List<String>): String =
        (årsaker.dropLast(2) + årsaker.takeLast(2).joinToString(separator = " og ")).joinToString()

    internal fun getWarningtekst(overstyringer: List<OverstyringType>, medlemskap: Boolean): String {
        val årsaker = mutableListOf<String>()
        if (medlemskap) årsaker.add("Lovvalg og medlemskap")
        if (overstyringer.contains(OverstyringType.Dager)) årsaker.add("Overstyring av utbetalingsdager")
        if (overstyringer.contains(OverstyringType.Inntekt)) årsaker.add("Overstyring av inntekt")
        if (overstyringer.contains(OverstyringType.Arbeidsforhold)) årsaker.add("Overstyring av annet arbeidsforhold")

        return "$BESLUTTEROPPGAVE_PREFIX ${formaterTekst(årsaker)}"
    }

    private fun varselkoder(overstyringer: List<OverstyringType>, medlemskap: Boolean): List<Varselkode> {
        val varselkoder = mutableListOf<Varselkode>()
        if (medlemskap) varselkoder.add(SB_BO_1)
        if (overstyringer.contains(OverstyringType.Dager)) varselkoder.add(SB_BO_2)
        if (overstyringer.contains(OverstyringType.Inntekt)) varselkoder.add(SB_BO_3)
        if (overstyringer.contains(OverstyringType.Arbeidsforhold)) varselkoder.add(SB_BO_4)
        return varselkoder
    }

    override fun execute(context: CommandContext): Boolean {
        val harMedlemskapsvarsel = harMedlemskapsVarsel()
        val overstyringer = finnOverstyringerMedType()

        if (harMedlemskapsvarsel || overstyringer.isNotEmpty()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
            if (Toggle.Totrinnsvurdering.enabled) {
                val totrinnsvurdering = totrinnsvurderingMediator.opprett(vedtaksperiodeId)

                if (totrinnsvurdering.erBeslutteroppgave()) {
                    totrinnsvurderingMediator.settAutomatiskRetur(vedtaksperiodeId)
                }
                if (totrinnsvurdering.saksbehandler != null) {
                    oppgaveMediator.reserverOppgave(
                        saksbehandleroid = totrinnsvurdering.saksbehandler,
                        fødselsnummer = fødselsnummer
                    )
                }
            } else {
                oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering()
            }

            warningDao.leggTilWarning(
                vedtaksperiodeId, Warning(
                    melding = getWarningtekst(overstyringer, harMedlemskapsvarsel),
                    kilde = WarningKilde.Spesialist,
                    opprettet = LocalDateTime.now()
                )
            )
            val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
            varselkoder(overstyringer, harMedlemskapsvarsel).forEach {
                it.nyttVarsel(generasjon, varselRepository)
            }
        }

        return true
    }

    private fun harMedlemskapsVarsel(): Boolean {
        val medlemSkapVarsel = "Vurder lovvalg og medlemskap"
        val harMedlemskapsVarsel =
            warningDao.finnAktiveWarningsMedMelding(vedtaksperiodeId, medlemSkapVarsel).isNotEmpty()
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveMediator.harFerdigstiltOppgave(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId harMedlemskapsVarsel: $harMedlemskapsVarsel")

        return harMedlemskapsVarsel && !vedtaksperiodeHarFerdigstiltOppgave
    }

    // Overstyringer og Revurderinger
    private fun finnOverstyringerMedType(): List<OverstyringType> {
        val vedtaksperiodeOverstyringtyper = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId har blitt overstyrt eller revurdert med typer: $vedtaksperiodeOverstyringtyper")

        return vedtaksperiodeOverstyringtyper
    }
}
