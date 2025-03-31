package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.InntektskildetypeDto.ORDINÆR
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.typer.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgOverstyringDaoTest : AbstractDBIntegrationTest() {
    private val PERSON_FORNAVN = "Per"
    private val PERSON_ETTERNAVN = "Son"
    private val PERSON_FØDSELSDATO = LocalDate.of(1998, 4, 20)
    private val PERSON_KJØNN = Kjønn.Ukjent
    private val ARBEIDSGIVER_NAVN = "Skrue McDuck"
    private val DEAKTIVERT = true
    private val SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 1, 1)
    private val BEGRUNNELSE = "BegrunnelseMal\n\nBegrunnelseFritekst"
    private val BEGRUNNELSEMAL = "BegrunnelseMal"
    private val BEGRUNNELSEFRITEKST = "BegrunnelseFritekst"
    private val BEGRUNNELSEKONKLUSJON = "BegrunnelseKonklusjon"
    private val FORKLARING = "Forklaring"
    private val ÅRSAK = "Årsak"
    private val OVERSTYRTE_DAGER =
        listOf(
            OverstyrtTidslinjedag(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag.toString(),
                grad = 100,
                fraType = Dagtype.Feriedag.toString(),
                fraGrad = null,
                lovhjemmel = null,
            ),
        )
    private val INNTEKT = 31000.0
    val saksbehandler = nyLegacySaksbehandler()
    val saksbehandlerOid = SaksbehandlerOid(saksbehandler.oid)

    private fun opprettPerson() {
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
    fun `Finner opprettede overstyringer for fødselsnummer`() {
        opprettPerson()
        opprettVedtaksperiode()

        persisterOverstyringTidslinje()
        persisterOverstyringMinimumSykdomsgrad()
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiver(),
            )
        )
        persisterOverstyringArbeidsforhold(
            listOf(
                arbeidsforhold(),
            )
        )
        persisterSkjønnsfastsettingSykepengegrunnlag()


        val overstyringer = overstyringRepository.finnAktive(FNR)

        assertEquals(5, overstyringer.size)
    }

    @Test
    fun `Får tilbake én overstyring når det er gjort flere endringer i en overstyring av arbeidsforhold`() {
        opprettPerson()
        opprettVedtaksperiode()

        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(organisasjonsnummer2)
        persisterOverstyringArbeidsforhold(
            listOf(
                arbeidsforhold(),
                arbeidsforhold(organisasjonsnummer2),
            )
        )
        val overstyringer = overstyringRepository.finnAktive(FNR)

        assertEquals(1, overstyringer.size)
    }

    @Test
    fun `Får tilbake én overstyring når det er gjort flere endringer i en overstyring av inntekt og refusjon`() {
        opprettPerson()
        opprettVedtaksperiode()

        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(organisasjonsnummer2)
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiver(),
                overstyrtArbeidsgiver(organisasjonsnummer2),
            )
        )
        val overstyringer = overstyringRepository.finnAktive(FNR)

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
        assertEquals(OVERSTYRTE_DAGER, hentetOverstyring.overstyrteDager.map { it.dtoToDomain() })
        assertEquals(saksbehandler.navn, hentetOverstyring.saksbehandlerNavn)
        assertEquals(saksbehandler.ident(), hentetOverstyring.saksbehandlerIdent)
        assertFalse(hentetOverstyring.ferdigstilt)
    }

    @Test
    fun `Finner opprettede arbeidsforholdoverstyringer`() {
        opprettPerson()
        persisterOverstyringArbeidsforhold(
            listOf(
                arbeidsforhold(),
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
        assertEquals(saksbehandler.navn, hentetOverstyring.saksbehandlerNavn)
        assertEquals(saksbehandler.ident(), hentetOverstyring.saksbehandlerIdent)
        assertFalse(hentetOverstyring.ferdigstilt)
    }

    @Test
    fun `Finner opprettede inntekt- og refusjonsoverstyringer`() {
        opprettPerson()
        persisterOverstyringInntektOgRefusjon(
            listOf(
                overstyrtArbeidsgiver(),
            )
        )
        val hentetOverstyring = overstyringApiDao.finnOverstyringer(FNR).first()
        check(hentetOverstyring is OverstyringInntektDto)

        assertEquals(FNR, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FORKLARING, hentetOverstyring.forklaring)
        assertEquals(saksbehandler.navn, hentetOverstyring.saksbehandlerNavn)
        assertEquals(saksbehandler.ident(), hentetOverstyring.saksbehandlerIdent)
        assertEquals(INNTEKT, hentetOverstyring.månedligInntekt)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetOverstyring.skjæringstidspunkt)
        assertFalse(hentetOverstyring.ferdigstilt)
        assertEquals(1, hentetOverstyring.refusjonsopplysninger?.size)
        val refusjonsopplysning = hentetOverstyring.refusjonsopplysninger?.first()
        assertEquals(1 jan 2018, refusjonsopplysning?.fom)
        assertEquals(31 jan 2018, refusjonsopplysning?.tom)
        assertEquals(1000.0, refusjonsopplysning?.beløp)
        assertEquals(1 jan 2018, hentetOverstyring.fom)
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
        assertEquals(saksbehandler.navn, hentetSkjønnsfastsetting.saksbehandlerNavn)
        assertEquals(saksbehandler.ident(), hentetSkjønnsfastsetting.saksbehandlerIdent)
        assertEquals(INNTEKT, hentetSkjønnsfastsetting.årlig)
        assertEquals(INNTEKT + 1, hentetSkjønnsfastsetting.fraÅrlig)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetSkjønnsfastsetting.skjæringstidspunkt)
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
        assertEquals(1 jan 2018, hentetMinimumSykdomsgrad.perioderVurdertOk.first().fom)
        assertEquals(31 jan 2018, hentetMinimumSykdomsgrad.perioderVurdertOk.first().tom)
        assertTrue(hentetMinimumSykdomsgrad.perioderVurdertOk.isNotEmpty())
        assertTrue(hentetMinimumSykdomsgrad.perioderVurdertIkkeOk.isEmpty())
        assertEquals("en begrunnelse", hentetMinimumSykdomsgrad.begrunnelse)
        assertEquals(saksbehandler.navn, hentetMinimumSykdomsgrad.saksbehandlerNavn)
        assertEquals(saksbehandler.ident(), hentetMinimumSykdomsgrad.saksbehandlerIdent)
        assertFalse(hentetMinimumSykdomsgrad.ferdigstilt)
    }

    private fun persisterSkjønnsfastsettingSykepengegrunnlag() {
        val skjønnsfastsattSykepengegrunnlag = SkjønnsfastsattSykepengegrunnlag.ny(
            aktørId = AKTØR,
            fødselsnummer = FNR,
            skjæringstidspunkt = 1 jan 2018,
            vedtaksperiodeId = VEDTAKSPERIODE,
            saksbehandlerOid = saksbehandlerOid,
            arbeidsgivere =
                listOf(
                    SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = ORGNUMMER,
                        årlig = INNTEKT,
                        fraÅrlig = INNTEKT + 1,
                        årsak = ÅRSAK,
                        type = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                        begrunnelseMal = BEGRUNNELSEMAL,
                        begrunnelseKonklusjon = BEGRUNNELSEKONKLUSJON,
                        begrunnelseFritekst = BEGRUNNELSEFRITEKST,
                        initierendeVedtaksperiodeId = VEDTAKSPERIODE.toString(),
                        lovhjemmel = Lovhjemmel(
                            paragraf = "paragraf",
                            lovverksversjon = "lovverksversjon",
                            lovverk = "lovverk"
                        ),
                    ),
                ),
        )

        overstyringRepository.lagre(listOf(skjønnsfastsattSykepengegrunnlag))
    }

    private fun persisterOverstyringMinimumSykdomsgrad(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        organisasjonsnummer: String = ORGNUMMER,
        aktørId: String = AKTØR,
    ): MinimumSykdomsgrad {
        val overstyring = MinimumSykdomsgrad.ny(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            perioderVurdertOk = listOf(
                MinimumSykdomsgradPeriode(
                    fom = 1 jan 2018,
                    tom = 31 jan 2018
                )
            ),
            perioderVurdertIkkeOk = emptyList(),
            begrunnelse = "en begrunnelse",
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            arbeidsgivere =
                listOf(
                    MinimumSykdomsgradArbeidsgiver(
                        organisasjonsnummer = organisasjonsnummer,
                        berørtVedtaksperiodeId = vedtaksperiodeId
                    )
                ),
        )
        overstyringRepository.lagre(
            listOf(
                overstyring,
            )
        )
        return overstyring
    }

    private fun persisterOverstyringInntektOgRefusjon(
        overstyrteArbeidsgivere: List<OverstyrtArbeidsgiver> = listOf(
            overstyrtArbeidsgiver()
        )
    ) {
        overstyringRepository.lagre(
            listOf(
                OverstyrtInntektOgRefusjon.ny(
                    aktørId = AKTØR,
                    fødselsnummer = FNR,
                    skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                    vedtaksperiodeId = VEDTAKSPERIODE,
                    saksbehandlerOid = saksbehandlerOid,
                    arbeidsgivere = overstyrteArbeidsgivere,
                )
            )
        )
    }

    private fun overstyrtArbeidsgiver(organisasjonsnummer: String = ORGNUMMER) =
        OverstyrtArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            månedligInntekt = INNTEKT,
            fraMånedligInntekt = INNTEKT + 1,
            refusjonsopplysninger =
                listOf(
                    Refusjonselement(
                        fom = 1 jan 2018,
                        tom = 31 jan 2018,
                        beløp = 1000.0,
                    ),
                ),
            fraRefusjonsopplysninger = null,
            begrunnelse = BEGRUNNELSE,
            forklaring = FORKLARING,
            lovhjemmel = null,
            fom = 1 jan 2018,
            tom = null
        )

    private fun persisterOverstyringArbeidsforhold(
        overstyrteArbeidsforhold: List<Arbeidsforhold> = listOf(
            arbeidsforhold()
        )
    ) {
        overstyringRepository.lagre(
            listOf(
                OverstyrtArbeidsforhold.ny(
                    fødselsnummer = FNR,
                    aktørId = AKTØR,
                    skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
                    vedtaksperiodeId = VEDTAKSPERIODE,
                    saksbehandlerOid = saksbehandlerOid,
                    overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                )
            )
        )
    }

    private fun arbeidsforhold(organisasjonsnummer: String = ORGNUMMER) = Arbeidsforhold(
        organisasjonsnummer = organisasjonsnummer,
        deaktivert = DEAKTIVERT,
        begrunnelse = BEGRUNNELSE,
        forklaring = FORKLARING,
        lovhjemmel = null
    )

    private fun persisterOverstyringTidslinje(): OverstyrtTidslinje {
        val overstyrtTidslinje = OverstyrtTidslinje.ny(
            aktørId = AKTØR,
            fødselsnummer = FNR,
            organisasjonsnummer = ORGNUMMER,
            dager = OVERSTYRTE_DAGER,
            begrunnelse = BEGRUNNELSE,
            vedtaksperiodeId = VEDTAKSPERIODE,
            saksbehandlerOid = saksbehandlerOid,
        )
        overstyringRepository.lagre(listOf(overstyrtTidslinje))
        return overstyrtTidslinje
    }

    private fun OverstyringDagDto.dtoToDomain(): OverstyrtTidslinjedag =
        OverstyrtTidslinjedag(
            dato = this.dato,
            type = this.type.toString(),
            fraType = this.fraType.toString(),
            grad = this.grad,
            fraGrad = this.fraGrad,
            lovhjemmel = null,
        )
}
