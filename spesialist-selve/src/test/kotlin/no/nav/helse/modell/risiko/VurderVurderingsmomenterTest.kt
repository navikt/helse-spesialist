package no.nav.helse.modell.risiko

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.util.januar
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

        private fun behovløsning(
            vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
            kanGodkjennesAutomatisk: Boolean = true
        ) = Risikovurderingløsning(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            løsning = objectMapper.readTree("""{}"""),
        )

    }

    private val behandling = Behandling(UUID.randomUUID(), testperson.vedtaksperiodeId1, 1.januar, 31.januar, 1.januar)
    private val sykefraværstilfelle =
        Sykefraværstilfelle(testperson.fødselsnummer, 1.januar, listOf(behandling))

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
        every { risikovurderingDao.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns null
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
                kunRefusjon = false,
                inntekt = inntekt()
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
                kunRefusjon = false,
                inntekt = inntekt()
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
                kunRefusjon = true,
                inntekt = inntekt()
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
                kunRefusjon = false,
                inntekt = inntekt()
            ), observer.behov.single()
        )
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() {
        every { risikovurderingDao.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns mockk()
        assertTrue(risikoCommand().resume(context))
        assertTrue(risikoCommand().execute(context))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Om vi har fått løsning på rett vedtaksperiode lagres den`() {
        every { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any()) } just Runs
        context.add(
            behovløsning()
        )
        val risikoCommand = risikoCommand()
        assertTrue(risikoCommand.execute(context))
        assertTrue(observer.behov.isEmpty())
        verify(exactly = 1) { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any()) }
    }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode sendes det behov`() {
        val enAnnenVedtaksperiodeId = UUID.randomUUID()
        context.add(
            behovløsning(
                vedtaksperiodeId = enAnnenVedtaksperiodeId
            )
        )

        assertFalse(risikoCommand().execute(context))
        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = true,
                inntekt = inntekt()
            ), observer.behov.single()
        )

        observer.behov.clear()

        assertFalse(risikoCommand().resume(context))
        assertEquals(
            Behov.Risikovurdering(
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                organisasjonsnummer = testperson.orgnummer,
                førstegangsbehandling = true,
                kunRefusjon = true,
                inntekt = inntekt()
            ), observer.behov.single()
        )
    }

    @Test
    fun `Lager varsel om risk-svaret tilsier det`() {
        every { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any()) } just Runs
        context.add(
            behovløsning(
                kanGodkjennesAutomatisk = false
            )
        )

        risikoCommand().execute(context)

        assertEquals(listOf("SB_RV_1"), behandling.toDto().varsler.map { it.varselkode })
    }

    private fun risikoCommand(
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        risikovurderingDao: RisikovurderingDao = this.risikovurderingDao,
        organisasjonsnummer: String = testperson.orgnummer,
        førstegangsbehandling: Boolean = true,
    ) = VurderVurderingsmomenter(
        vedtaksperiodeId = vedtaksperiodeId,
        risikovurderingDao = risikovurderingDao,
        organisasjonsnummer = organisasjonsnummer,
        førstegangsbehandling = førstegangsbehandling,
        sykefraværstilfelle = sykefraværstilfelle,
        utbetaling = utbetalingMock,
        spleisSykepengegrunnlangsfakta = SpleisSykepengegrunnlagsfakta(
            listOf(
                SykepengegrunnlagsArbeidsgiver(
                    omregnetÅrsinntekt = 123456.7,
                    arbeidsgiver = testperson.orgnummer,
                    inntektskilde = Inntektsopplysningkilde.Arbeidsgiver,
                    skjønnsfastsatt = null
                )
            ),
        )
    )

    private fun inntekt() = InntektTilRisk(
        omregnetÅrsinntekt = 123456.7,
        inntektskilde = "Arbeidsgiver"
    )
}
