package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.oppgave.OppgaveDao

internal class TrengerTotrinnsvurderingCommand(
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveDao: OppgaveDao
) : Command {
    override fun execute(context: CommandContext): Boolean {

        /*
        context.harBehov()

        if(harMedlemskapsVarsel() || harOppgaveMedEndring()) {
                //TODO sett TrengerTotrinnsvurdering på akutell oppgave
         }*/

        return true
    }

    private fun harMedlemskapsVarsel(): Boolean {
        val medlemSkapVarsel = "Vurder lovvalg og medlemskap"

        return warningDao.finnAktiveWarningsMedMelding(vedtaksperiodeId, medlemSkapVarsel).isNotEmpty()
    }

    private fun harOppgaveMedEndring(): Boolean {

        return oppgaveDao.harOppgaveMedEndring(vedtaksperiodeId)
    }

}