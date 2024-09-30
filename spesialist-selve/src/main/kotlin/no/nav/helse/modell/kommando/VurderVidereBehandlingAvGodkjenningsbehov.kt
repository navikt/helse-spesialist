package no.nav.helse.modell.kommando

import no.nav.helse.db.OppgaveRepository
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import org.slf4j.LoggerFactory

internal class VurderVidereBehandlingAvGodkjenningsbehov(
    private val commandData: GodkjenningsbehovData,
    private val utbetalingDao: UtbetalingDao,
    private val oppgaveRepository: OppgaveRepository,
    private val vedtakDao: VedtakDao,
) : Command {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val utbetalingId = commandData.utbetalingId
        val meldingId = commandData.id
        if (utbetalingDao.erUtbetalingForkastet(utbetalingId)) {
            sikkerlogg.info("Ignorerer godkjenningsbehov med id=$meldingId for utbetalingId=$utbetalingId fordi utbetalingen er forkastet")
            return ferdigstill(context)
        }
        if (oppgaveRepository.harGyldigOppgave(utbetalingId) || vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            sikkerlogg.info(
                "vedtaksperiodeId=${commandData.vedtaksperiodeId} med utbetalingId=$utbetalingId har gyldig oppgave eller er automatisk godkjent. Ignorerer godkjenningsbehov med id=$meldingId",
            )
            return ferdigstill(context)
        }
        return true
    }
}
