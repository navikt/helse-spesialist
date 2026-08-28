package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.melding.StpPeriodeTilRisk
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.TestPerson
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

internal class VurderVurderingsmomenterTest : ApplicationTest() {
    private val utbetalingMock = mockk<Utbetaling>(relaxed = true)

    private companion object {
        private val testperson = TestPerson()

        private fun behovløsning(
            vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
            kanGodkjennesAutomatisk: Boolean = true,
        ) = Risikovurderingløsning(
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

    private val behandling =
        Behandling
            .ny(
                SpleisBehandlingId(UUID.randomUUID()),
                vedtaksperiodeId = VedtaksperiodeId(testperson.vedtaksperiodeId1),
                fom = 1 jan 2018,
                tom = 31 jan 2018,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also { sessionContext.behandlingRepository.lagre(it) }

    private val sykefraværstilfelle =
        Sykefraværstilfelle(testperson.fødselsnummer, 1 jan 2018, listOf(legacyBehandling))
    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }

    @Test
    fun `Sender behov for risikovurdering ved execute`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true
        val risikoCommand = risikoCommand()
        assertFalse(risikoCommand.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())

        assertEquals(
            risikovurdering(kunRefusjon = false),
            observer.behov.single(),
        )
    }

    @Test
    fun `Sender behov for risikovurdering ved resume dersom vi mangler løsning`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true
        val risikoCommand = risikoCommand()
        assertFalse(risikoCommand.resume(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())

        assertEquals(
            risikovurdering(kunRefusjon = false),
            observer.behov.single(),
        )
    }

    @Test
    fun `Sender kunRefusjon=true når det ikke skal utbetales noe til den sykmeldte`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns false

        assertFalse(risikoCommand().execute(commandContext, sessionContext, outbox))

        assertEquals(
            risikovurdering(kunRefusjon = true),
            observer.behov.single(),
        )
    }

    @Test
    fun `Sender kunRefusjon=false når det er utbetaling til den sykmeldte`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true

        assertFalse(risikoCommand().execute(commandContext, sessionContext, outbox))

        assertEquals(
            risikovurdering(kunRefusjon = false),
            observer.behov.single(),
        )
    }

    @Test
    fun `Går videre hvis risikovurderingen for vedtaksperioden allerede er gjort`() {
        sessionContext.risikovurderingDao.lagre(
            testperson.vedtaksperiodeId1,
            true,
            JsonNodeFactory.instance.objectNode(),
            LocalDateTime.now(),
        )
        assertTrue(risikoCommand().resume(commandContext, sessionContext, outbox))
        assertTrue(risikoCommand().execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isEmpty())
    }

    @Test
    fun `Om vi har fått løsning på rett vedtaksperiode lagres den`() {
        commandContext.add(
            behovløsning(),
        )
        val risikoCommand = risikoCommand()
        assertTrue(risikoCommand.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isEmpty())
        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(testperson.vedtaksperiodeId1))
    }

    @Test
    fun `Om vi har fått løsning på en annen vedtaksperiode sendes det behov`() {
        val enAnnenVedtaksperiodeId = UUID.randomUUID()
        commandContext.add(
            behovløsning(
                vedtaksperiodeId = enAnnenVedtaksperiodeId,
            ),
        )

        assertFalse(risikoCommand().execute(commandContext, sessionContext, outbox))
        assertEquals(
            risikovurdering(kunRefusjon = true),
            observer.behov.single(),
        )

        observer.behov.clear()

        assertFalse(risikoCommand().resume(commandContext, sessionContext, outbox))
        assertEquals(
            risikovurdering(kunRefusjon = true),
            observer.behov.single(),
        )
    }

    @Test
    fun `Lager varsel om risk-svaret tilsier det`() {
        commandContext.add(
            behovløsning(
                kanGodkjennesAutomatisk = false,
            ),
        )

        risikoCommand().execute(commandContext, sessionContext, outbox)

        assertEquals(listOf("SB_RV_1"), sessionContext.varselRepository.finnVarslerFor(behandling.id).map { it.kode })
    }

    private fun risikoCommand(
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        organisasjonsnummer: String = testperson.orgnummer,
        førstegangsbehandling: Boolean = true,
    ) = VurderVurderingsmomenter(
        vedtaksperiodeId = vedtaksperiodeId,
        periode = legacyBehandling.periode,
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
        spleisVedtaksperioder =
            listOf(
                SpleisVedtaksperiode(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    spleisBehandlingId = UUID.randomUUID(),
                    fom = legacyBehandling.periode.fom,
                    tom = legacyBehandling.periode.tom,
                    skjæringstidspunkt = legacyBehandling.skjæringstidspunkt,
                    yrkesaktivitet =
                        SpleisVedtaksperiode.Yrkesaktivitet(
                            organisasjonsnummer = testperson.orgnummer,
                            yrkesaktivitetstype = "ARBEIDSTAKER",
                        ),
                ),
                SpleisVedtaksperiode(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    spleisBehandlingId = UUID.randomUUID(),
                    fom = legacyBehandling.periode.fom,
                    tom = legacyBehandling.periode.tom,
                    skjæringstidspunkt = legacyBehandling.skjæringstidspunkt,
                    yrkesaktivitet = null,
                ),
                SpleisVedtaksperiode(
                    vedtaksperiodeId = testperson.vedtaksperiodeId1,
                    spleisBehandlingId = UUID.randomUUID(),
                    fom = legacyBehandling.periode.fom,
                    tom = legacyBehandling.periode.tom,
                    skjæringstidspunkt = legacyBehandling.skjæringstidspunkt,
                    yrkesaktivitet =
                        SpleisVedtaksperiode.Yrkesaktivitet(
                            organisasjonsnummer = null,
                            yrkesaktivitetstype = "SELVSTENDIG",
                        ),
                ),
            ),
    )

    private fun risikovurdering(kunRefusjon: Boolean) =
        Behov.Risikovurdering(
            vedtaksperiodeId = testperson.vedtaksperiodeId1,
            organisasjonsnummer = testperson.orgnummer,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            førstegangsbehandling = true,
            kunRefusjon = kunRefusjon,
            inntekt = inntekt(),
            periode = legacyBehandling.periode,
            skjæringstidspunkt = legacyBehandling.skjæringstidspunkt,
            perioderMedSammeSkjæringstidspunkt =
                listOf(
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = testperson.vedtaksperiodeId1,
                        fom = legacyBehandling.periode.fom,
                        tom = legacyBehandling.periode.tom,
                        organisasjonsnummer = testperson.orgnummer,
                        yrkesaktivitetstype = "ARBEIDSTAKER",
                    ),
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = testperson.vedtaksperiodeId1,
                        fom = legacyBehandling.periode.fom,
                        tom = legacyBehandling.periode.tom,
                        organisasjonsnummer = null,
                        yrkesaktivitetstype = null,
                    ),
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = testperson.vedtaksperiodeId1,
                        fom = legacyBehandling.periode.fom,
                        tom = legacyBehandling.periode.tom,
                        organisasjonsnummer = null,
                        yrkesaktivitetstype = "SELVSTENDIG",
                    ),
                ),
        )

    private fun inntekt() =
        InntektTilRisk(
            omregnetÅrsinntekt = 123456.7,
            inntektskilde = "Arbeidsgiver",
        )
}
