package no.nav.helse.modell.kommando

import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val vedtaksperiodeId: UUID,
    private val utbetalingDao: UtbetalingDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
) : Command {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        val utbetalinger = utbetalingDao.utbetalingerForVedtaksperiode(vedtaksperiodeId)
        val utbetalingId = utbetalinger.firstOrNull()?.utbetalingId

        if (utbetalingId != null) {
            sikkerLogg.info(
                "Vedtaksperiode reberegnet lagres som periodehistorikk på vedtaksperiode {} med utbetalingId {}",
                vedtaksperiodeId,
                utbetalingId,
            )
            periodehistorikkDao.lagre(PeriodehistorikkType.VEDTAKSPERIODE_REBEREGNET, null, utbetalingId)
        } else {
            sikkerLogg.info(
                "Kunne ikke legge til periodehistorikk ved reberegnet vedtaksperiode. Finner ikke utbetalingId for vedtaksperiodeId {} ",
                vedtaksperiodeId,
            )
        }

        return true
    }
}
