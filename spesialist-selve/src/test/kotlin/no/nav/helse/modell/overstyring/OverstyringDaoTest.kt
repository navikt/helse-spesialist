package no.nav.helse.modell.overstyring

import DatabaseIntegrationTest
import no.nav.helse.db.ArbeidsforholdForDatabase
import no.nav.helse.db.InntektskilderDao
import no.nav.helse.db.LovhjemmelForDatabase
import no.nav.helse.db.MinimumSykdomsgradForDatabase
import no.nav.helse.db.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.OverstyrtArbeidsgiverForDatabase
import no.nav.helse.db.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.RefusjonselementForDatabase
import no.nav.helse.db.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.januar
import no.nav.helse.modell.InntektskildetypeDto.ORDINÆR
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OverstyringDaoTest : DatabaseIntegrationTest() {
    private val inntektskilderDao = InntektskilderDao(dataSource)
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
        saksbehandlerDao.opprettSaksbehandler(OID, SAKSBEHANDLER_NAVN, EPOST, SAKSBEHANDLER_IDENT)
        inntektskilderDao.lagreInntektskilder(
            listOf(
                KomplettInntektskildeDto(ORGNUMMER, ORDINÆR, ARBEIDSGIVER_NAVN, BRANSJER, LocalDate.now())
            )
        )
        val navn_ref =
            personDao.insertPersoninfo(
                PERSON_FORNAVN,
                null,
                PERSON_ETTERNAVN,
                PERSON_FØDSELSDATO,
                PERSON_KJØNN,
                ADRESSEBESKYTTELSE,
            )
        val infotrygdutbetaling_ref = personDao.upsertInfotrygdutbetalinger(FNR, objectMapper.createObjectNode())
        personDao.insertPerson(FNR, AKTØR, navn_ref, 420, infotrygdutbetaling_ref)
    }

    @Test
    fun `Kan koble overstyringhendelse og vedtaksperiode`() {
        opprettPerson()
        overstyringDao.persisterOverstyringTidslinje(
            OverstyrtTidslinjeForDatabase(
                EKSTERN_HENDELSE_ID,
                AKTØR,
                FNR,
                ORGNUMMER,
                OVERSTYRTE_DAGER,
                BEGRUNNELSE,
                OPPRETTET,
                VEDTAKSPERIODE,
            ),
            OID,
        )
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)

        assertTrue(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))
        assertFalse(overstyringDao.harVedtaksperiodePågåendeOverstyring(UUID.randomUUID()))
    }

    @Test
    fun `Finnes ekstern_hendelse_id i overstyringtabell`() {
        opprettPerson()
        overstyringDao.persisterOverstyringTidslinje(
            OverstyrtTidslinjeForDatabase(
                EKSTERN_HENDELSE_ID,
                AKTØR,
                FNR,
                ORGNUMMER,
                OVERSTYRTE_DAGER,
                BEGRUNNELSE,
                OPPRETTET,
                VEDTAKSPERIODE,
            ),
            OID,
        )

        assertTrue(overstyringDao.finnesEksternHendelseId(EKSTERN_HENDELSE_ID))
        assertFalse(overstyringDao.finnesEksternHendelseId(UUID.randomUUID()))
    }

    @Test
    fun `Vedtaksperiode har ikke pågående overstyring etter ferdigstilling`() {
        opprettPerson()
        overstyringDao.persisterOverstyringTidslinje(
            OverstyrtTidslinjeForDatabase(
                EKSTERN_HENDELSE_ID,
                AKTØR,
                FNR,
                ORGNUMMER,
                OVERSTYRTE_DAGER,
                BEGRUNNELSE,
                OPPRETTET,
                VEDTAKSPERIODE,
            ),
            OID,
        )
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
    fun `Finner opprettede tidslinjeoverstyringer`() {
        opprettPerson()
        overstyringDao.persisterOverstyringTidslinje(
            OverstyrtTidslinjeForDatabase(
                EKSTERN_HENDELSE_ID,
                AKTØR,
                FNR,
                ORGNUMMER,
                OVERSTYRTE_DAGER,
                BEGRUNNELSE,
                OPPRETTET,
                VEDTAKSPERIODE,
            ),
            OID,
        )
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
        overstyringDao.persisterOverstyringArbeidsforhold(
            OverstyrtArbeidsforholdForDatabase(
                id = EKSTERN_HENDELSE_ID,
                fødselsnummer = FNR,
                aktørId = AKTØR,
                skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                overstyrteArbeidsforhold =
                    listOf(
                        ArbeidsforholdForDatabase(
                            organisasjonsnummer = ORGNUMMER,
                            deaktivert = DEAKTIVERT,
                            begrunnelse = BEGRUNNELSE,
                            forklaring = FORKLARING,
                        ),
                    ),
            ),
            OID,
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
        overstyringDao.persisterOverstyringInntektOgRefusjon(
            OverstyrtInntektOgRefusjonForDatabase(
                id = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                arbeidsgivere =
                    listOf(
                        OverstyrtArbeidsgiverForDatabase(
                            organisasjonsnummer = ORGNUMMER,
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
                        ),
                    ),
            ),
            OID,
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
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            SkjønnsfastsattSykepengegrunnlagForDatabase(
                id = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                skjæringstidspunkt = 1.januar,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
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
        overstyringDao.persisterMinimumSykdomsgrad(
            MinimumSykdomsgradForDatabase(
                id = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                fom = 1.januar,
                tom = 31.januar,
                vurdering = true,
                begrunnelse = "en begrunnelse",
                opprettet = OPPRETTET,
                initierendeVedtaksperiodeId = VEDTAKSPERIODE,
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
        val hentetMinimumSykdomsgrad = overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetMinimumSykdomsgrad is OverstyringMinimumSykdomsgradDto)

        assertEquals(FNR, hentetMinimumSykdomsgrad.fødselsnummer)
        assertEquals(ORGNUMMER, hentetMinimumSykdomsgrad.organisasjonsnummer)
        assertEquals(1.januar, hentetMinimumSykdomsgrad.fom)
        assertEquals(31.januar, hentetMinimumSykdomsgrad.tom)
        assertTrue(hentetMinimumSykdomsgrad.vurdering)
        assertEquals("en begrunnelse", hentetMinimumSykdomsgrad.begrunnelse,)
        assertEquals(SAKSBEHANDLER_NAVN, hentetMinimumSykdomsgrad.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetMinimumSykdomsgrad.saksbehandlerIdent)
        assertEquals(OPPRETTET, hentetMinimumSykdomsgrad.timestamp)
        assertFalse(hentetMinimumSykdomsgrad.ferdigstilt)
    }

    @Test
    fun `Finner hendelsesid'er for ikke ferdigstilte overstyringer for vedtaksperiode`() {
        opprettPerson()
        overstyringDao.persisterOverstyringInntektOgRefusjon(
            OverstyrtInntektOgRefusjonForDatabase(
                id = EKSTERN_HENDELSE_ID,
                aktørId = AKTØR,
                fødselsnummer = FNR,
                skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                arbeidsgivere =
                    listOf(
                        OverstyrtArbeidsgiverForDatabase(
                            organisasjonsnummer = ORGNUMMER,
                            månedligInntekt = INNTEKT,
                            fraMånedligInntekt = INNTEKT + 1,
                            refusjonsopplysninger = null,
                            fraRefusjonsopplysninger = null,
                            begrunnelse = BEGRUNNELSE,
                            forklaring = FORKLARING,
                            lovhjemmel = null,
                            fom = 1.januar,
                            tom = null,
                        ),
                    ),
            ),
            OID,
        )
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)
        val eksternHendelsesIdArbeidsforhold = UUID.randomUUID()
        overstyringDao.persisterOverstyringArbeidsforhold(
            OverstyrtArbeidsforholdForDatabase(
                id = eksternHendelsesIdArbeidsforhold,
                fødselsnummer = FNR,
                aktørId = AKTØR,
                skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                opprettet = OPPRETTET,
                vedtaksperiodeId = VEDTAKSPERIODE,
                overstyrteArbeidsforhold =
                    listOf(
                        ArbeidsforholdForDatabase(
                            organisasjonsnummer = ORGNUMMER,
                            deaktivert = DEAKTIVERT,
                            begrunnelse = BEGRUNNELSE,
                            forklaring = FORKLARING,
                        ),
                    ),
            ),
            OID,
        )
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), eksternHendelsesIdArbeidsforhold)

        val aktiveOverstyringer = overstyringDao.finnAktiveOverstyringer(VEDTAKSPERIODE)

        assertEquals(EKSTERN_HENDELSE_ID, aktiveOverstyringer.first())
        assertEquals(eksternHendelsesIdArbeidsforhold, aktiveOverstyringer.last())
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
