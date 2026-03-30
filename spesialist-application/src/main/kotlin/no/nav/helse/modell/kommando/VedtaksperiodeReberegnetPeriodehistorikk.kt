package no.nav.helse.modell.kommando

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.spesialist.application.Outbox

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val vedtaksperiode: LegacyVedtaksperiode,
    private val periodehistorikkDao: PeriodehistorikkDao,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val innslag = Historikkinnslag.vedtaksperiodeReberegnet()
        periodehistorikkDao.lagre(historikkinnslag = innslag, behandlingUnikId = vedtaksperiode.gjeldendeUnikId)
        return true
    }
}
