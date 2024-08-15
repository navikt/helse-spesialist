package no.nav.helse.modell.kommando

import no.nav.helse.db.AnnulleringDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.util.UUID

internal class LagreAnnulleringCommand(
    private val utbetalingDao: UtbetalingDao,
    private val annulleringDao: AnnulleringDao,
    private val utbetalingId: UUID,
    private val arbeidsgiverFagsystemId: String,
) : Command {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean = leggTilAnnullertAvSaksbehandler()

    private fun leggTilAnnullertAvSaksbehandler(): Boolean {
        annulleringDao.oppdaterAnnullering(arbeidsgiverFagsystemId, utbetalingId)
        annulleringDao.finnAnnulleringId(arbeidsgiverFagsystemId)?.let { annulleringId ->
            utbetalingDao.leggTilAnnullertAvSaksbehandler(utbetalingId, annulleringId)
            return true
        }
        sikkerlogg.error("Finner ikke annullering for utbetalingId={}", utbetalingId)
        return false
    }
}
