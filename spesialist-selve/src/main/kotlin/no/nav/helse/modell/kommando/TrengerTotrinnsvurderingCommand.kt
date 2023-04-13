package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.slf4j.LoggerFactory

internal class TrengerTotrinnsvurderingCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val warningDao: WarningDao,
    private val oppgaveMediator: OppgaveMediator,
    private val overstyringDao: OverstyringDao,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(TrengerTotrinnsvurderingCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val harMedlemskapsvarsel = harMedlemskapsVarsel()
        val overstyringer = finnOverstyringerMedType()

        if (harMedlemskapsvarsel || overstyringer.isNotEmpty()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")
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
