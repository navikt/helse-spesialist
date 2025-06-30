package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringId
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
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
        opprettVedtaksperiode()
    }

    @Test
    fun `Kan lagre overstyringer`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

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
                skjønnsfastsattOverstyring,
            ),
            totrinnsvurderingId,
        )
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)
        assertEquals(5, overstyringer.size)
    }

    @Test
    fun `Kan ferdigstille overstyringer`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

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
            ),
            totrinnsvurderingId
        )
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)
        overstyringer.forEach { overstyring ->
            overstyring.ferdigstill()
        }
        overstyringRepository.lagre(overstyringer, totrinnsvurderingId)
        val ferdigstilteOverstyringer = finnFerdigstilteOverstyringer(totrinnsvurderingId)

        assertEquals(5, ferdigstilteOverstyringer.size)
    }

    @Test
    fun `TidslinjeOverstyring lagres riktig`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

        val tidslinjeOverstyring = nyTidslinjeOverstyring()
        overstyringRepository.lagre(listOf(tidslinjeOverstyring), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
        val hentetTidslinjeOverstyring = overstyringer.first()
        assertIs<OverstyrtTidslinje>(hentetTidslinjeOverstyring)
        assertEquals(AKTØR, hentetTidslinjeOverstyring.aktørId)
        assertEquals(false, hentetTidslinjeOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetTidslinjeOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetTidslinjeOverstyring.saksbehandlerOid.value)
        assertEquals(ORGNUMMER, hentetTidslinjeOverstyring.organisasjonsnummer)
        assertEquals("begrunnelse", hentetTidslinjeOverstyring.begrunnelse)
        assertEquals(FNR, hentetTidslinjeOverstyring.fødselsnummer)
        assertEqualsUnordered(
            expected = listOf(nyOverstyrtTidslinjedag()),
            actual = hentetTidslinjeOverstyring.dager,
            uniqueness = OverstyrtTidslinjedag::dato
        )
        assertTrue(hentetTidslinjeOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetTidslinjeOverstyring.harFåttTildeltId())
    }

    @Test
    fun `InntektOgRefusjonOverstyring lagres riktig`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

        val inntektOgRefusjonOverstyring = nyInntektOgRefusjonOverstyring()
        overstyringRepository.lagre(listOf(inntektOgRefusjonOverstyring), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
        val hentetInntektOgRefusjonOverstyring = overstyringer.first()
        assertIs<OverstyrtInntektOgRefusjon>(hentetInntektOgRefusjonOverstyring)
        assertEquals(AKTØR, hentetInntektOgRefusjonOverstyring.aktørId)
        assertEquals(false, hentetInntektOgRefusjonOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetInntektOgRefusjonOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetInntektOgRefusjonOverstyring.saksbehandlerOid.value)
        assertEquals(FNR, hentetInntektOgRefusjonOverstyring.fødselsnummer)
        assertEquals(1 jan 2018, hentetInntektOgRefusjonOverstyring.skjæringstidspunkt)
        assertEqualsUnordered(
            expected = listOf(nyOverstyrtArbeidsgiver()),
            actual = hentetInntektOgRefusjonOverstyring.arbeidsgivere,
            uniqueness = OverstyrtArbeidsgiver::organisasjonsnummer
        )

        assertTrue(hentetInntektOgRefusjonOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetInntektOgRefusjonOverstyring.harFåttTildeltId())
    }

    @Test
    fun `ArbeidsforholdOverstyring lagres riktig`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

        val arbeidsforholdOverstyring = nyArbeidsforholdOverstyring()
        overstyringRepository.lagre(listOf(arbeidsforholdOverstyring), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
        val hentetArbeidsforholdOverstyring = overstyringer.first()
        assertIs<OverstyrtArbeidsforhold>(hentetArbeidsforholdOverstyring)
        assertEquals(AKTØR, hentetArbeidsforholdOverstyring.aktørId)
        assertEquals(false, hentetArbeidsforholdOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetArbeidsforholdOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetArbeidsforholdOverstyring.saksbehandlerOid.value)
        assertEquals(FNR, hentetArbeidsforholdOverstyring.fødselsnummer)
        assertEqualsUnordered(
            expected = listOf(nyArbeidsforhold()),
            actual = hentetArbeidsforholdOverstyring.overstyrteArbeidsforhold,
            uniqueness = Arbeidsforhold::organisasjonsnummer
        )
        assertTrue(hentetArbeidsforholdOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetArbeidsforholdOverstyring.harFåttTildeltId())
    }

    @Test
    fun `MinimumSykdomsgradOverstyring lagres riktig`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

        val minimumSykdomsgradOverstyring = nyMinimumSykdomsgradOverstyring()
        overstyringRepository.lagre(listOf(minimumSykdomsgradOverstyring), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
        val hentetMinimumSykdomsgradOverstyring = overstyringer.first()
        assertIs<MinimumSykdomsgrad>(hentetMinimumSykdomsgradOverstyring)
        assertEquals(AKTØR, hentetMinimumSykdomsgradOverstyring.aktørId)
        assertEquals(false, hentetMinimumSykdomsgradOverstyring.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetMinimumSykdomsgradOverstyring.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetMinimumSykdomsgradOverstyring.saksbehandlerOid.value)
        assertEquals(FNR, hentetMinimumSykdomsgradOverstyring.fødselsnummer)
        assertEquals("begrunnelse", hentetMinimumSykdomsgradOverstyring.begrunnelse)
        assertEqualsUnordered(
            expected = listOf(nyMinimumSykdomsgradArbeidsgiver()),
            actual = hentetMinimumSykdomsgradOverstyring.arbeidsgivere,
            uniqueness = MinimumSykdomsgradArbeidsgiver::organisasjonsnummer
        )
        assertEqualsUnordered(
            expected = listOf(nyMinimumSykdomsgradPeriode(fom = 1 jan 2018, tom = 15 jan 2018)),
            actual = hentetMinimumSykdomsgradOverstyring.perioderVurdertOk,
            uniqueness = MinimumSykdomsgradPeriode::fom
        )

        assertEqualsUnordered(
            expected = listOf(nyMinimumSykdomsgradPeriode(fom = 16 jan 2018, tom = 31 jan 2018)),
            actual = hentetMinimumSykdomsgradOverstyring.perioderVurdertIkkeOk,
            uniqueness = MinimumSykdomsgradPeriode::fom
        )
        assertTrue(hentetMinimumSykdomsgradOverstyring.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetMinimumSykdomsgradOverstyring.harFåttTildeltId())
    }

    @Test
    fun `SkjønnsfastsattSykepengegrunnlag lagres riktig`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()

        val skjønnsfastsattSykepengegrunnlag = nySkjønnsfastsattOverstyring()
        overstyringRepository.lagre(listOf(skjønnsfastsattSykepengegrunnlag), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
        val hentetSkjønnsfastsattSykepengegrunnlag = overstyringer.first()
        assertIs<SkjønnsfastsattSykepengegrunnlag>(hentetSkjønnsfastsattSykepengegrunnlag)

        assertEquals(AKTØR, hentetSkjønnsfastsattSykepengegrunnlag.aktørId)
        assertEquals(false, hentetSkjønnsfastsattSykepengegrunnlag.ferdigstilt)
        assertEquals(VEDTAKSPERIODE, hentetSkjønnsfastsattSykepengegrunnlag.vedtaksperiodeId)
        assertEquals(SAKSBEHANDLER_OID, hentetSkjønnsfastsattSykepengegrunnlag.saksbehandlerOid.value)
        assertEquals(FNR, hentetSkjønnsfastsattSykepengegrunnlag.fødselsnummer)
        assertEquals(1 jan 2018, hentetSkjønnsfastsattSykepengegrunnlag.skjæringstidspunkt)
        assertEqualsUnordered(
            expected = listOf(nySkjønnsfastsattArbeidsgiver()),
            actual = hentetSkjønnsfastsattSykepengegrunnlag.arbeidsgivere,
            uniqueness = SkjønnsfastsattArbeidsgiver::organisasjonsnummer
        )

        assertTrue(hentetSkjønnsfastsattSykepengegrunnlag.opprettet.isBefore(LocalDateTime.now()))
        assertTrue(hentetSkjønnsfastsattSykepengegrunnlag.harFåttTildeltId())
    }

    @Test
    fun `Får tilbake én overstyring når det er gjort flere endringer i en overstyring av arbeidsforhold`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()
        val orgnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(orgnummer2)

        val arbeidsforholdOverstyring =
            nyArbeidsforholdOverstyring(listOf(nyArbeidsforhold(), nyArbeidsforhold(orgnummer2)))
        overstyringRepository.lagre(listOf(arbeidsforholdOverstyring), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
    }

    @Test
    fun `Får tilbake én overstyring når det er gjort flere endringer i en overstyring av inntekt og refusjon`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering()
        val orgnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(orgnummer2)

        val inntektOgRefusjonOverstyring =
            nyInntektOgRefusjonOverstyring(listOf(nyOverstyrtArbeidsgiver(), nyOverstyrtArbeidsgiver(orgnummer2)))
        overstyringRepository.lagre(listOf(inntektOgRefusjonOverstyring), totrinnsvurderingId)
        val overstyringer = overstyringRepository.finnAktive(totrinnsvurderingId)

        assertEquals(1, overstyringer.size)
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

    private fun nyArbeidsforholdOverstyring(overstyrteArbeidsforhold: List<Arbeidsforhold> = listOf(nyArbeidsforhold())): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            skjæringstidspunkt = 1 jan 2018,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold
        )

    private fun nyArbeidsforhold(orgnummer: String = ORGNUMMER): Arbeidsforhold =
        Arbeidsforhold(
            organisasjonsnummer = orgnummer,
            deaktivert = true,
            begrunnelse = "begrunnelse",
            forklaring = "forklaring",
            lovhjemmel = null,
        )

    private fun nyInntektOgRefusjonOverstyring(
        overstyrtArbeidsgiver: List<OverstyrtArbeidsgiver> = listOf(
            nyOverstyrtArbeidsgiver()
        )
    ): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon.ny(
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
            fødselsnummer = FNR,
            aktørId = AKTØR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            skjæringstidspunkt = 1 jan 2018,
            arbeidsgivere = overstyrtArbeidsgiver,
        )

    private fun nyOverstyrtArbeidsgiver(orgnummer: String = ORGNUMMER): OverstyrtArbeidsgiver =
        OverstyrtArbeidsgiver(
            organisasjonsnummer = orgnummer,
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

    private fun finnFerdigstilteOverstyringer(totrinnsvurderingId: TotrinnsvurderingId): List<OverstyringId> =
        dbQuery.list(
            """
                select id from overstyring 
                where totrinnsvurdering_ref = :totrinnsvurderingId and ferdigstilt = true
            """.trimMargin(),
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ) { OverstyringId(it.long("id")) }

    private fun <T, R : Comparable<R>> assertEqualsUnordered(expected: List<T>, actual: List<T>, uniqueness: (T) -> R) {
        assertEquals(expected.sortedBy(uniqueness), actual.sortedBy(uniqueness))
    }
}
