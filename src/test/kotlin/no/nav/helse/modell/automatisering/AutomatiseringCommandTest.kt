package no.nav.helse.modell.automatisering

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.MiljøstyrtFeatureToggle
import no.nav.helse.modell.command.nyny.CommandContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class AutomatiseringCommandTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val hendelseId = UUID.randomUUID()
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val miljøstyrtFeatureToggle = mockk<MiljøstyrtFeatureToggle>(relaxed = true)
    private val command =
        AutomatiseringCommand(vedtaksperiodeId, hendelseId, automatisering, miljøstyrtFeatureToggle, "{}")
    private var captureBleAutomatisert = CapturingSlot<Boolean>()
    private var captureVedtaksperiodeId = CapturingSlot<UUID>()
    private var captureHendelseId = CapturingSlot<UUID>()

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `feature toggle av gir ikke-automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(false)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `feature toggle på og ikke automatiserbar gir ikke-automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        every { automatisering.godkjentForAutomatisertBehandling(any(), any()) }.returns(false)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `feature toggle av og ikke automatiserbar gir ikke-automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(false)
        every { automatisering.godkjentForAutomatisertBehandling(any(), any()) }.returns(false)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `feature toggle på og automatiserbar gir automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        every { automatisering.godkjentForAutomatisertBehandling(any(), any()) }.returns(true)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = true)
        assertEquals(1, context.meldinger().size)
    }

    fun assertLagre(automatisert: Boolean) {
        assertEquals(automatisert, captureBleAutomatisert.captured)
        assertEquals(vedtaksperiodeId, captureVedtaksperiodeId.captured)
        assertEquals(hendelseId, captureHendelseId.captured)
    }
}
