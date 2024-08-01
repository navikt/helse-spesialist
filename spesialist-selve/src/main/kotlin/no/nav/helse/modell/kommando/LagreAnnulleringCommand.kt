package no.nav.helse.modell.kommando

import no.nav.helse.db.AnnulleringDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.util.UUID

internal class LagreAnnulleringCommand(
    private val utbetalingDao: UtbetalingDao,
    private val annulleringDao: AnnulleringDao,
    private val utbetalingId: UUID,
) : Command {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        return leggTilAnnullertAvSaksbehandler()
    }

    private fun leggTilAnnullertAvSaksbehandler(): Boolean {
        annulleringDao.finnAnnulleringId(utbetalingId)?.let { annulleringId ->
            utbetalingDao.leggTilAnnullertAvSaksbehandler(utbetalingId, annulleringId)
            return true
        }
        sikkerlogg.error("Finner ikke annullering for utbetalingId={}", utbetalingId)
        return false
    }
}
