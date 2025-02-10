package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.db.overstyring.ArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.LovhjemmelForDatabase
import no.nav.helse.db.overstyring.MinimumSykdomsgradForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.overstyring.RefusjonselementForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.modell.InntektskildetypeDto.ORDINÆR
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgOverstyringDaoTest : DatabaseIntegrationTest() {
    private val inntektskilderRepository = DBSessionContext(session, TilgangskontrollForTestHarIkkeTilgang).inntektskilderRepository
    private val PERSON_FORNAVN = "Per"
    private val PERSON_ETTERNAVN = "Son"
    private val PERSON_FØDSELSDATO = LocalDate.of(1998, 4, 20)
    private val PERSON_KJØNN = Kjønn.Ukjent
    private val ARBEIDSGIVER_NAVN = "Skrue McDuck"
    private val EKSTERN_HENDELSE_ID = UUID.randomUUID()
    private val DEAKTIVERT = true
    private val SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 1, 1)
    private val OID = UUID.randomUUID()
    private val EPOST = "saks.behandler@nav.no"
    private val BEGRUNNELSE = "BegrunnelseMal\n\nBegrunnelseFritekst"
    private val BEGRUNNELSEMAL = "BegrunnelseMal"
    private val BEGRUNNELSEFRITEKST = "BegrunnelseFritekst"
    private val BEGRUNNELSEKONKLUSJON = "BegrunnelseKonklusjon"
    private val FORKLARING = "Forklaring"
    private val ÅRSAK = "Årsak"
    private val OVERSTYRTE_DAGER =
        listOf(
            OverstyrtTidslinjedagForDatabase(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag.toString(),
                grad = 100,
                fraType = Dagtype.Feriedag.toString(),
                fraGrad = null,
                lovhjemmel = null,
            ),
        )
    private val OPPRETTET = LocalDate.of(2022, 6, 9).atStartOfDay()
    private val INNTEKT = 31000.0

    private fun opprettPerson() {
        saksbehandlerDao.opprettEllerOppdater(OID, SAKSBEHANDLER_NAVN, EPOST, SAKSBEHANDLER_IDENT)
        inntektskilderRepository.lagreInntektskilder(
            listOf(
                KomplettInntektskildeDto(ORGNUMMER, ORDINÆR, ARBEIDSGIVER_NAVN, BRANSJER, LocalDate.now())
            )
        )
        personDao.lagreMinimalPerson(MinimalPersonDto(FNR, AKTØR))
        opprettPersoninfo(
            FNR,
            PERSON_FORNAVN,
            null,
            PERSON_ETTERNAVN,
            PERSON_FØDSELSDATO,
            PERSON_KJØNN,
            ADRESSEBESKYTTELSE,
        )
        personDao.upsertInfotrygdutbetalinger(FNR, objectMapper.createObjectNode())
    }

    @Test
    fun `Kan koble overstyringhendelse og vedtaksperiode`() {
        opprettPerson()
        persisterOverstyringTidslinje()
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)

        assertTrue(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))
        assertFalse(overstyringDao.harVedtaksperiodePågåendeOverstyring(UUID.randomUUID()))
    }

    @Test
    fun `Finnes ekstern_hendelse_id i overstyringtabell`() {
        opprettPerson()
        persisterOverstyringTidslinje()

        assertTrue(overstyringDao.finnesEksternHendelseId(EKSTERN_HENDELSE_ID))
        assertFalse(overstyringDao.finnesEksternHendelseId(UUID.randomUUID()))
    }

    @Test
    fun `Vedtaksperiode har ikke pågående overstyring etter ferdigstilling`() {
        opprettPerson()
        persisterOverstyringTidslinje()
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)

        val hentetOverstyring = overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetOverstyring is OverstyringTidslinjeDto)
        assertFalse(hentetOverstyring.ferdigstilt)

        assertTrue(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))
        overstyringDao.ferdigstillOverstyringerForVedtaksperiode(VEDTAKSPERIODE)
        assertFalse(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))

        val hentetOverstyringEtterFerdigstilling =
            overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetOverstyringEtterFerdigstilling is OverstyringTidslinjeDto)
        assertTrue(hentetOverstyringEtterFerdigstilling.ferdigstilt)
    }

    @Test
    fun `Finner opprettede overstyringer for fødselsnummer`() {
        opprettPerson()

        persisterOverstyringTidslinje()
        persisterOverstyringMinimumSykdomsgrad()
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiverForDatabase(),
            )
        )
        persisterOverstyringArbeidsforhold(
            EKSTERN_HENDELSE_ID, listOf(
                arbeidsforholdForDatabase(),
            )
        )
        persisterSkjønnsfastsettingSykepengegrunnlag()


        val overstyringer = overstyringDao.finnOverstyringer(FNR)

        assertEquals(5, overstyringer.size)
    }

    @Test
    fun `Får tilbake én overstyring når det er gjort flere endringer i en overstyring av arbeidsforhold`() {
        opprettPerson()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(organisasjonsnummer2)
        persisterOverstyringArbeidsforhold(
            EKSTERN_HENDELSE_ID, listOf(
                arbeidsforholdForDatabase(),
                arbeidsforholdForDatabase(organisasjonsnummer2),
            )
        )
        val overstyringer = overstyringDao.finnOverstyringer(FNR)

        assertEquals(1, overstyringer.size)
    }

    @Test
    fun `Får tilbake én overstyring når det er gjort flere endringer i en overstyring av inntekt og refusjon`() {
        opprettPerson()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(organisasjonsnummer2)
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiverForDatabase(),
                overstyrtArbeidsgiverForDatabase(organisasjonsnummer2),
            )
        )
        val overstyringer = overstyringDao.finnOverstyringer(FNR)

        assertEquals(1, overstyringer.size)
    }

    @Test
    fun `Finner opprettede tidslinjeoverstyringer`() {
        opprettPerson()
        persisterOverstyringTidslinje()
        val hentetOverstyring = overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetOverstyring is OverstyringTidslinjeDto)

        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FNR, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(OVERSTYRTE_DAGER, hentetOverstyring.overstyrteDager.map { it.dtoToDatabase() })
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetOverstyring.saksbehandlerIdent)
        assertEquals(OPPRETTET, hentetOverstyring.timestamp)
        assertFalse(hentetOverstyring.ferdigstilt)
    }

    @Test
    fun `Finner opprettede arbeidsforholdoverstyringer`() {
        opprettPerson()
        persisterOverstyringArbeidsforhold(
            EKSTERN_HENDELSE_ID, listOf(
                arbeidsforholdForDatabase(),
            )
        )
        val hentetOverstyring = overstyringApiDao.finnOverstyringer(FNR).single()
        check(hentetOverstyring is OverstyringArbeidsforholdDto)

        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FORKLARING, hentetOverstyring.forklaring)
        assertEquals(DEAKTIVERT, hentetOverstyring.deaktivert)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetOverstyring.skjæringstidspunkt)
        assertEquals(FNR, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetOverstyring.saksbehandlerIdent)
        assertEquals(OPPRETTET, hentetOverstyring.timestamp)
        assertFalse(hentetOverstyring.ferdigstilt)
    }

    @Test
    fun `Finner opprettede inntekt- og refusjonsoverstyringer`() {
        opprettPerson()
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiverForDatabase(),
            )
        )
        val hentetOverstyring = overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetOverstyring is OverstyringInntektDto)

        assertEquals(FNR, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FORKLARING, hentetOverstyring.forklaring)
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetOverstyring.saksbehandlerIdent)
        assertEquals(INNTEKT, hentetOverstyring.månedligInntekt)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetOverstyring.skjæringstidspunkt)
        assertEquals(OPPRETTET, hentetOverstyring.timestamp)
        assertFalse(hentetOverstyring.ferdigstilt)
        assertEquals(1, hentetOverstyring.refusjonsopplysninger?.size)
        val refusjonsopplysning = hentetOverstyring.refusjonsopplysninger?.first()
        assertEquals(1.januar, refusjonsopplysning?.fom)
        assertEquals(31.januar, refusjonsopplysning?.tom)
        assertEquals(1000.0, refusjonsopplysning?.beløp)
        assertEquals(1.januar, hentetOverstyring.fom)
        assertNull(hentetOverstyring.tom)
    }

    @Test
    fun `Finner opprettede skjønnsfastsatte sykepengegrunnlag`() {
        opprettPerson()
        persisterSkjønnsfastsettingSykepengegrunnlag()
        val hentetSkjønnsfastsetting =
            overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetSkjønnsfastsetting is SkjønnsfastsettingSykepengegrunnlagDto)

        assertEquals(FNR, hentetSkjønnsfastsetting.fødselsnummer)
        assertEquals(ORGNUMMER, hentetSkjønnsfastsetting.organisasjonsnummer)
        assertEquals(
            BEGRUNNELSEMAL + "\n\n" + BEGRUNNELSEFRITEKST + "\n\n" + BEGRUNNELSEKONKLUSJON,
            hentetSkjønnsfastsetting.begrunnelse,
        )
        assertEquals(BEGRUNNELSEMAL, hentetSkjønnsfastsetting.begrunnelseMal)
        assertEquals(BEGRUNNELSEFRITEKST, hentetSkjønnsfastsetting.begrunnelseFritekst)
        assertEquals(BEGRUNNELSEKONKLUSJON, hentetSkjønnsfastsetting.begrunnelseKonklusjon)
        assertEquals(ÅRSAK, hentetSkjønnsfastsetting.årsak)
        assertEquals(Skjonnsfastsettingstype.OMREGNET_ARSINNTEKT.name, hentetSkjønnsfastsetting.type.name)
        assertEquals(SAKSBEHANDLER_NAVN, hentetSkjønnsfastsetting.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetSkjønnsfastsetting.saksbehandlerIdent)
        assertEquals(INNTEKT, hentetSkjønnsfastsetting.årlig)
        assertEquals(INNTEKT + 1, hentetSkjønnsfastsetting.fraÅrlig)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetSkjønnsfastsetting.skjæringstidspunkt)
        assertEquals(OPPRETTET, hentetSkjønnsfastsetting.timestamp)
        assertFalse(hentetSkjønnsfastsetting.ferdigstilt)
    }

    @Test
    fun `Finner opprettede overstyringer av minimum sykdomsgrad`() {
        opprettPerson()
        persisterOverstyringMinimumSykdomsgrad()
        val hentetMinimumSykdomsgrad = overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetMinimumSykdomsgrad is OverstyringMinimumSykdomsgradDto)

        assertEquals(FNR, hentetMinimumSykdomsgrad.fødselsnummer)
        assertEquals(ORGNUMMER, hentetMinimumSykdomsgrad.organisasjonsnummer)
        assertEquals(1.januar, hentetMinimumSykdomsgrad.perioderVurdertOk.first().fom)
        assertEquals(31.januar, hentetMinimumSykdomsgrad.perioderVurdertOk.first().tom)
        assertTrue(hentetMinimumSykdomsgrad.perioderVurdertOk.isNotEmpty())
        assertTrue(hentetMinimumSykdomsgrad.perioderVurdertIkkeOk.isEmpty())
        assertEquals("en begrunnelse", hentetMinimumSykdomsgrad.begrunnelse)
        assertEquals(SAKSBEHANDLER_NAVN, hentetMinimumSykdomsgrad.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetMinimumSykdomsgrad.saksbehandlerIdent)
        assertEquals(OPPRETTET, hentetMinimumSykdomsgrad.timestamp)
        assertFalse(hentetMinimumSykdomsgrad.ferdigstilt)
    }

    @Test
    fun `Finner hendelsesid'er for ikke ferdigstilte overstyringer for vedtaksperiode`() {
        opprettPerson()
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiverForDatabase(),
            )
        )
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)
        val eksternHendelsesIdArbeidsforhold = UUID.randomUUID()
        persisterOverstyringArbeidsforhold(
            eksternHendelsesIdArbeidsforhold, listOf(
                arbeidsforholdForDatabase(),
            )
        )
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), eksternHendelsesIdArbeidsforhold)

        val aktiveOverstyringer = overstyringDao.finnAktiveOverstyringer(VEDTAKSPERIODE)

        assertEquals(EKSTERN_HENDELSE_ID, aktiveOverstyringer.first())
        assertEquals(eksternHendelsesIdArbeidsforhold, aktiveOverstyringer.last())
    }

    private fun persisterSkjønnsfastsettingSykepengegrunnlag() {
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            SkjønnsfastsattSykepengegrunnlagForDatabase(
                eksternHendelseId = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                skjæringstidspunkt = 1.januar,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                saksbehandlerOid = UUID.randomUUID(),
                arbeidsgivere =
                    listOf(
                        SkjønnsfastsattArbeidsgiverForDatabase(
                            organisasjonsnummer = ORGNUMMER,
                            årlig = INNTEKT,
                            fraÅrlig = INNTEKT + 1,
                            årsak = ÅRSAK,
                            type = SkjønnsfastsettingstypeForDatabase.OMREGNET_ÅRSINNTEKT,
                            begrunnelseMal = BEGRUNNELSEMAL,
                            begrunnelseKonklusjon = BEGRUNNELSEKONKLUSJON,
                            begrunnelseFritekst = BEGRUNNELSEFRITEKST,
                            initierendeVedtaksperiodeId = VEDTAKSPERIODE.toString(),
                            lovhjemmel = LovhjemmelForDatabase(paragraf = "paragraf"),
                        ),
                    ),
            ),
            OID,
        )
    }

    private fun persisterOverstyringMinimumSykdomsgrad() {
        overstyringDao.persisterMinimumSykdomsgrad(
            MinimumSykdomsgradForDatabase(
                eksternHendelseId = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                perioderVurdertOk = listOf(
                    MinimumSykdomsgradForDatabase.MinimumSykdomsgradPeriodeForDatabase(
                        fom = 1.januar,
                        tom = 31.januar
                    )
                ),
                perioderVurdertIkkeOk = emptyList(),
                begrunnelse = "en begrunnelse",
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                saksbehandlerOid = UUID.randomUUID(),
                arbeidsgivere =
                    listOf(
                        MinimumSykdomsgradForDatabase.MinimumSykdomsgradArbeidsgiverForDatabase(
                            organisasjonsnummer = ORGNUMMER,
                            berørtVedtaksperiodeId = VEDTAKSPERIODE
                        )
                    ),
            ),
            OID,
        )
    }

    private fun persisterOverstyringInntektOgRefusjon(overstyrteArbeidsgivere: List<OverstyrtArbeidsgiverForDatabase> = listOf(overstyrtArbeidsgiverForDatabase())) {
        overstyringDao.persisterOverstyringInntektOgRefusjon(
            OverstyrtInntektOgRefusjonForDatabase(
                eksternHendelseId = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                saksbehandlerOid = UUID.randomUUID(),
                arbeidsgivere =
                    overstyrteArbeidsgivere,
            ),
            OID,
        )
    }

    private fun overstyrtArbeidsgiverForDatabase(organisasjonsnummer: String = ORGNUMMER) = OverstyrtArbeidsgiverForDatabase(
        organisasjonsnummer = organisasjonsnummer,
        månedligInntekt = INNTEKT,
        fraMånedligInntekt = INNTEKT + 1,
        refusjonsopplysninger =
            listOf(
                RefusjonselementForDatabase(
                    fom = 1.januar,
                    tom = 31.januar,
                    beløp = 1000.0,
                ),
            ),
        fraRefusjonsopplysninger = null,
        begrunnelse = BEGRUNNELSE,
        forklaring = FORKLARING,
        lovhjemmel = null,
        fom = 1.januar,
        tom = null
    )

    private fun persisterOverstyringArbeidsforhold(
        eksternHendelsesIdArbeidsforhold: UUID, overstyrteArbeidsforhold: List<ArbeidsforholdForDatabase> = listOf(arbeidsforholdForDatabase())
    ) {
        overstyringDao.persisterOverstyringArbeidsforhold(
            OverstyrtArbeidsforholdForDatabase(
                eksternHendelseId = eksternHendelsesIdArbeidsforhold,
                fødselsnummer = FNR,
                aktørId = AKTØR,
                skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                saksbehandlerOid = UUID.randomUUID(),
                overstyrteArbeidsforhold =
                    overstyrteArbeidsforhold,
            ),
            OID,
        )
    }

    private fun arbeidsforholdForDatabase(organisasjonsnummer: String = ORGNUMMER) = ArbeidsforholdForDatabase(
        organisasjonsnummer = organisasjonsnummer,
        deaktivert = DEAKTIVERT,
        begrunnelse = BEGRUNNELSE,
        forklaring = FORKLARING,
    )

    private fun persisterOverstyringTidslinje() {
        overstyringDao.persisterOverstyringTidslinje(
            OverstyrtTidslinjeForDatabase(
                eksternHendelseId = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                organisasjonsnummer = ORGNUMMER,
                dager = OVERSTYRTE_DAGER,
                begrunnelse = BEGRUNNELSE,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                saksbehandlerOid = UUID.randomUUID(),
            ),
            OID,
        )
    }

    private fun OverstyringDagDto.dtoToDatabase(): OverstyrtTidslinjedagForDatabase =
        OverstyrtTidslinjedagForDatabase(
            dato = this.dato,
            type = this.type.toString(),
            fraType = this.fraType.toString(),
            grad = this.grad,
            fraGrad = this.fraGrad,
            lovhjemmel = null,
        )
}
