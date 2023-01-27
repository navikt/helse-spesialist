package no.nav.helse.modell.dkif

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.løsninger.DigitalKontaktinformasjonløsning
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DigitalKontaktinformasjonCommandTest {

    private companion object {
        private const val FNR = "12345678911"
    }

    private val dao = mockk<DigitalKontaktinformasjonDao>(relaxed = true)

    private val command = DigitalKontaktinformasjonCommand(dao)
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `Ber ikke om digital kontaktinformasjon lenger`() {
        assertTrue(command.execute(context))
        assertTrue(context.behov().isEmpty())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.lagre(any(), any(), any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(DigitalKontaktinformasjonløsning(LocalDateTime.now(), FNR, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.lagre(any(), any(), any()) }
    }
}
