package no.nav.helse.modell.automatisering

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class AutomatiseringCommandTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private const val fødselsnummer = "12345678910"
        private val hendelseId = UUID.randomUUID()
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val command =
        AutomatiseringCommand(
            fødselsnummer,
            vedtaksperiodeId,
            hendelseId,
            automatisering,
            "{}",
            GodkjenningMediator(warningDao = mockk(relaxed = true), vedtakDao = mockk(relaxed = true))
        )
    private var captureVurdering = CapturingSlot<Automatisering.Automatiseringsvurdering>()
    private var captureVedtaksperiodeId = CapturingSlot<UUID>()
    private var captureHendelseId = CapturingSlot<UUID>()

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `ikke automatiserbar gir ikke-automatiserbar behandling`() {
        every {
            automatisering.vurder(
                any(),
                any()
            )
        } returns Automatisering.Automatiseringsvurdering(mutableListOf("Problem"))
        assertTrue(command.execute(context))
        verify {
            automatisering.lagre(
                capture(captureVurdering),
                capture(captureVedtaksperiodeId),
                capture(captureHendelseId)
            )
        }
        assertAutomatisert(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `automatiserbar gir automatiserbar behandling`() {
        every { automatisering.vurder(any(), any()) } returns Automatisering.Automatiseringsvurdering(mutableListOf())
        assertTrue(command.execute(context))
        verify {
            automatisering.lagre(
                capture(captureVurdering),
                capture(captureVedtaksperiodeId),
                capture(captureHendelseId)
            )
        }
        assertAutomatisert(automatisert = true)
        assertEquals(1, context.meldinger().size)
    }

    private fun assertAutomatisert(automatisert: Boolean) {
        assertEquals(automatisert, captureVurdering.captured.erAutomatiserbar())
        assertEquals(vedtaksperiodeId, captureVedtaksperiodeId.captured)
        assertEquals(hendelseId, captureHendelseId.captured)
    }
}
