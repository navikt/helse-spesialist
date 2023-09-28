package no.nav.helse.modell.kommando

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.modell.utbetaling.UtbetalingDao

internal class LagreAnnulleringCommand(
    private val utbetalingDao: UtbetalingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val annullertTidspunkt: LocalDateTime,
    private val saksbehandlerEpost: String,
    private val utbetalingId: UUID
) : Command {

    override fun execute(context: CommandContext): Boolean {
        return lagreSaksbehandlerInfo()
    }

    private fun lagreSaksbehandlerInfo(): Boolean {
        val saksbehandlerOid = requireNotNull(saksbehandlerDao.finnOid(saksbehandlerEpost)) {
            "Finner ikke saksbehandler for annullering med id: $utbetalingId"
        }
        val annulleringId = utbetalingDao.nyAnnullering(annullertTidspunkt, saksbehandlerOid)

        utbetalingDao.leggTilAnnullertAvSaksbehandler(utbetalingId, annulleringId)

        return true
    }
}
