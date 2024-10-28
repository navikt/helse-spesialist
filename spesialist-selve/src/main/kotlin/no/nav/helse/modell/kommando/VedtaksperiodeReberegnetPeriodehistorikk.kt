package no.nav.helse.modell.kommando

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.HistorikkinnslagDto
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val vedtaksperiode: Vedtaksperiode,
    private val periodehistorikkDao: PeriodehistorikkDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val innslag = HistorikkinnslagDto.vedtaksperiodeReberegnet()
        periodehistorikkDao.lagre(
            historikkinnslag = innslag,
            generasjonId = vedtaksperiode.gjeldendeGenerasjonId,
        )
        return true
    }
}
