package no.nav.helse.modell.kommando

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.spesialist.api.oppgave.BESLUTTEROPPGAVE_PREFIX
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.slf4j.LoggerFactory

internal class TrengerTotrinnsvurderingCommand(
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveMediator: OppgaveMediator,
    private val overstyringDao: OverstyringDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(TrengerTotrinnsvurderingCommand::class.java)
    }

    private fun formaterTekst(årsaker: List<String>): String =
        (årsaker.dropLast(2) + årsaker.takeLast(2).joinToString(separator = " og ")).joinToString()

    internal fun getWarningtekst(overstyringer: List<OverstyringType>, medlemskap: Boolean): String {
        val årsaker = mutableListOf<String>()
        if(medlemskap) årsaker.add("Lovvalg og medlemskap")
        if(overstyringer.contains(OverstyringType.Dager)) årsaker.add("Overstyring av utbetalingsdager")
        if(overstyringer.contains(OverstyringType.Inntekt)) årsaker.add("Overstyring av inntekt")
        if(overstyringer.contains(OverstyringType.Arbeidsforhold)) årsaker.add("Overstyring av annet arbeidsforhold")

        return "$BESLUTTEROPPGAVE_PREFIX ${formaterTekst(årsaker)}"
    }

    override fun execute(context: CommandContext): Boolean {
        val harMedlemskapsvarsel = harMedlemskapsVarsel()
        val overstyringer = finnOverstyringerMedType()

        if (harMedlemskapsvarsel || overstyringer.isNotEmpty()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
            oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering()

            warningDao.leggTilWarning(vedtaksperiodeId, Warning(
                melding = getWarningtekst(overstyringer, harMedlemskapsvarsel),
                kilde = WarningKilde.Spesialist,
                opprettet = LocalDateTime.now()
            ))
        }

        return true
    }

    private fun harMedlemskapsVarsel(): Boolean {
        val medlemSkapVarsel = "Vurder lovvalg og medlemskap"
        val harMedlemskapsVarsel = warningDao.finnAktiveWarningsMedMelding(vedtaksperiodeId, medlemSkapVarsel).isNotEmpty()

        logg.info("Vedtaksperioden: $vedtaksperiodeId harMedlemskapsVarsel: $harMedlemskapsVarsel")

        return harMedlemskapsVarsel
    }

    // Overstyringer og Revurderinger
    private fun finnOverstyringerMedType(): List<OverstyringType> {
        val vedtaksperiodeOverstyringtyper = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId har blitt overstyrt eller revurdert med typer: $vedtaksperiodeOverstyringtyper")

        return vedtaksperiodeOverstyringtyper
    }
}
