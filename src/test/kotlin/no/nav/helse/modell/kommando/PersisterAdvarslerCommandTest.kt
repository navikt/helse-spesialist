package no.nav.helse.modell.kommando

import io.mockk.Ordering
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class PersisterAdvarslerCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val context = CommandContext(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        clearMocks(vedtakDao)
    }

    @Test
    fun `fjerner warnings før insert`() {
        val warnings = listOf("Warning A", "Warning B").somWarnings()
        val command = PersisterAdvarslerCommand(VEDTAKSPERIODE_ID, warnings, vedtakDao)
        command.execute(context)

        verify(Ordering.SEQUENCE) {
            vedtakDao.fjernWarnings(VEDTAKSPERIODE_ID)
            vedtakDao.leggTilWarnings(VEDTAKSPERIODE_ID, warnings)
        }
    }

    private fun List<String>.somWarnings() = map { WarningDto(it, WarningKilde.Spleis) }
}
