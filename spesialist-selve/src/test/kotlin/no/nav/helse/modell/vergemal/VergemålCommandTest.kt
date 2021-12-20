package no.nav.helse.modell.vergemal

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.mediator.meldinger.utgående.VedtaksperiodeAvvist
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.behov
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class VergemålCommandTest {

    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val dao = mockk<VergemålDao>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)

    private val command = VergemålCommand(
        vergemålDao = dao, vedtaksperiodeId = VEDTAKSPERIODE_ID,
        fødselsnummer = FNR,
        godkjenningMediator = godkjenningMediator,
        godkjenningsbehovJson = "{}"
    )
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
    fun `Toggle va - ber ikke om informasjon om vergemål`() = Toggle.VergemålToggle.disable {
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

    @Test
    fun `avbryter saksbehandling og sender svar på godkjenningsbehov om bruker har vergemål`() {
        every {
            godkjenningMediator.lagVedtaksperiodeAvvist(
                any(),
                any(),
                any()
            )
        } returns VedtaksperiodeAvvist(
            UUID.randomUUID(),
            "",
            emptyList(),
            Periodetype.FØRSTEGANGSBEHANDLING,
            mockk(relaxed = true)
        )
        context.add(Vergemålløsning(FNR, vergemål))
        Assertions.assertTrue(command.resume(context))
        Assertions.assertEquals(2, context.meldinger().size)
        Assertions.assertFalse(
            objectMapper.readTree(context.meldinger().first())
                .path("@løsning")
                .path("Godkjenning")
                .path("godkjent")
                .booleanValue()
        )
        Assertions.assertTrue(context.meldinger().last().contains("vedtaksperiode_avvist"))
    }
}
