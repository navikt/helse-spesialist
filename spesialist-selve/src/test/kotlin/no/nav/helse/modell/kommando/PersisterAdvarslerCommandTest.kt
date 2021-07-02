package no.nav.helse.modell.kommando

import io.mockk.Ordering
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning.Companion.warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class PersisterAdvarslerCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val context = CommandContext(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        clearMocks(warningDao)
    }

    @Test
    fun `fjerner warnings før insert`() {
        val warnings = listOf("Warning A", "Warning B").somWarnings()
        val command = PersisterAdvarslerCommand(VEDTAKSPERIODE_ID, warnings, warningDao)
        command.execute(context)

        verify(Ordering.SEQUENCE) {
            warningDao.fjernWarnings(VEDTAKSPERIODE_ID)
            warningDao.leggTilWarnings(VEDTAKSPERIODE_ID, warnings)
        }
    }

    private fun List<String>.somWarnings() = map { warning(it, WarningKilde.Spleis) }
}
