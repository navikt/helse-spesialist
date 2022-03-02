package no.nav.helse.modell.egenansatt

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.EgenAnsattløsning
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class EgenAnsattCommandTest {
    private companion object {
        private const val FNR = "12345678911"
    }

    private val dao = mockk<EgenAnsattDao>(relaxed = true)

    private val command = EgenAnsattCommand(dao)
    private lateinit var context: CommandContext


    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `ber om informasjon om egen ansatt`() {
        assertFalse(command.execute(context))
        assertEquals(listOf("EgenAnsatt"), context.behov().keys.toList())
    }

    @Test
    fun `mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.lagre(any(), any(), any()) }
    }

    @Test
    fun `lagrer løsning ved resume`() {
        context.add(EgenAnsattløsning(LocalDateTime.now(), FNR, false))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.lagre(FNR, false, any()) }
    }
}
