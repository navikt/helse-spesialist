package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.melding.StpPeriodeTilRisk
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

internal class VurderVurderingsmomenterTest : ApplicationTest() {
    private val utbetalingMock = mockk<Utbetaling>(relaxed = true)

    private fun behovløsning(
        vedtaksperiodeId: UUID = vedtaksperiode1.id.value,
        kanGodkjennesAutomatisk: Boolean = true,
    ) = Risikovurderingløsning(
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
        løsning = JsonNodeFactory.instance.objectNode(),
    )

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
            vedtaksperiode1.id.value,
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
        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode1.id.value))
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

        behandling1.assertAntallVarsler(1)
        behandling1.assertHarVarsel(forventetKode = "SB_RV_1", forventetStatus = Varsel.Status.AKTIV)
    }

    private fun risikoCommand(
        spleisBehandlingId: SpleisBehandlingId = behandling1.spleisBehandlingId!!,
        organisasjonsnummer: String = vedtaksperiode1.organisasjonsnummer,
        førstegangsbehandling: Boolean = true,
    ) = VurderVurderingsmomenter(
        spleisBehandlingId = spleisBehandlingId,
        periode = Periode(behandling1.fom, behandling1.tom),
        organisasjonsnummer = organisasjonsnummer,
        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        førstegangsbehandling = førstegangsbehandling,
        utbetaling = utbetalingMock,
        sykepengegrunnlagsfakta =
            Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
                arbeidsgivere =
                    listOf(
                        Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                            omregnetÅrsinntekt = 123456.7,
                            organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
                            inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver,
                        ),
                    ),
                seksG = 6 * 118620.0,
                sykepengegrunnlag = BigDecimal("123456.7"),
            ),
        spleisVedtaksperioder =
            listOf(
                SpleisVedtaksperiode(
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                    fom = behandling1.fom,
                    tom = behandling1.tom,
                    skjæringstidspunkt = behandling1.skjæringstidspunkt,
                    yrkesaktivitet =
                        SpleisVedtaksperiode.Yrkesaktivitet(
                            organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
                            yrkesaktivitetstype = "ARBEIDSTAKER",
                        ),
                ),
                SpleisVedtaksperiode(
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                    fom = behandling1.fom,
                    tom = behandling1.tom,
                    skjæringstidspunkt = behandling1.skjæringstidspunkt,
                    yrkesaktivitet = null,
                ),
                SpleisVedtaksperiode(
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                    fom = behandling1.fom,
                    tom = behandling1.tom,
                    skjæringstidspunkt = behandling1.skjæringstidspunkt,
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
            vedtaksperiodeId = vedtaksperiode1.id.value,
            organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            førstegangsbehandling = true,
            kunRefusjon = kunRefusjon,
            inntekt = inntekt(),
            periode = Periode(behandling1.fom, behandling1.tom),
            skjæringstidspunkt = behandling1.skjæringstidspunkt,
            perioderMedSammeSkjæringstidspunkt =
                listOf(
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = vedtaksperiode1.id.value,
                        fom = behandling1.fom,
                        tom = behandling1.tom,
                        organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
                        yrkesaktivitetstype = "ARBEIDSTAKER",
                    ),
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = vedtaksperiode1.id.value,
                        fom = behandling1.fom,
                        tom = behandling1.tom,
                        organisasjonsnummer = null,
                        yrkesaktivitetstype = null,
                    ),
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = vedtaksperiode1.id.value,
                        fom = behandling1.fom,
                        tom = behandling1.tom,
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
