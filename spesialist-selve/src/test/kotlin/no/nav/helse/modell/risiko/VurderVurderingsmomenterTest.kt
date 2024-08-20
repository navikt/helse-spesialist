package no.nav.helse.modell.risiko

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VurderVurderingsmomenterTest {
    private val risikovurderingDao = mockk<RisikovurderingDao>()
    private val utbetalingMock = mockk<Utbetaling>(relaxed = true)

    private companion object {
        private val testperson = TestPerson()

        private fun risikovurderingLøsning(funn: List<Risikofunn>) =
            objectMapper.readTree(
                Testmeldingfabrikk.lagRisikovurderingløsning(
                    aktørId = testperson.aktørId,
                    fødselsnummer = testperson.fødselsnummer,
                    organisasjonsnummer = testperson.orgnummer,
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    funn = funn,
                ),
            ).path("@løsning").path("Risikovurdering")
    }

    private val generasjon = Generasjon(UUID.randomUUID(), testperson.vedtaksperiodeId1, 1.januar, 31.januar, 1.januar)
    private val sykefraværstilfelle =
        Sykefraværstilfelle(testperson.fødselsnummer, 1.januar, listOf(generasjon))

    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableMapOf<String, Map<String, Any>>()

            override fun behov(
                behov: String,
                ekstraKontekst: Map<String, Any>,
                detaljer: Map<String, Any>,
            ) {
                this.behov[behov] = detaljer
            }

            override fun hendelse(hendelse: String) {}
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        every { risikovurderingDao.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns null
    }

    @Test
    fun `Sender behov for risikovurdering`() {
        val risikoCommand = risikoCommand()
        risikoCommand.assertFalse()
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(1, observer.behov.size)
        assertEquals("Risikovurdering", observer.behov.keys.first())
        assertTrue(observer.behov.getValue("Risikovurdering").keys.contains("førstegangsbehandling"))
    }

    @Test
    fun `Sender kunRefusjon=true når det ikke skal utbetales noe til den sykmeldte`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns false

        risikoCommand().assertFalse()

        val risikobehov = observer.behov.getValue("Risikovurdering")
        assertTrue(risikobehov["kunRefusjon"] as Boolean)
    }

    @Test
    fun `Sender kunRefusjon=false når det er utbetaling til den sykmeldte`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true

        risikoCommand().assertFalse()

        val risikobehov = observer.behov.getValue("Risikovurdering")
        assertFalse(risikobehov["kunRefusjon"] as Boolean)
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() {
        every { risikovurderingDao.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns mockk()
        val risikoCommand = risikoCommand()
        risikoCommand.assertTrue()
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Om vi har fått løsning på en rett vedtaksperiode lagres den`() {
        every { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any(), any()) } returns
            context.add(
                Risikovurderingløsning(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    opprettet = LocalDateTime.now(),
                    kanGodkjennesAutomatisk = true,
                    løsning =
                        risikovurderingLøsning(
                            funn =
                                listOf(
                                    Risikofunn(
                                        kategori = listOf("test"),
                                        beskrivelse = "test",
                                        kreverSupersaksbehandler = false,
                                    ),
                                ),
                        ),
                ),
            )
        val risikoCommand = risikoCommand()
        assertTrue(risikoCommand.execute(context))
        assertTrue(observer.behov.isEmpty())
        verify(exactly = 1) { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any(), any()) }
    }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode etterspør vi nytt behov`() {
        val enAnnenVedtaksperiodeId = UUID.randomUUID()
        context.add(
            Risikovurderingløsning(
                vedtaksperiodeId = enAnnenVedtaksperiodeId,
                opprettet = LocalDateTime.now(),
                kanGodkjennesAutomatisk = true,
                løsning =
                    risikovurderingLøsning(
                        funn =
                            listOf(
                                Risikofunn(
                                    kategori = listOf("test"),
                                    beskrivelse = "test",
                                    kreverSupersaksbehandler = false,
                                ),
                            ),
                    ),
            ),
        )
        val risikoCommand = risikoCommand()
        risikoCommand.assertFalse()
        assertTrue(observer.behov.isNotEmpty())
        assertEquals(setOf("Risikovurdering"), observer.behov.keys)
        assertEquals(testperson.vedtaksperiodeId1, observer.behov.entries.first().value["vedtaksperiodeId"])
    }

    private fun VurderVurderingsmomenter.assertTrue() {
        assertTrue(resume(context))
        assertTrue(execute(context))
    }

    private fun VurderVurderingsmomenter.assertFalse() {
        assertFalse(resume(context))
        assertFalse(execute(context))
    }

    private fun risikoCommand(
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        risikovurderingDao: RisikovurderingDao = this.risikovurderingDao,
        organisasjonsnummer: String = testperson.orgnummer,
        førstegangsbehandling: Boolean = true,
    ) = VurderVurderingsmomenter(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        risikovurderingDao = risikovurderingDao,
        organisasjonsnummer = organisasjonsnummer,
        førstegangsbehandling = førstegangsbehandling,
        sykefraværstilfelle = sykefraværstilfelle,
        utbetaling = utbetalingMock,
    )
}
