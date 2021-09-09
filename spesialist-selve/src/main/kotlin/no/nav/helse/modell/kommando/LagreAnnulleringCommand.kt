package no.nav.helse.modell.kommando

import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import java.time.LocalDateTime
import java.util.*

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
        val saksbehandlerOid =
            requireNotNull(saksbehandlerDao.finnSaksbehandler(saksbehandlerEpost)) { "Finner ikke saksbehandler for annullering med id: $utbetalingId " }
            .oid
        val annulleringId = utbetalingDao.nyAnnullering(annullertTidspunkt, saksbehandlerOid)

        utbetalingDao.leggTilAnnullertAvSaksbehandler(utbetalingId, annulleringId)

        return true
    }
}
