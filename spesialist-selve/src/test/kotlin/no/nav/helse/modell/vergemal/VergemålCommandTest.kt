package no.nav.helse.modell.vergemal

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.behov
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class VergemålCommandTest {

    private companion object {
        private const val FNR = "12345678911"
    }

    private val dao = mockk<VergemålDao>(relaxed = true)

    private val command = VergemålCommand(vergemålDao = dao)
    private lateinit var context: CommandContext

    private val ingenVergemål = Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = false)
    private val vergemål = Vergemål(harVergemål = true, harFremtidsfullmakter = false, harFullmakter = false)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `Toggle på - ber om informasjon om vergemål`() = Toggle.VergemålToggle.enable {
        Assertions.assertFalse(command.execute(context))
        Assertions.assertEquals(listOf("Vergemål"), context.behov().keys.toList())
    }

    @Test
    fun `Toggle av - ber ikke om informasjon om vergemål`() = Toggle.VergemålToggle.disable {
        Assertions.assertTrue(command.execute(context))
        Assertions.assertTrue(context.behov().keys.toList().isEmpty())
    }

    @Test
    fun `gjør ingen behandling om vi mangler løsning ved resume`() = Toggle.VergemålToggle.enable {
        Assertions.assertFalse(command.resume(context))
        verify(exactly = 0) { dao.lagre(any(), any()) }
    }

    @Test
    fun `lagrer svar på vergemål ved løsning`() {
        context.add(Vergemålløsning(FNR, ingenVergemål))
        Assertions.assertTrue(command.resume(context))
        verify(exactly = 1) { dao.lagre(FNR, ingenVergemål) }
        Assertions.assertEquals(0, context.meldinger().size)
    }
}
