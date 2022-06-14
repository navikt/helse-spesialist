package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyringType
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.slf4j.LoggerFactory

internal class TrengerTotrinnsvurderingCommand(
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveMediator: OppgaveMediator,
    private val overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(TrengerTotrinnsvurderingCommand::class.java)
    }
    override fun execute(context: CommandContext): Boolean {
        val harMedlemskapsvarsel = harMedlemskapsVarsel()
        val overstyringer = finnOverstyringerMedType()

        if (harMedlemskapsvarsel || overstyringer.isNotEmpty()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
            oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering()
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
        val vedtaksperiodeOverstyringtyper = overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId har blitt overstyrt eller revurdert med typer: $vedtaksperiodeOverstyringtyper")

        return vedtaksperiodeOverstyringtyper
    }
}
