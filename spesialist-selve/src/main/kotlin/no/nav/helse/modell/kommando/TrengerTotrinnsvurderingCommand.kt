package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.slf4j.LoggerFactory

internal class TrengerTotrinnsvurderingCommand(
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveDao: OppgaveDao,
    private val overstyrtVedtaksperiodeDao: OverstyrtVedtaksperiodeDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(TrengerTotrinnsvurderingCommand::class.java)
    }
    override fun execute(context: CommandContext): Boolean {

        if (harMedlemskapsVarsel() || harOverstyringEllerRevurdering()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
            oppgaveDao.setTrengerTotrinnsvurdering(vedtaksperiodeId)
        }

        return true
    }

    private fun harMedlemskapsVarsel(): Boolean {
        val medlemSkapVarsel = "Vurder lovvalg og medlemskap"
        val harMedlemskapsVarsel = warningDao.finnAktiveWarningsMedMelding(vedtaksperiodeId, medlemSkapVarsel).isNotEmpty()

        logg.info("Vedtaksperioden: $vedtaksperiodeId harMedlemskapsVarsel: $harMedlemskapsVarsel")

        return harMedlemskapsVarsel
    }

    private fun harOverstyringEllerRevurdering(): Boolean {
        val erVedtaksperiodeOverstyrt = overstyrtVedtaksperiodeDao.erVedtaksperiodeOverstyrt(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId har tidligere blitt overstyrt eller revurdert: $erVedtaksperiodeOverstyrt")

        return erVedtaksperiodeOverstyrt
    }

}
