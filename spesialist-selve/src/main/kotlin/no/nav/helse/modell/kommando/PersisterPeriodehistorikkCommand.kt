package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import org.slf4j.LoggerFactory

internal class PersisterPeriodehistorikkCommand(
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val utbetalingDao: UtbetalingDao,
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(PersisterPeriodehistorikkCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val alleUtbetalingerForVedtaksperiode = utbetalingDao.utbetalingerForVedtaksperiode(vedtaksperiodeId)

        if (alleUtbetalingerForVedtaksperiode.size < 2) return true

        val forrigeUtbetaling = alleUtbetalingerForVedtaksperiode.last()
        val forrigeStatus = forrigeUtbetaling.utbetalingsstatus

        if (forrigeStatus == Utbetalingsstatus.FORKASTET) {
            val forrigeUtbetalingId = forrigeUtbetaling.utbetalingId
            logg.info("Migrerer periodehistorikk fra utbetalingId $forrigeUtbetalingId til utbetalingId $utbetalingId for vedtaksperiodeId $vedtaksperiodeId")
            periodehistorikkDao.migrer(forrigeUtbetalingId, utbetalingId)
        }

        return true
    }


}
