package no.nav.helse.modell.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.VedtaksperiodeReberegnet
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtaksperiodeReberegnetPeriodehistorikkTest {
    @Test
    fun `Lagrer historikkinnslag når vedtaksperioden er reberegnet`() {
        val context = CommandContext(UUID.randomUUID())
        val repository = mockk<PeriodehistorikkDao>(relaxed = true)
        val command = VedtaksperiodeReberegnetPeriodehistorikk(mockk(relaxed = true), repository)
        assertTrue(command.execute(context))
        verify(exactly = 1) { repository.lagre(any<VedtaksperiodeReberegnet>(), any<UUID>(), any()) }
    }
}
