package no.nav.helse.modell.dkif

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.meldinger.DigitalKontaktinformasjonLøsning
import no.nav.helse.modell.command.nyny.CommandContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `Ber om digital kontaktinformasjon`() {
        assertFalse(command.execute(context))
        assertEquals(listOf("DigitalKontaktinformasjon"), context.behov().keys.toList())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command.resume(context))
        verify(exactly = 0) { dao.persisterDigitalKontaktinformasjon(any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(DigitalKontaktinformasjonLøsning(LocalDateTime.now(), FNR, true))
        assertTrue(command.resume(context))
        verify(exactly = 1) { dao.persisterDigitalKontaktinformasjon(any()) }
    }
}
