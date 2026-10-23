package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.melding.StpPeriodeTilRisk
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
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
        andreVedtaksperioder: List<Risikovurderingløsning.AnnenVedtaksperiode> = emptyList(),
    ) = Risikovurderingløsning(
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
        løsning = JsonNodeFactory.instance.objectNode(),
        andreVedtaksperioder = andreVedtaksperioder,
    )

    private fun annenVedtaksperiode(
        vedtaksperiodeId: UUID = vedtaksperiode2.id.value,
        kanGodkjennesAutomatisk: Boolean = false,
    ) = Risikovurderingløsning.AnnenVedtaksperiode(
        vedtaksperiodeId = vedtaksperiodeId,
        kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
        løsning = JsonNodeFactory.instance.objectNode(),
    )

    private fun spleisBehandling(behandling: Behandling) =
        SpleisBehandling(
            vedtaksperiodeId = behandling.vedtaksperiodeId.value,
            spleisBehandlingId = behandling.spleisBehandlingId!!.value,
            fom = behandling.fom,
            tom = behandling.tom,
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            yrkesaktivitet = SpleisBehandling.Yrkesaktivitet(organisasjonsnummer = "987654321", yrkesaktivitetstype = "ARBEIDSTAKER"),
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

    @Test
    fun `Uten risikovurdering av andre vedtaksperioder i løsningen lagres ingenting for andre vedtaksperioder`() {
        commandContext.add(behovløsning(kanGodkjennesAutomatisk = false))

        assertTrue(risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(behandling2))).execute(commandContext, sessionContext, outbox))

        assertEquals(0, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode2.id.value))
        behandling2.assertAntallVarsler(0)
        behandling1.assertHarVarsel(forventetKode = "SB_RV_1", forventetStatus = Varsel.Status.AKTIV)
    }

    @Test
    fun `Lagrer risikovurdering og lager varsel for annen vedtaksperiode med samme skjæringstidspunkt`() {
        commandContext.add(behovløsning(andreVedtaksperioder = listOf(annenVedtaksperiode(kanGodkjennesAutomatisk = false))))

        assertTrue(risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(behandling2))).execute(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode1.id.value))
        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode2.id.value))
        behandling1.assertAntallVarsler(0)
        behandling2.assertAntallVarsler(1)
        behandling2.assertHarVarsel(forventetKode = "SB_RV_1", forventetStatus = Varsel.Status.AKTIV)
    }

    @Test
    fun `Lagrer risikovurdering uten varsel for annen vedtaksperiode som kan godkjennes automatisk`() {
        commandContext.add(behovløsning(andreVedtaksperioder = listOf(annenVedtaksperiode(kanGodkjennesAutomatisk = true))))

        assertTrue(risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(behandling2))).execute(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode2.id.value))
        behandling2.assertAntallVarsler(0)
    }

    @Test
    fun `Forhåndslagret risikovurdering brukes når den andre vedtaksperioden kommer til godkjenning`() {
        commandContext.add(behovløsning(andreVedtaksperioder = listOf(annenVedtaksperiode(kanGodkjennesAutomatisk = false))))
        risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(behandling2))).execute(commandContext, sessionContext, outbox)

        val commandContextForVedtaksperiode2 = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }
        assertTrue(
            risikoCommand(spleisBehandlingId = behandling2.spleisBehandlingId!!).execute(commandContextForVedtaksperiode2, sessionContext, outbox),
        )

        assertTrue(observer.behov.isEmpty())
        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode2.id.value))
        behandling2.assertAntallVarsler(1)
    }

    @Test
    fun `Ignorerer risikovurdering for vedtaksperiode som ikke var blant perioderMedSammeSkjæringstidspunkt`() {
        commandContext.add(behovløsning(andreVedtaksperioder = listOf(annenVedtaksperiode(kanGodkjennesAutomatisk = false))))

        assertTrue(risikoCommand().execute(commandContext, sessionContext, outbox))

        assertEquals(0, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode2.id.value))
        behandling2.assertAntallVarsler(0)
    }

    @Test
    fun `Ignorerer risikovurdering for annen vedtaksperiode som allerede er risikovurdert`() {
        sessionContext.risikovurderingDao.lagre(vedtaksperiode2.id.value, true, JsonNodeFactory.instance.objectNode(), LocalDateTime.now())
        commandContext.add(behovløsning(andreVedtaksperioder = listOf(annenVedtaksperiode(kanGodkjennesAutomatisk = false))))

        assertTrue(risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(behandling2))).execute(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode2.id.value))
        behandling2.assertAntallVarsler(0)
    }

    @Test
    fun `Ignorerer risikovurdering for annen vedtaksperiode uten åpen behandling`() {
        val vedtaksperiode3 = lagVedtaksperiode(identitetsnummer = person.id).also { sessionContext.vedtaksperiodeRepository.lagre(it) }
        val lukketBehandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode3.id,
                tilstand = Behandling.Tilstand.VedtakFattet,
                fom = behandling1.fom,
                tom = behandling1.tom,
                skjæringstidspunkt = behandling1.skjæringstidspunkt,
            ).also { sessionContext.behandlingRepository.lagre(it) }
        commandContext.add(
            behovløsning(andreVedtaksperioder = listOf(annenVedtaksperiode(vedtaksperiodeId = vedtaksperiode3.id.value, kanGodkjennesAutomatisk = false))),
        )

        assertTrue(risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(lukketBehandling))).execute(commandContext, sessionContext, outbox))

        assertEquals(0, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode3.id.value))
        lukketBehandling.assertAntallVarsler(0)
    }

    @Test
    fun `Ignorerer risikovurdering av egen vedtaksperiode i andreVedtaksperioder`() {
        commandContext.add(
            behovløsning(
                kanGodkjennesAutomatisk = true,
                andreVedtaksperioder = listOf(annenVedtaksperiode(vedtaksperiodeId = vedtaksperiode1.id.value, kanGodkjennesAutomatisk = false)),
            ),
        )

        assertTrue(risikoCommand().execute(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.risikovurderingDao.antallLagret(vedtaksperiode1.id.value))
        behandling1.assertAntallVarsler(0)
    }

    @Test
    fun `Sender inntekt for hver periode med samme skjæringstidspunkt`() {
        every { utbetalingMock.harEndringIUtbetalingTilSykmeldt() } returns true

        assertFalse(risikoCommand(andreSpleisVedtaksperioder = listOf(spleisBehandling(behandling2))).execute(commandContext, sessionContext, outbox))

        val perioder = (observer.behov.single() as Behov.Risikovurdering).perioderMedSammeSkjæringstidspunkt
        assertEquals(inntekt(), perioder.first { it.vedtaksperiodeId == vedtaksperiode1.id.value && it.organisasjonsnummer != null }.inntekt)
        assertEquals(
            InntektTilRisk(omregnetÅrsinntekt = 654321.0, inntektskilde = "AOrdningen"),
            perioder.single { it.vedtaksperiodeId == vedtaksperiode2.id.value }.inntekt,
        )
    }

    private fun risikoCommand(
        spleisBehandlingId: SpleisBehandlingId = behandling1.spleisBehandlingId!!,
        organisasjonsnummer: String = vedtaksperiode1.organisasjonsnummer,
        førstegangsbehandling: Boolean = true,
        andreSpleisVedtaksperioder: List<SpleisBehandling> = emptyList(),
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
                        Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                            omregnetÅrsinntekt = 654321.0,
                            organisasjonsnummer = "987654321",
                            inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.AOrdningen,
                        ),
                    ),
                seksG = 6 * 118620.0,
                sykepengegrunnlag = BigDecimal("123456.7"),
            ),
        spleisVedtaksperioder =
            listOf(
                SpleisBehandling(
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                    fom = behandling1.fom,
                    tom = behandling1.tom,
                    skjæringstidspunkt = behandling1.skjæringstidspunkt,
                    yrkesaktivitet =
                        SpleisBehandling.Yrkesaktivitet(
                            organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
                            yrkesaktivitetstype = "ARBEIDSTAKER",
                        ),
                ),
                SpleisBehandling(
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                    fom = behandling1.fom,
                    tom = behandling1.tom,
                    skjæringstidspunkt = behandling1.skjæringstidspunkt,
                    yrkesaktivitet = null,
                ),
                SpleisBehandling(
                    vedtaksperiodeId = vedtaksperiode1.id.value,
                    spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
                    fom = behandling1.fom,
                    tom = behandling1.tom,
                    skjæringstidspunkt = behandling1.skjæringstidspunkt,
                    yrkesaktivitet =
                        SpleisBehandling.Yrkesaktivitet(
                            organisasjonsnummer = null,
                            yrkesaktivitetstype = "SELVSTENDIG",
                        ),
                ),
            ) + andreSpleisVedtaksperioder,
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
                        inntekt = inntekt(),
                    ),
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = vedtaksperiode1.id.value,
                        fom = behandling1.fom,
                        tom = behandling1.tom,
                        organisasjonsnummer = null,
                        yrkesaktivitetstype = null,
                        inntekt = null,
                    ),
                    StpPeriodeTilRisk(
                        vedtaksperiodeId = vedtaksperiode1.id.value,
                        fom = behandling1.fom,
                        tom = behandling1.tom,
                        organisasjonsnummer = null,
                        yrkesaktivitetstype = "SELVSTENDIG",
                        inntekt = null,
                    ),
                ),
        )

    private fun inntekt() =
        InntektTilRisk(
            omregnetÅrsinntekt = 123456.7,
            inntektskilde = "Arbeidsgiver",
        )
}
