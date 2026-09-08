package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.BehandlingUnikId

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val behandlingUnikId: BehandlingUnikId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val innslag = Historikkinnslag.vedtaksperiodeReberegnet()
        sessionContext.periodehistorikkDao.lagre(historikkinnslag = innslag, behandlingUnikId = behandlingUnikId.value)
        return true
    }
}
