package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val vedtaksperiodeId: UUID,
    private val utbetalingDao: UtbetalingDao,
    private val periodehistorikkDao: PeriodehistorikkDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val utbetalinger = utbetalingDao.utbetalingerForVedtaksperiode(vedtaksperiodeId)
        val utbetalingId = utbetalinger.firstOrNull()?.utbetalingId

        if(utbetalingId != null) {
            periodehistorikkDao.lagre(PeriodehistorikkType.VEDTAKSPERIODE_REBEREGNET, null, utbetalingId)
        }

        return true
    }
}