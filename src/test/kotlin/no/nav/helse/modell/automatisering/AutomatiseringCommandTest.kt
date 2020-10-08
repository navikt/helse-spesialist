package no.nav.helse.modell.automatisering

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.command.nyny.CommandContext
import org.junit.jupiter.api.AfterEach
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
    private val miljøstyrtFeatureToggle = mockk<MiljøstyrtFeatureToggle>(relaxed = true)
    private val command =
        AutomatiseringCommand(fødselsnummer, vedtaksperiodeId, hendelseId, automatisering, miljøstyrtFeatureToggle, "{}")
    private var captureBleAutomatisert = CapturingSlot<Boolean>()
    private var captureVedtaksperiodeId = CapturingSlot<UUID>()
    private var captureHendelseId = CapturingSlot<UUID>()

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
    }

    @AfterEach
    fun tearDown() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(false)
    }

    @Test
    fun `feature toggle av gir ikke-automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.automatisering() }.returns(false)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `feature toggle på og ikke automatiserbar gir ikke-automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.automatisering() }.returns(true)
        every { automatisering.godkjentForAutomatisertBehandling(any(), any()) }.returns(false)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `feature toggle av og ikke automatiserbar gir ikke-automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.automatisering() }.returns(false)
        every { automatisering.godkjentForAutomatisertBehandling(any(), any()) }.returns(false)
        assertTrue(command.execute(context))
        verify { automatisering.lagre(capture(captureBleAutomatisert), capture(captureVedtaksperiodeId), capture(captureHendelseId)) }
        assertLagre(automatisert = false)
        assertTrue(context.meldinger().isEmpty())
    }

    @Test
    fun `feature toggle på og automatiserbar gir automatiserbar behandling`() {
        every { miljøstyrtFeatureToggle.automatisering() }.returns(true)
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
