package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.periodehistorikk.VedtaksperiodeReberegnet
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtaksperiodeReberegnetPeriodehistorikkTest : ApplicationTest() {
    @Test
    fun `Lagrer historikkinnslag når vedtaksperioden er reberegnet`() {
        val context = CommandContext(UUID.randomUUID())
        val command = VedtaksperiodeReberegnetPeriodehistorikk(behandling1.id)
        assertTrue(command.execute(context, sessionContext, outbox))
        val innslag = sessionContext.periodehistorikkDao.behandlingData[behandling1.id.value]
        assertTrue(innslag?.singleOrNull() is VedtaksperiodeReberegnet)
    }
}
