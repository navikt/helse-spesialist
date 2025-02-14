package no.nav.helse.modell.kommando

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val vedtaksperiode: Vedtaksperiode,
    private val periodehistorikkDao: PeriodehistorikkDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val innslag = Historikkinnslag.vedtaksperiodeReberegnet()
        periodehistorikkDao.lagre(historikkinnslag = innslag, generasjonId = vedtaksperiode.gjeldendeUnikId)
        return true
    }
}
