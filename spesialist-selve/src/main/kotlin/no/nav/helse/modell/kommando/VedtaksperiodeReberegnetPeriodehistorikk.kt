package no.nav.helse.modell.kommando

import no.nav.helse.db.UtbetalingRepository
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val vedtaksperiodeId: UUID,
    private val utbetalingRepository: UtbetalingRepository,
    private val periodehistorikkDao: PeriodehistorikkDao,
) : Command {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun execute(context: CommandContext): Boolean {
        val utbetalinger = utbetalingRepository.utbetalingerForVedtaksperiode(vedtaksperiodeId)
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
