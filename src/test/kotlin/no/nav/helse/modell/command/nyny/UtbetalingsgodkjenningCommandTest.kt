package no.nav.helse.modell.command.nyny

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.command.HendelseDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingsgodkjenningCommandTest {
    private companion object {
        private const val GODKJENT = true
        private const val IDENT = "Z999999"
        private const val EPOST = "saksbehandler@nav.no"
        private val OID = UUID.randomUUID()
        private val TIDSPUNKT = LocalDateTime.now()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
    }

    private val behov = mockk<UtbetalingsgodkjenningMessage>(relaxed = true)
    private val dao = mockk<HendelseDao>()
    private lateinit var commandContext: CommandContext
    private lateinit var command: UtbetalingsgodkjenningCommand

    @BeforeEach
    fun setup() {
        clearMocks(dao, behov)
        commandContext = CommandContext(UUID.randomUUID())
        command = UtbetalingsgodkjenningCommand(GODKJENT, IDENT, OID, EPOST, TIDSPUNKT, null, null, null, GODKJENNINGSBEHOV_ID, dao)
    }

    @Test
    fun `løser godkjenningsbehovet`() {
        every { dao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns behov
        assertTrue(command.execute(commandContext))
        assertEquals(1, commandContext.meldinger().size)
        verify(exactly = 1) { behov.løs(GODKJENT, IDENT, TIDSPUNKT, null, null, null) }
    }
}
