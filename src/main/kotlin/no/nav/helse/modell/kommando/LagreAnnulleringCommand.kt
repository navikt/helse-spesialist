package no.nav.helse.modell.kommando

import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class LagreAnnulleringCommand(
    private val utbetalingDao: UtbetalingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val annullertTidspunkt: LocalDateTime,
    private val saksbehandlerEpost: String,
    private val utbetalingId: UUID
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotUtenÅLagreWarningsCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        TODO("Not yet implemented")
    }

    private fun lagreSaksbehandlerInfo(): Boolean {
        val oid = requireNotNull(saksbehandlerDao.finnSaksbehandler(saksbehandlerEpost)
            .firstOrNull()) { "Finner ikke saksbehandler for annullering med id: $utbetalingId " } .oid
        utbetalingDao.nyAnnullering(annullertTidspunkt, oid)

    }
}
