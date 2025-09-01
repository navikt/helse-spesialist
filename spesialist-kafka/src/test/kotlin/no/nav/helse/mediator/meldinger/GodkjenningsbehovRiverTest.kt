package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.GodkjenningsbehovRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class GodkjenningsbehovRiverTest {
    private val HENDELSE = UUID.randomUUID()
    private val VEDTAKSPERIODE = UUID.randomUUID()
    private val UTBETALING_ID = UUID.randomUUID()
    private val FNR = "12345678911"
    private val AKTØR = "1234567891234"
    private val ORGNR = "123456789"
    private val FOM = LocalDate.of(2020, 1, 1)
    private val TOM = LocalDate.of(2020, 1, 31)

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(GodkjenningsbehovRiver(mediator))

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser Godkjenningbehov`() {
        val relevanteArbeidsforhold = listOf(ORGNR)
        val vilkårsgrunnlagId = UUID.randomUUID()
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = AKTØR,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                organisasjonsnummer = ORGNR,
                periodeFom = FOM,
                periodeTom = TOM,
                skjæringstidspunkt = FOM,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
                førstegangsbehandling = true,
                utbetalingtype = Utbetalingtype.UTBETALING,
                inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
                orgnummereMedRelevanteArbeidsforhold = relevanteArbeidsforhold,
                kanAvvises = true,
                id = HENDELSE,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                sykepengegrunnlagsfakta = Testmeldingfabrikk.godkjenningsbehovFastsattEtterHovedregel(
                    seksG = 6 * 118620.0,
                    arbeidsgivere = listOf(
                        mapOf(
                            "arbeidsgiver" to ORGNR,
                            "omregnetÅrsinntekt" to 600000.00,
                            "inntektskilde" to "Arbeidsgiver"
                        )
                    )
                )
            )
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding = withArg<Godkjenningsbehov> {
                    assertEquals(HENDELSE, it.id)
                    assertEquals(FNR, it.fødselsnummer())
                    assertEquals(VEDTAKSPERIODE, it.vedtaksperiodeId())
                    assertEquals(ORGNR, it.organisasjonsnummer)
                    assertEquals(FOM, it.periodeFom)
                    assertEquals(TOM, it.periodeTom)
                    assertEquals(FOM, it.skjæringstidspunkt)
                    assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, it.inntektskilde)
                    assertEquals(relevanteArbeidsforhold, it.orgnummereMedRelevanteArbeidsforhold)
                    assertEquals(true, it.kanAvvises)
                    assertEquals(true, it.førstegangsbehandling)
                    assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, it.periodetype)
                    assertEquals(Utbetalingtype.UTBETALING, it.utbetalingtype)
                    assertEquals(
                        Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                            seksG = 6 * 118620.0,
                            arbeidsgivere = listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                    ORGNR,
                                    omregnetÅrsinntekt = 600000.0,
                                    inntektskilde = Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver
                                )
                            ),
                            sykepengegrunnlag = 600000.0
                        ),
                        it.sykepengegrunnlagsfakta
                    )
                },
                kontekstbasertPubliserer = any()
            )
        }
    }

    @Test
    fun `leser Godkjenningbehov fastsatt etter skjønn`() {
        val relevanteArbeidsforhold = listOf(ORGNR)
        val vilkårsgrunnlagId = UUID.randomUUID()
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = AKTØR,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                organisasjonsnummer = ORGNR,
                periodeFom = FOM,
                periodeTom = TOM,
                skjæringstidspunkt = FOM,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
                førstegangsbehandling = true,
                utbetalingtype = Utbetalingtype.UTBETALING,
                inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
                orgnummereMedRelevanteArbeidsforhold = relevanteArbeidsforhold,
                kanAvvises = true,
                id = HENDELSE,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                sykepengegrunnlagsfakta = Testmeldingfabrikk.godkjenningsbehovFastsattEtterSkjønn(
                    sykepengegrunnlag = 500000.00,
                    seksG = 6 * 118620.0,
                    arbeidsgivere = listOf(
                        mapOf(
                            "arbeidsgiver" to ORGNR,
                            "omregnetÅrsinntekt" to 500000.00,
                            "skjønnsfastsatt" to 600000.00,
                            "inntektskilde" to "Saksbehandler"
                        )
                    )
                ),
            )
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding = withArg<Godkjenningsbehov> {
                    assertEquals(HENDELSE, it.id)
                    assertEquals(FNR, it.fødselsnummer())
                    assertEquals(VEDTAKSPERIODE, it.vedtaksperiodeId())
                    assertEquals(ORGNR, it.organisasjonsnummer)
                    assertEquals(FOM, it.periodeFom)
                    assertEquals(TOM, it.periodeTom)
                    assertEquals(FOM, it.skjæringstidspunkt)
                    assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, it.inntektskilde)
                    assertEquals(relevanteArbeidsforhold, it.orgnummereMedRelevanteArbeidsforhold)
                    assertEquals(true, it.kanAvvises)
                    assertEquals(true, it.førstegangsbehandling)
                    assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, it.periodetype)
                    assertEquals(Utbetalingtype.UTBETALING, it.utbetalingtype)
                    assertEquals(
                        Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                            seksG = 6 * 118620.0,
                            arbeidsgivere = listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                    ORGNR,
                                    omregnetÅrsinntekt = 500000.0,
                                    inntektskilde = Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Saksbehandler,
                                    skjønnsfastsatt = 600000.0
                                )
                            )
                        ),
                        it.sykepengegrunnlagsfakta
                    )
                },
                kontekstbasertPubliserer = any()
            )
        }
    }

    @Test
    fun `leser Godkjenningbehov fastsatt i Infortrygd`() {
        val relevanteArbeidsforhold = listOf(ORGNR)
        val vilkårsgrunnlagId = UUID.randomUUID()
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = AKTØR,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                organisasjonsnummer = ORGNR,
                periodeFom = FOM,
                periodeTom = TOM,
                skjæringstidspunkt = FOM,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
                førstegangsbehandling = true,
                utbetalingtype = Utbetalingtype.UTBETALING,
                inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
                orgnummereMedRelevanteArbeidsforhold = relevanteArbeidsforhold,
                kanAvvises = true,
                id = HENDELSE,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                sykepengegrunnlagsfakta = Testmeldingfabrikk.godkjenningsbehovFastsattIInfotrygd(),
            )
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding = withArg<Godkjenningsbehov> {
                    assertEquals(HENDELSE, it.id)
                    assertEquals(FNR, it.fødselsnummer())
                    assertEquals(VEDTAKSPERIODE, it.vedtaksperiodeId())
                    assertEquals(ORGNR, it.organisasjonsnummer)
                    assertEquals(FOM, it.periodeFom)
                    assertEquals(TOM, it.periodeTom)
                    assertEquals(FOM, it.skjæringstidspunkt)
                    assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, it.inntektskilde)
                    assertEquals(relevanteArbeidsforhold, it.orgnummereMedRelevanteArbeidsforhold)
                    assertEquals(true, it.kanAvvises)
                    assertEquals(true, it.førstegangsbehandling)
                    assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, it.periodetype)
                    assertEquals(Utbetalingtype.UTBETALING, it.utbetalingtype)
                    assertEquals(Sykepengegrunnlagsfakta.Infotrygd, it.sykepengegrunnlagsfakta)
                },
                kontekstbasertPubliserer = any()
            )
        }
    }

    @Test
    fun `leser Godkjenningbehov med yrkesaktivitetstype selvstendig`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = AKTØR,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                organisasjonsnummer = "",
                yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
                periodeFom = FOM,
                periodeTom = TOM,
                skjæringstidspunkt = FOM,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
                førstegangsbehandling = true,
                utbetalingtype = Utbetalingtype.UTBETALING,
                inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
                orgnummereMedRelevanteArbeidsforhold = emptyList(),
                kanAvvises = true,
                id = HENDELSE,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                sykepengegrunnlagsfakta = Testmeldingfabrikk.godkjenningsbehovFastsattEtterHovedregel(
                    arbeidsgivere = listOf(
                        mapOf(
                            "arbeidsgiver" to ORGNR,
                            "omregnetÅrsinntekt" to 500000.00,
                            "skjønnsfastsatt" to 600000.00,
                            "inntektskilde" to "Saksbehandler"
                        )
                    )),
            )
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding = withArg<Godkjenningsbehov> {
                    assertEquals(HENDELSE, it.id)
                    assertEquals(FNR, it.fødselsnummer())
                    assertEquals(VEDTAKSPERIODE, it.vedtaksperiodeId())
                    assertEquals("", it.organisasjonsnummer)
                    assertEquals(Yrkesaktivitetstype.SELVSTENDIG, it.yrkesaktivitetstype)
                    assertEquals(FOM, it.periodeFom)
                    assertEquals(TOM, it.periodeTom)
                    assertEquals(FOM, it.skjæringstidspunkt)
                    assertEquals(Inntektskilde.EN_ARBEIDSGIVER, it.inntektskilde)
                    assertEquals(true, it.kanAvvises)
                    assertEquals(true, it.førstegangsbehandling)
                    assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, it.periodetype)
                    assertEquals(Utbetalingtype.UTBETALING, it.utbetalingtype)
                },
                kontekstbasertPubliserer = any()
            )
        }
    }
}
