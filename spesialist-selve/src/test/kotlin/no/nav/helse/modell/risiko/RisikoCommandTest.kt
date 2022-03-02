package no.nav.helse.modell.risiko

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RisikoCommandTest {

    private companion object {
        private const val ORGNUMMER = "123456789"
        private val PERIODETYPE = Periodetype.FØRSTEGANGSBEHANDLING
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val AKTIV_VEDTAKSPERIODE = AktivVedtaksperiode(
            ORGNUMMER,
            VEDTAKSPERIODE_ID,
            PERIODETYPE
        )

        private val RISIKOVURDERING_DAO = mockk<RisikovurderingDao>()
        private val WARNING_DAO = mockk<WarningDao>()
    }

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearAllMocks()
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID) } returns null
    }

    @Test
    fun `Sender behov for risikovurdering for en arbeidsgiver`() {
        val risikoCommand = risikoCommand(aktiveVedtaksperioder = listOf(AKTIV_VEDTAKSPERIODE))
        risikoCommand.assertFalse()
        assertTrue(context.harBehov())
        assertEquals(1, context.behov().size)
        assertEquals("Risikovurdering", context.behov().keys.first())
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() {
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID) } returns mockk()
        val risikoCommand = risikoCommand()
        risikoCommand.assertTrue()
        assertFalse(context.harBehov())
    }

    private fun RisikoCommand.assertTrue() {
        assertTrue(resume(context))
        assertTrue(execute(context))
    }

    private fun RisikoCommand.assertFalse() {
        assertFalse(resume(context))
        assertFalse(execute(context))
    }

    private fun risikoCommand(
        aktiveVedtaksperioder: List<AktivVedtaksperiode> = listOf(AKTIV_VEDTAKSPERIODE),
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        risikovurderingDao: RisikovurderingDao = RISIKOVURDERING_DAO,
        warningDao: WarningDao = WARNING_DAO,
        organisasjonsnummer: String = ORGNUMMER,
        periodetype: Periodetype = PERIODETYPE
    ) = RisikoCommand(
        vedtaksperiodeId = vedtaksperiodeId,
        aktiveVedtaksperioder = aktiveVedtaksperioder,
        risikovurderingDao = risikovurderingDao,
        warningDao = warningDao,
        organisasjonsnummer = organisasjonsnummer,
        periodetype = periodetype
    )
}
