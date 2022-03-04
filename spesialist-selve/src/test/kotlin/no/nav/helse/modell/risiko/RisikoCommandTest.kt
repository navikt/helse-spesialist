package no.nav.helse.modell.risiko

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.Godkjenningsbehov.AktivVedtaksperiode
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
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

        private val meldingsfabrikk = Testmeldingfabrikk("foo", "bar")
        private fun risikovurderingLøsning(funn: List<Risikofunn>) = objectMapper.readTree(meldingsfabrikk.lagRisikovurderingløsning(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            funn = funn
        )).path("@løsning").path("Risikovurdering")
    }

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearAllMocks()
        every { RISIKOVURDERING_DAO.hentRisikovurdering(VEDTAKSPERIODE_ID) } returns null
    }

    @Test
    fun `Sender behov for risikovurdering`() {
        val risikoCommand = risikoCommand()
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

    @Test
    fun `Om vi har fått løsning på en rett vedtaksperiode lagres den`() {
        every { RISIKOVURDERING_DAO.lagre(VEDTAKSPERIODE_ID, any(), any(), any(), any()) } returns
        context.add(Risikovurderingløsning(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            opprettet = LocalDateTime.now(),
            kanGodkjennesAutomatisk = true,
            løsning = risikovurderingLøsning(funn = listOf(Risikofunn(kategori = listOf("test"), beskrivele = "test", kreverSupersaksbehandler = false)))
        ))
        val risikoCommand = risikoCommand()
        assertTrue(risikoCommand.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 1) { RISIKOVURDERING_DAO.lagre(VEDTAKSPERIODE_ID, any(), any(), any(), any()) }
    }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode etterspør vi nytt behov`() {
        val enAnnenVedtaksperiodeId = UUID.randomUUID()
        context.add(Risikovurderingløsning(
            vedtaksperiodeId = enAnnenVedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            kanGodkjennesAutomatisk = true,
            løsning = risikovurderingLøsning(funn = listOf(Risikofunn(kategori = listOf("test"), beskrivele = "test", kreverSupersaksbehandler = false)))
        ))
        val risikoCommand = risikoCommand()
        risikoCommand.assertFalse()
        assertTrue(context.harBehov())
        assertEquals(setOf("Risikovurdering"), context.behov().keys)
        assertEquals(VEDTAKSPERIODE_ID, context.behov().entries.first().value["vedtaksperiodeId"])
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
        risikovurderingDao = risikovurderingDao,
        warningDao = warningDao,
        organisasjonsnummer = organisasjonsnummer,
        periodetype = periodetype
    )
}
