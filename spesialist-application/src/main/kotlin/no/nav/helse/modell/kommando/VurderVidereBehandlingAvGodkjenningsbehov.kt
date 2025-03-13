package no.nav.helse.modell.kommando

import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import org.slf4j.LoggerFactory

internal class VurderVidereBehandlingAvGodkjenningsbehov(
    private val commandData: GodkjenningsbehovData,
    private val utbetalingDao: UtbetalingDao,
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveDao: OppgaveDao,
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

        val oppgaveTilstand = oppgaveRepository.finnSisteOppgaveTilstandForUtbetaling(utbetalingId)
        val erAutomatiskGodkjent = vedtakDao.erAutomatiskGodkjent(utbetalingId)
        if (oppgaveTilstand != null && oppgaveTilstand !is Oppgave.Invalidert || erAutomatiskGodkjent) {
            if (erAutomatiskGodkjent) {
                sikkerlogg.info(
                    "Ignorerer godkjenningsbehov for utbetalingId=$utbetalingId. " +
                        "Utbetalingen er allerede automatisk godkjent.",
                )
            } else {
                sikkerlogg.info(
                    "Ignorerer godkjenningsbehov for utbetalingId=$utbetalingId " +
                        "på grunn av oppgave med tilstand ${oppgaveTilstand?.let { it::class.simpleName }}.",
                )
            }
            oppgaveDao.oppdaterPekerTilGodkjenningsbehov(meldingId, utbetalingId)
            sikkerlogg.info("Oppdaterte peker til godkjenningsbehov for oppgave med utbetalingId=$utbetalingId til id=$meldingId")
            if (oppgaveTilstand is Oppgave.Ferdigstilt || erAutomatiskGodkjent) {
                sikkerlogg.warn(
                    "utbetalingId=$utbetalingId er allerede ferdig behandlet. " +
                        "Det var litt rart at dette kom inn, " +
                        "men det kan være normalt dersom behandlingen i Spesialist nettopp ble ferdig.",
                )
            }
            return ferdigstill(context)
        }

        return true
    }
}
