package no.nav.helse.modell.risiko

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.objectMapper
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RisikoCommandTest {

    private val risikovurderingDao = mockk<RisikovurderingDao>()
    private val utbetalingMock = mockk<GraphQLUtbetaling>(relaxed = true)

    private companion object {
        private const val ORGNUMMER = "123456789"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()

        private val meldingsfabrikk = Testmeldingfabrikk("foo", "bar")
        private fun risikovurderingLøsning(funn: List<Risikofunn>) = objectMapper.readTree(meldingsfabrikk.lagRisikovurderingløsning(
            aktørId = AKTØR,
            fødselsnummer = FØDSELSNUMMER,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            funn = funn
        )).path("@løsning").path("Risikovurdering")
    }

    private val generasjon = Generasjon(UUID.randomUUID(), VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar)
    private val sykefraværstilfelle = Sykefraværstilfelle(FØDSELSNUMMER, 1.januar, listOf(generasjon))


    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearAllMocks()
        every { risikovurderingDao.hentRisikovurdering(VEDTAKSPERIODE_ID) } returns null
    }

    @Test
    fun `Sender behov for risikovurdering`() {
        val risikoCommand = risikoCommand()
        risikoCommand.assertFalse()
        assertTrue(context.harBehov())
        assertEquals(1, context.behov().size)
        assertEquals("Risikovurdering", context.behov().keys.first())
        assertTrue(context.behov().getValue("Risikovurdering").keys.contains("førstegangsbehandling"))
    }

    @Test
    fun `Sender kunRefusjon=true når det ikke skal utbetales noe til den sykmeldte`() {
        every { utbetalingMock.arbeidsgiverNettoBelop } returns 1
        every { utbetalingMock.personNettoBelop } returns 0

        risikoCommand().assertFalse()

        val risikobehov = context.behov().getValue("Risikovurdering")
        assertTrue(risikobehov["kunRefusjon"] as Boolean)
    }

    @Test
    fun `Sender kunRefusjon=false når det er utbetaling til den sykmeldte`() {
        every { utbetalingMock.arbeidsgiverNettoBelop } returns 1
        every { utbetalingMock.personNettoBelop } returns 1

        risikoCommand().assertFalse()

        val risikobehov = context.behov().getValue("Risikovurdering")
        assertFalse(risikobehov["kunRefusjon"] as Boolean)
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() {
        every { risikovurderingDao.hentRisikovurdering(VEDTAKSPERIODE_ID) } returns mockk()
        val risikoCommand = risikoCommand()
        risikoCommand.assertTrue()
        assertFalse(context.harBehov())
    }

    @Test
    fun `Om vi har fått løsning på en rett vedtaksperiode lagres den`() {
        every { risikovurderingDao.lagre(VEDTAKSPERIODE_ID, any(), any(), any(), any()) } returns
        context.add(
            Risikovurderingløsning(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            opprettet = LocalDateTime.now(),
            kanGodkjennesAutomatisk = true,
            løsning = risikovurderingLøsning(funn = listOf(Risikofunn(kategori = listOf("test"), beskrivelse = "test", kreverSupersaksbehandler = false)))
        )
        )
        val risikoCommand = risikoCommand()
        assertTrue(risikoCommand.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 1) { risikovurderingDao.lagre(VEDTAKSPERIODE_ID, any(), any(), any(), any()) }
    }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode etterspør vi nytt behov`() {
        val enAnnenVedtaksperiodeId = UUID.randomUUID()
        context.add(
            Risikovurderingløsning(
            vedtaksperiodeId = enAnnenVedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            kanGodkjennesAutomatisk = true,
            løsning = risikovurderingLøsning(funn = listOf(Risikofunn(kategori = listOf("test"), beskrivelse = "test", kreverSupersaksbehandler = false)))
        )
        )
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
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        risikovurderingDao: RisikovurderingDao = this.risikovurderingDao,
        organisasjonsnummer: String = ORGNUMMER,
        førstegangsbehandling: Boolean = true
    ) = RisikoCommand(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        risikovurderingDao = risikovurderingDao,
        organisasjonsnummer = organisasjonsnummer,
        førstegangsbehandling = førstegangsbehandling,
        sykefraværstilfelle = sykefraværstilfelle,
    ) { utbetalingMock }
}
