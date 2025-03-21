package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringId
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PgOverstyringRepositoryTest : AbstractDBIntegrationTest() {

    @BeforeEach
    fun setup() {
        opprettPerson()
        opprettSaksbehandler()
        opprettArbeidsgiver()
    }

    @Test
    fun `Kan lagre overstyringer`() {
        val tidslinjeOverstyring = nyTidslinjeOverstyring()
        val inntektOgRefusjonOverstyring = nyInntektOgRefusjonOverstyring()
        val arbeidsforholdOverstyring = nyArbeidsforholdOverstyring()
        val minimumSykdomsgradOverstyring = nyMinimumSykdomsgradOverstyring()
        val skjønnsfastsattOverstyring = nySkjønnsfastsattOverstyring()
        val tilkommenInntektOverstyring = nyTilkommenInntektOverstyring()

        overstyringRepository.lagre(
            listOf(
                tidslinjeOverstyring,
                inntektOgRefusjonOverstyring,
                arbeidsforholdOverstyring,
                minimumSykdomsgradOverstyring,
                skjønnsfastsattOverstyring,
                tilkommenInntektOverstyring
            )
        )
        val overstyringer = overstyringRepository.finnAktive(FNR)
        assertEquals(6, overstyringer.size)
    }

    @Test
    fun `Kan ferdigstille overstyringer`() {
        val tidslinjeOverstyring = nyTidslinjeOverstyring()
        val inntektOgRefusjonOverstyring = nyInntektOgRefusjonOverstyring()
        val arbeidsforholdOverstyring = nyArbeidsforholdOverstyring()
        val minimumSykdomsgradOverstyring = nyMinimumSykdomsgradOverstyring()
        val skjønnsfastsattOverstyring = nySkjønnsfastsattOverstyring()

        overstyringRepository.lagre(
            listOf(
                tidslinjeOverstyring,
                inntektOgRefusjonOverstyring,
                arbeidsforholdOverstyring,
                minimumSykdomsgradOverstyring,
                skjønnsfastsattOverstyring
            )
        )
        val overstyringer = overstyringRepository.finnAktive(FNR)
        overstyringer.forEach { overstyring ->
            overstyring.ferdigstill()
        }
        overstyringRepository.lagre(overstyringer)
        val ferdigstilteOverstyringer = finnFerdigstilteOverstyringer(FNR)

        assertEquals(5, ferdigstilteOverstyringer.size)
    }

    @Test
    fun `TidslinjeOverstyring lagres riktig`() {
        val tidslinjeOverstyring = nyTidslinjeOverstyring()
        overstyringRepository.lagre(listOf(tidslinjeOverstyring))
        val overstyringer = overstyringRepository.finnAktive(FNR)
        val hentetTidslinjeOverstyring = overstyringer.first()

        assertEquals(1, overstyringer.size)
        assertIs<OverstyrtTidslinje>(hentetTidslinjeOverstyring)
        assertEquals(AKTØR, hentetTidslinjeOverstyring.aktørId)
        assertEquals(false, hentetTidslinjeOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetTidslinjeOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetTidslinjeOverstyring.saksbehandlerOid.value)
        assertEquals(ORGNUMMER, hentetTidslinjeOverstyring.organisasjonsnummer)
        assertEquals("begrunnelse", hentetTidslinjeOverstyring.begrunnelse)
        assertEquals(FNR, hentetTidslinjeOverstyring.fødselsnummer)
        assertEquals(nyOverstyrtTidslinjedag(), hentetTidslinjeOverstyring.dager.first())
        assertTrue(hentetTidslinjeOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetTidslinjeOverstyring.harFåttTildeltId())
    }

    @Test
    fun `InntektOgRefusjonOverstyring lagres riktig`() {
        val inntektOgRefusjonOverstyring = nyInntektOgRefusjonOverstyring()
        overstyringRepository.lagre(listOf(inntektOgRefusjonOverstyring))
        val overstyringer = overstyringRepository.finnAktive(FNR)
        val hentetInntektOgRefusjonOverstyring = overstyringer.first()

        assertEquals(1, overstyringer.size)
        assertIs<OverstyrtInntektOgRefusjon>(hentetInntektOgRefusjonOverstyring)
        assertEquals(AKTØR, hentetInntektOgRefusjonOverstyring.aktørId)
        assertEquals(false, hentetInntektOgRefusjonOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetInntektOgRefusjonOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetInntektOgRefusjonOverstyring.saksbehandlerOid.value)
        assertEquals(FNR, hentetInntektOgRefusjonOverstyring.fødselsnummer)
        assertEquals(1 jan 2018, hentetInntektOgRefusjonOverstyring.skjæringstidspunkt)
        assertEquals(nyOverstyrtArbeidsgiver(), hentetInntektOgRefusjonOverstyring.arbeidsgivere.first())
        assertTrue(hentetInntektOgRefusjonOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetInntektOgRefusjonOverstyring.harFåttTildeltId())
    }

    @Test
    fun `ArbeidsforholdOverstyring lagres riktig`() {
        val arbeidsforholdOverstyring = nyArbeidsforholdOverstyring()
        overstyringRepository.lagre(listOf(arbeidsforholdOverstyring))
        val overstyringer = overstyringRepository.finnAktive(FNR)
        val hentetArbeidsforholdOverstyring = overstyringer.first()

        assertEquals(1, overstyringer.size)
        assertIs<OverstyrtArbeidsforhold>(hentetArbeidsforholdOverstyring)
        assertEquals(AKTØR, hentetArbeidsforholdOverstyring.aktørId)
        assertEquals(false, hentetArbeidsforholdOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetArbeidsforholdOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetArbeidsforholdOverstyring.saksbehandlerOid.value)
        assertEquals(FNR, hentetArbeidsforholdOverstyring.fødselsnummer)
        assertEquals(nyArbeidsforhold(), hentetArbeidsforholdOverstyring.overstyrteArbeidsforhold.first())
        assertTrue(hentetArbeidsforholdOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetArbeidsforholdOverstyring.harFåttTildeltId())
    }

    @Test
    fun `MinimumSykdomsgradOverstyring lagres riktig`() {
        val minimumSykdomsgradOverstyring = nyMinimumSykdomsgradOverstyring()
        overstyringRepository.lagre(listOf(minimumSykdomsgradOverstyring))
        val overstyringer = overstyringRepository.finnAktive(FNR)
        val hentetMinimumSykdomsgradOverstyring = overstyringer.first()

        assertEquals(1, overstyringer.size)
        assertIs<MinimumSykdomsgrad>(hentetMinimumSykdomsgradOverstyring)
        assertEquals(AKTØR, hentetMinimumSykdomsgradOverstyring.aktørId)
        assertEquals(false, hentetMinimumSykdomsgradOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetMinimumSykdomsgradOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetMinimumSykdomsgradOverstyring.saksbehandlerOid.value)
        assertEquals(FNR, hentetMinimumSykdomsgradOverstyring.fødselsnummer)
        assertEquals("begrunnelse", hentetMinimumSykdomsgradOverstyring.begrunnelse)
        assertEquals(nyMinimumSykdomsgradArbeidsgiver(), hentetMinimumSykdomsgradOverstyring.arbeidsgivere.first())
        assertEquals(
            nyMinimumSykdomsgradPeriode(fom = 1 jan 2018, tom = 15 jan 2018),
            hentetMinimumSykdomsgradOverstyring.perioderVurdertOk.first()
        )
        assertEquals(
            nyMinimumSykdomsgradPeriode(fom = 16 jan 2018, tom = 31 jan 2018),
            hentetMinimumSykdomsgradOverstyring.perioderVurdertIkkeOk.first()
        )
        assertTrue(hentetMinimumSykdomsgradOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetMinimumSykdomsgradOverstyring.harFåttTildeltId())
    }

    @Test
    fun `SkjønnsfastsattSykepengegrunnlag lagres riktig`() {
        val skjønnsfastsattSykepengegrunnlag = nySkjønnsfastsattOverstyring()
        overstyringRepository.lagre(listOf(skjønnsfastsattSykepengegrunnlag))
        val overstyringer = overstyringRepository.finnAktive(FNR)
        val hentetSkjønnsfastsattSykepengegrunnlag = overstyringer.first()

        assertEquals(1, overstyringer.size)
        assertIs<SkjønnsfastsattSykepengegrunnlag>(hentetSkjønnsfastsattSykepengegrunnlag)

        assertEquals(AKTØR, hentetSkjønnsfastsattSykepengegrunnlag.aktørId)
        assertEquals(false, hentetSkjønnsfastsattSykepengegrunnlag.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetSkjønnsfastsattSykepengegrunnlag.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetSkjønnsfastsattSykepengegrunnlag.saksbehandlerOid.value)
        assertEquals(FNR, hentetSkjønnsfastsattSykepengegrunnlag.fødselsnummer)
        assertEquals(1 jan 2018, hentetSkjønnsfastsattSykepengegrunnlag.skjæringstidspunkt)
        assertEquals(nySkjønnsfastsattArbeidsgiver(), hentetSkjønnsfastsattSykepengegrunnlag.arbeidsgivere.first())
        assertTrue(hentetSkjønnsfastsattSykepengegrunnlag.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetSkjønnsfastsattSykepengegrunnlag.harFåttTildeltId())
    }

    @Test
    fun `OverstyrTilkommenInntekt lagres riktig`() {
        val overstyrTilkommenInntekt = nyTilkommenInntektOverstyring()
        overstyringRepository.lagre(listOf(overstyrTilkommenInntekt))
        val overstyringer = overstyringRepository.finnAktive(FNR)
        val hentetOverstyrTilkommenInntekt = overstyringer.first()

        assertEquals(1, overstyringer.size)
        assertIs<OverstyrTilkommenInntekt>(hentetOverstyrTilkommenInntekt)

        assertEquals(overstyrTilkommenInntekt.aktørId, hentetOverstyrTilkommenInntekt.aktørId)
        assertEquals(overstyrTilkommenInntekt.ferdigstilt, hentetOverstyrTilkommenInntekt.ferdigstilt)
        assertEquals(overstyrTilkommenInntekt.vedtaksperiodeId, hentetOverstyrTilkommenInntekt.vedtaksperiodeId)
        assertEquals(overstyrTilkommenInntekt.saksbehandlerOid.value, hentetOverstyrTilkommenInntekt.saksbehandlerOid.value)
        assertEquals(overstyrTilkommenInntekt.eksternHendelseId, hentetOverstyrTilkommenInntekt.eksternHendelseId)
        assertEquals(overstyrTilkommenInntekt.opprettet.withNano(0), hentetOverstyrTilkommenInntekt.opprettet.withNano(0))
        assertEquals(overstyrTilkommenInntekt.fødselsnummer, hentetOverstyrTilkommenInntekt.fødselsnummer)
        assertEquals(overstyrTilkommenInntekt.fjernedeInntekter, hentetOverstyrTilkommenInntekt.fjernedeInntekter)
        assertEquals(overstyrTilkommenInntekt.nyEllerEndredeInntekter, hentetOverstyrTilkommenInntekt.nyEllerEndredeInntekter)
    }

    private fun nyTidslinjeOverstyring(): OverstyrtTidslinje =
        OverstyrtTidslinje.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            organisasjonsnummer = ORGNUMMER,
            begrunnelse = "begrunnelse",
            dager = listOf(nyOverstyrtTidslinjedag()),
        )

    private fun nyArbeidsforholdOverstyring(): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            skjæringstidspunkt = 1 jan 2018,
            overstyrteArbeidsforhold = listOf(nyArbeidsforhold())
        )

    private fun nyArbeidsforhold(): Arbeidsforhold =
        Arbeidsforhold(
            organisasjonsnummer = ORGNUMMER,
            deaktivert = true,
            begrunnelse = "begrunnelse",
            forklaring = "forklaring",
            lovhjemmel = null,
        )

    private fun nyInntektOgRefusjonOverstyring(): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            skjæringstidspunkt = 1 jan 2018,
            arbeidsgivere = listOf(nyOverstyrtArbeidsgiver()),
        )

    private fun nyOverstyrtArbeidsgiver(): OverstyrtArbeidsgiver =
        OverstyrtArbeidsgiver(
            organisasjonsnummer = ORGNUMMER,
            månedligInntekt = 1001.0,
            fraMånedligInntekt = 1000.0,
            fraRefusjonsopplysninger = null,
            refusjonsopplysninger = listOf(
                Refusjonselement(
                    fom = 1 jan 2018,
                    tom = 31 jan 2018,
                    beløp = 1000.0,
                ),
            ),
            begrunnelse = "begrunnelse",
            forklaring = "forklaring",
            lovhjemmel = null,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
        )

    private fun nyMinimumSykdomsgradOverstyring(): MinimumSykdomsgrad =
        MinimumSykdomsgrad.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            begrunnelse = "begrunnelse",
            perioderVurdertOk = listOf(nyMinimumSykdomsgradPeriode(fom = 1 jan 2018, tom = 15 jan 2018)),
            perioderVurdertIkkeOk = listOf(nyMinimumSykdomsgradPeriode(fom = 16 jan 2018, tom = 31 jan 2018)),
            arbeidsgivere = listOf(nyMinimumSykdomsgradArbeidsgiver()),
        )

    private fun nyMinimumSykdomsgradPeriode(
        fom: LocalDate = 1 jan 2018,
        tom: LocalDate = 31 jan 2018
    ): MinimumSykdomsgradPeriode =
        MinimumSykdomsgradPeriode(
            fom = fom,
            tom = tom
        )

    private fun nyMinimumSykdomsgradArbeidsgiver(): MinimumSykdomsgradArbeidsgiver =
        MinimumSykdomsgradArbeidsgiver(
            organisasjonsnummer = ORGNUMMER,
            berørtVedtaksperiodeId = VEDTAKSPERIODE
        )

    private fun nySkjønnsfastsattOverstyring(): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            skjæringstidspunkt = 1 jan 2018,
            arbeidsgivere = listOf(nySkjønnsfastsattArbeidsgiver()),
        )
    private fun nyTilkommenInntektOverstyring(): OverstyrTilkommenInntekt =
        OverstyrTilkommenInntekt.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            nyEllerEndredeInntekter = listOf(nyEllerEndretInntekt()),
            fjernedeInntekter = listOf(fjernetInntekt()),
        )

    private fun nySkjønnsfastsattArbeidsgiver(): SkjønnsfastsattArbeidsgiver =
        SkjønnsfastsattArbeidsgiver(
            organisasjonsnummer = ORGNUMMER,
            årlig = 1000.0,
            fraÅrlig = 10001.0,
            årsak = "årsak",
            type = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
            begrunnelseMal = "mal",
            begrunnelseKonklusjon = "konklusjon",
            begrunnelseFritekst = "fritekst",
            initierendeVedtaksperiodeId = VEDTAKSPERIODE.toString(),
            lovhjemmel = null,
        )

    private fun nyEllerEndretInntekt(): OverstyrTilkommenInntekt.NyEllerEndretInntekt =
        OverstyrTilkommenInntekt.NyEllerEndretInntekt(
            organisasjonsnummer = ORGNUMMER,
            perioder = listOf(
                OverstyrTilkommenInntekt.NyEllerEndretInntekt.PeriodeMedBeløp(1 jan 2018, 31 jan 2018, 15000.0)
            )
        )

    private fun fjernetInntekt(): OverstyrTilkommenInntekt.FjernetInntekt =
        OverstyrTilkommenInntekt.FjernetInntekt(
            organisasjonsnummer = ORGNUMMER,
            perioder = listOf(
                OverstyrTilkommenInntekt.FjernetInntekt.PeriodeUtenBeløp(1 jan 2018, 31 jan 2018)
            )
        )

    private fun finnFerdigstilteOverstyringer(fødselsnummer: String): List<OverstyringId> =
        dbQuery.list(
            """
                select o.id from overstyring o 
                    inner join person p on o.person_ref = p.id
                where p.fødselsnummer = :fodselsnummer and ferdigstilt = true
            """.trimMargin(),
            "fodselsnummer" to fødselsnummer,
        ) { OverstyringId(it.long("id")) }

}
