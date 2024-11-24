package no.nav.helse.modell.risiko

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.RisikovurderingRepository
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.behov.Behov
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
    private val risikovurderingRepository = mockk<RisikovurderingRepository>()
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
            val behov = mutableListOf<Behov>()

            override fun behov(behov: Behov, commandContextId: UUID) {
                this.behov.add(behov)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        every { risikovurderingRepository.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns null
    }

    @Test
    fun `Sender behov for risikovurdering ved execute`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true
        val risikoCommand = risikoCommand()
        assertFalse(risikoCommand.execute(context))
        assertTrue(observer.behov.isNotEmpty())

        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = false
            ), observer.behov.single()
        )
    }

    @Test
    fun `Sender behov for risikovurdering ved resume dersom vi mangler løsning`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true
        val risikoCommand = risikoCommand()
        assertFalse(risikoCommand.resume(context))
        assertTrue(observer.behov.isNotEmpty())

        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = false
            ), observer.behov.single()
        )
    }

    @Test
    fun `Sender kunRefusjon=true når det ikke skal utbetales noe til den sykmeldte`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns false

        assertFalse(risikoCommand().execute(context))

        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = true
            ), observer.behov.single()
        )
    }

    @Test
    fun `Sender kunRefusjon=false når det er utbetaling til den sykmeldte`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true

        assertFalse(risikoCommand().execute(context))

        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = false
            ), observer.behov.single()
        )
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() {
        every { risikovurderingRepository.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns mockk()
        assertTrue(risikoCommand().resume(context))
        assertTrue(risikoCommand().execute(context))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Om vi har fått løsning på rett vedtaksperiode lagres den`() {
        every {
            risikovurderingRepository.lagre(testperson.vedtaksperiodeId1, any(), any(), any(), any())
        } returns context.add(
            Risikovurderingløsning(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                opprettet = LocalDateTime.now(),
                kanGodkjennesAutomatisk = true,
                løsning = risikovurderingLøsning(
                    funn = listOf(
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
        verify(exactly = 1) { risikovurderingRepository.lagre(testperson.vedtaksperiodeId1, any(), any(), any(), any()) }
    }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode etterspør vi nytt behov`() {
        val enAnnenVedtaksperiodeId = UUID.randomUUID()
        context.add(
            Risikovurderingløsning(
                vedtaksperiodeId = enAnnenVedtaksperiodeId,
                opprettet = LocalDateTime.now(),
                kanGodkjennesAutomatisk = true,
                løsning = risikovurderingLøsning(
                    funn = listOf(
                        Risikofunn(
                            kategori = listOf("test"),
                            beskrivelse = "test",
                            kreverSupersaksbehandler = false,
                        ),
                    ),
                ),
            ),
        )

        assertFalse(risikoCommand().execute(context))
        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = true
            ), observer.behov.single()
        )

        observer.behov.clear()

        assertFalse(risikoCommand().resume(context))
        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = true
            ), observer.behov.single()
        )
    }

    private fun risikoCommand(
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        risikovurderingRepository: RisikovurderingRepository = this.risikovurderingRepository,
        organisasjonsnummer: String = testperson.orgnummer,
        førstegangsbehandling: Boolean = true,
    ) = VurderVurderingsmomenter(
        vedtaksperiodeId = vedtaksperiodeId,
        risikovurderingRepository = risikovurderingRepository,
        organisasjonsnummer = organisasjonsnummer,
        førstegangsbehandling = førstegangsbehandling,
        sykefraværstilfelle = sykefraværstilfelle,
        utbetaling = utbetalingMock,
    )
}
