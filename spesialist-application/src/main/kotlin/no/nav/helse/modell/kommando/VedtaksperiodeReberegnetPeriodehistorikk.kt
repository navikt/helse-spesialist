package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.application.Outbox
import java.util.UUID

internal class VedtaksperiodeReberegnetPeriodehistorikk(
    private val spesialistBehandlingId: UUID,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val innslag = Historikkinnslag.vedtaksperiodeReberegnet()
        sessionContext.periodehistorikkDao.lagre(historikkinnslag = innslag, behandlingUnikId = spesialistBehandlingId)
        return true
    }
}
