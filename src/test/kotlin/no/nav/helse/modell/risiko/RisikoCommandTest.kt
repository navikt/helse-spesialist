package no.nav.helse.modell.risiko

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.Toggles
import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.behov
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RisikoCommandTest {

    private companion object {
        private const val ORGNUMMER1 = "123456789"
        private const val ORGNUMMER2 = "456789123"
        private const val ORGNUMMER3 = "789456123"
        private val PERIODETYPE1 = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
        private val PERIODETYPE2 = Saksbehandleroppgavetype.FORLENGELSE
        private val PERIODETYPE3 = Saksbehandleroppgavetype.OVERGANG_FRA_IT
        private val VEDTAKSPERIODE_ID_1 = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID_3 = UUID.randomUUID()
        private val AKTIV_VEDTAKSPERIODE_1 = Godkjenningsbehov.AktivVedtaksperiode(
            ORGNUMMER1,
            VEDTAKSPERIODE_ID_1,
            PERIODETYPE1
        )
        private val AKTIV_VEDTAKSPERIODE_2 = Godkjenningsbehov.AktivVedtaksperiode(
            ORGNUMMER2,
            VEDTAKSPERIODE_ID_2,
            PERIODETYPE2
        )
        private val AKTIV_VEDTAKSPERIODE_3 = Godkjenningsbehov.AktivVedtaksperiode(
            ORGNUMMER3,
            VEDTAKSPERIODE_ID_3,
            PERIODETYPE3
        )
        private val RISIKOVURDERING_DAO = mockk<RisikovurderingDao>()
        private val WARNING_DAO = mockk<WarningDao>()
    }

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_1) } returns null
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_2) } returns null
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_3) } returns null
        Toggles.Risikovurdering.enable()
    }

    @AfterEach
    fun teardown() {
        Toggles.Risikovurdering.pop()
    }

    @Test
    fun `Sender behov for risikovurdering for en arbeidsgiver`() {
        val risikoCommand = risikoCommand(aktiveVedtaksperioder = listOf(AKTIV_VEDTAKSPERIODE_1))
        val context = CommandContext(UUID.randomUUID())

        assertFalse(risikoCommand.execute(context))

        assertTrue(context.harBehov())
        assertEquals(1, context.behov().size)
        assertEquals("Risikovurdering", context.behov().keys.first())
    }

    @Test
    fun `Sender behov for risikovurdering for to parallelle arbeidsgivere`() {
        val risikoCommand =
            risikoCommand(aktiveVedtaksperioder = listOf(AKTIV_VEDTAKSPERIODE_1, AKTIV_VEDTAKSPERIODE_2))
        val context = CommandContext(UUID.randomUUID())

        assertFalse(risikoCommand.execute(context))

        assertTrue(context.harBehov())
        val behovsgrupper = context.behovsgrupper()
        assertEquals(2, behovsgrupper.size)
        assertEquals("Risikovurdering", behovsgrupper.first().behov().keys.first())
        assertEquals("Risikovurdering", behovsgrupper.last().behov().keys.first())
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede har en løsning`() {
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_1) } returns mockk()
        val risikoCommand = risikoCommand()
        val context = CommandContext(UUID.randomUUID())

        assertTrue(risikoCommand.execute(context))
    }

    @Test
    fun `Venter på løsning på alle utstedte behov`() {
        val risikoCommand = risikoCommand()
        val context = CommandContext(UUID.randomUUID())
        context.add(mockk<Risikovurderingløsning>(relaxed = true))

        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_1) } returns mockk()
        assertFalse(risikoCommand.resume(context))

        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_2) } returns mockk()
        assertFalse(risikoCommand.resume(context))

        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID_3) } returns mockk()
        assertTrue(risikoCommand.resume(context))
    }

    private fun risikoCommand(
        aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode> = listOf(
            AKTIV_VEDTAKSPERIODE_1,
            AKTIV_VEDTAKSPERIODE_2,
            AKTIV_VEDTAKSPERIODE_3
        ),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID_1,
        risikovurderingDao: RisikovurderingDao = RISIKOVURDERING_DAO,
        warningDao: WarningDao = WARNING_DAO
    ) = RisikoCommand(
        vedtaksperiodeId = vedtaksperiodeId,
        aktiveVedtaksperioder = aktiveVedtaksperioder,
        risikovurderingDao = risikovurderingDao,
        warningDao = warningDao
    )
}
