package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.oppgave.OppgaveDao
import org.slf4j.LoggerFactory

internal class TrengerTotrinnsvurderingCommand(
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveDao: OppgaveDao
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(TrengerTotrinnsvurderingCommand::class.java)
    }
    override fun execute(context: CommandContext): Boolean {

        if (harMedlemskapsVarsel() || harOppgaveMedEndring()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId setter trenger totrinnsvurdering")
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

    private fun harOppgaveMedEndring(): Boolean {

        val harOppgaveMedEndring = oppgaveDao.harOppgaveMedEndring(vedtaksperiodeId)

        logg.info("Vedtaksperioden: $vedtaksperiodeId harOppgaveMedEndring: $harOppgaveMedEndring")

        return harOppgaveMedEndring
    }

}