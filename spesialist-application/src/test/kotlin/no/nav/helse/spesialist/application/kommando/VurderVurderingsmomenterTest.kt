package no.nav.helse.spesialist.application.kommando

import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.TestPerson
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

internal class VurderVurderingsmomenterTest {
    private val risikovurderingDao = mockk<RisikovurderingDao>()
    private val utbetalingMock = mockk<Utbetaling>(relaxed = true)

    private companion object {
        private val testperson = TestPerson()

        private fun behovløsning(
            vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
            kanGodkjennesAutomatisk: Boolean = true,
        ) = `Risikovurderingløsning`(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDateTime.now(),
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            løsning = JsonNodeFactory.instance.objectNode(),
        )
    }

    private val legacyBehandling =
        LegacyBehandling(
            id = UUID.randomUUID(),
            vedtaksperiodeId = testperson.vedtaksperiodeId1,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            skjæringstidspunkt = 1 jan 2018,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        )
    private val sykefraværstilfelle =
        `Sykefraværstilfelle`(testperson.fødselsnummer, 1 jan 2018, listOf(legacyBehandling))

    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
            ) {
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
    fun `Sender behov for risikovurdering ved execute`() =
        testMedSessionContext {
            every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true
            val risikoCommand = risikoCommand()
            Assertions.assertFalse(risikoCommand.execute(context, it))
            Assertions.assertTrue(observer.behov.isNotEmpty())

            Assertions.assertEquals(
                Behov.Risikovurdering(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    organisasjonsnummer = testperson.orgnummer,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    førstegangsbehandling = true,
                    kunRefusjon = false,
                    inntekt = inntekt(),
                ),
                observer.behov.single(),
            )
        }

    @Test
    fun `Sender behov for risikovurdering ved resume dersom vi mangler løsning`() =
        testMedSessionContext {
            every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true
            val risikoCommand = risikoCommand()
            Assertions.assertFalse(risikoCommand.resume(context, it))
            Assertions.assertTrue(observer.behov.isNotEmpty())

            Assertions.assertEquals(
                Behov.Risikovurdering(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    organisasjonsnummer = testperson.orgnummer,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    førstegangsbehandling = true,
                    kunRefusjon = false,
                    inntekt = inntekt(),
                ),
                observer.behov.single(),
            )
        }

    @Test
    fun `Sender kunRefusjon=true når det ikke skal utbetales noe til den sykmeldte`() =
        testMedSessionContext {
            every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns false

            Assertions.assertFalse(risikoCommand().execute(context, it))

            Assertions.assertEquals(
                Behov.Risikovurdering(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    organisasjonsnummer = testperson.orgnummer,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    førstegangsbehandling = true,
                    kunRefusjon = true,
                    inntekt = inntekt(),
                ),
                observer.behov.single(),
            )
        }

    @Test
    fun `Sender kunRefusjon=false når det er utbetaling til den sykmeldte`() =
        testMedSessionContext {
            every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true

            Assertions.assertFalse(risikoCommand().execute(context, it))

            Assertions.assertEquals(
                Behov.Risikovurdering(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    organisasjonsnummer = testperson.orgnummer,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    førstegangsbehandling = true,
                    kunRefusjon = false,
                    inntekt = inntekt(),
                ),
                observer.behov.single(),
            )
        }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() =
        testMedSessionContext {
            every { risikovurderingDao.hentRisikovurdering(testperson.vedtaksperiodeId1) } returns mockk()
            Assertions.assertTrue(risikoCommand().resume(context, it))
            Assertions.assertTrue(risikoCommand().execute(context, it))
            Assertions.assertTrue(observer.behov.isEmpty())
        }

    @Test
    fun `Om vi har fått løsning på rett vedtaksperiode lagres den`() =
        testMedSessionContext {
            every { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any()) } just Runs
            context.add(
                behovløsning(),
            )
            val risikoCommand = risikoCommand()
            Assertions.assertTrue(risikoCommand.execute(context, it))
            Assertions.assertTrue(observer.behov.isEmpty())
            verify(exactly = 1) { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any()) }
        }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode sendes det behov`() =
        testMedSessionContext {
            val enAnnenVedtaksperiodeId = UUID.randomUUID()
            context.add(
                behovløsning(
                    vedtaksperiodeId = enAnnenVedtaksperiodeId,
                ),
            )

            Assertions.assertFalse(risikoCommand().execute(context, it))
            Assertions.assertEquals(
                Behov.Risikovurdering(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    organisasjonsnummer = testperson.orgnummer,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    førstegangsbehandling = true,
                    kunRefusjon = true,
                    inntekt = inntekt(),
                ),
                observer.behov.single(),
            )

            observer.behov.clear()

            Assertions.assertFalse(risikoCommand().resume(context, it))
            Assertions.assertEquals(
                Behov.Risikovurdering(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    organisasjonsnummer = testperson.orgnummer,
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                    førstegangsbehandling = true,
                    kunRefusjon = true,
                    inntekt = inntekt(),
                ),
                observer.behov.single(),
            )
        }

    @Test
    fun `Lager varsel om risk-svaret tilsier det`() =
        testMedSessionContext {
            every { risikovurderingDao.lagre(testperson.vedtaksperiodeId1, any(), any(), any()) } just Runs
            context.add(
                behovløsning(
                    kanGodkjennesAutomatisk = false,
                ),
            )

            risikoCommand().execute(context, it)

            Assertions.assertEquals(listOf("SB_RV_1"), legacyBehandling.toDto().varsler.map { it.varselkode })
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
        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        førstegangsbehandling = førstegangsbehandling,
        sykefraværstilfelle = sykefraværstilfelle,
        utbetaling = utbetalingMock,
        sykepengegrunnlagsfakta =
            Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
                arbeidsgivere =
                    listOf(
                        Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                            omregnetÅrsinntekt = 123456.7,
                            organisasjonsnummer = testperson.orgnummer,
                            inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver,
                        ),
                    ),
                seksG = 6 * 118620.0,
                sykepengegrunnlag = BigDecimal("123456.7"),
            ),
    )

    private fun inntekt() =
        InntektTilRisk(
            omregnetÅrsinntekt = 123456.7,
            inntektskilde = "Arbeidsgiver",
        )
}
