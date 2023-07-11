package no.nav.helse.modell.overstyring

import DatabaseIntegrationTest
import io.mockk.mockk
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.SkjønnsfastsettingSykepengegrunnlag
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringTidslinje
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyringDaoTest : DatabaseIntegrationTest() {

    companion object {
        private const val PERSON_FORNAVN = "Per"
        private const val PERSON_ETTERNAVN = "Son"
        private val PERSON_FØDSELSDATO = LocalDate.of(1998, 4, 20)
        private val PERSON_KJØNN = Kjønn.Ukjent
        private const val ARBEIDSGIVER_NAVN = "Skrue McDuck"
        private val ID = UUID.randomUUID()
        private val EKSTERN_HENDELSE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val DEAKTIVERT = true
        private val SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 1, 1)
        private const val AKTØR_ID = "100000234234"
        private val OID = UUID.randomUUID()
        private const val SAKSBEHANDLER_NAVN = "Saks Behandler"
        private const val SAKSBEHANDLER_IDENT = "Z999999"
        private const val EPOST = "saks.behandler@nav.no"
        private const val ORGNUMMER = "987654321"
        private const val GHOST_ORGNUMMER = "123412"
        private const val BEGRUNNELSE = "Begrunnelse"
        private const val FORKLARING = "Forklaring"
        private const val ÅRSAK = "Årsak"
        private val OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag,
                grad = 100,
                fraType = Dagtype.Feriedag,
                fraGrad = null
            )
        )
        private val OPPRETTET = LocalDate.of(2022, 6, 9).atStartOfDay()
        private const val INNTEKT = 31000.0
    }

    private fun opprettPerson() {
        saksbehandlerDao.opprettSaksbehandler(OID, SAKSBEHANDLER_NAVN, EPOST, SAKSBEHANDLER_IDENT)
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ARBEIDSGIVER_NAVN, BRANSJER)
        val navn_ref = personDao.insertPersoninfo(PERSON_FORNAVN, null, PERSON_ETTERNAVN, PERSON_FØDSELSDATO, PERSON_KJØNN, ADRESSEBESKYTTELSE)
        val infotrygdutbetaling_ref = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        personDao.insertPerson(FØDSELSNUMMER, AKTØR_ID, navn_ref, 420, infotrygdutbetaling_ref)
    }

    @Test
    fun `Kan koble overstyringhendelse og vedtaksperiode`() {
        opprettPerson()
        hendelseDao.opprett(overstyringTidslinje())
        overstyringDao.persisterOverstyringTidslinje(ID, EKSTERN_HENDELSE_ID, FØDSELSNUMMER, ORGNUMMER, BEGRUNNELSE, OVERSTYRTE_DAGER, OID, OPPRETTET)
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)

        assertTrue(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))
        assertFalse(overstyringDao.harVedtaksperiodePågåendeOverstyring(UUID.randomUUID()))
    }

    @Test
    fun `Finnes ekstern_hendelse_id i overstyringtabell`() {
        opprettPerson()
        hendelseDao.opprett(overstyringTidslinje())
        overstyringDao.persisterOverstyringTidslinje(ID, EKSTERN_HENDELSE_ID, FØDSELSNUMMER, ORGNUMMER, BEGRUNNELSE, OVERSTYRTE_DAGER, OID, OPPRETTET)

        assertTrue(overstyringDao.finnesEksternHendelseId(EKSTERN_HENDELSE_ID))
        assertFalse(overstyringDao.finnesEksternHendelseId(UUID.randomUUID()))
    }

    @Test
    fun `Vedtaksperiode har ikke pågående overstyring etter ferdigstilling`() {
        opprettPerson()
        hendelseDao.opprett(overstyringTidslinje())
        overstyringDao.persisterOverstyringTidslinje(ID, EKSTERN_HENDELSE_ID, FØDSELSNUMMER, ORGNUMMER, BEGRUNNELSE, OVERSTYRTE_DAGER, OID, OPPRETTET)
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)

        val hentetOverstyring = overstyringApiDao.finnOverstyringerAvTidslinjer(FØDSELSNUMMER, ORGNUMMER).first()
        assertFalse(hentetOverstyring.ferdigstilt)

        assertTrue(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))
        overstyringDao.ferdigstillOverstyringerForVedtaksperiode(VEDTAKSPERIODE)
        assertFalse(overstyringDao.harVedtaksperiodePågåendeOverstyring(VEDTAKSPERIODE))

        val hentetOverstyringEtterFerdigstilling = overstyringApiDao.finnOverstyringerAvTidslinjer(FØDSELSNUMMER, ORGNUMMER).first()
        assertTrue(hentetOverstyringEtterFerdigstilling.ferdigstilt)
    }

    @Test
    fun `Finner opprettede tidslinjeoverstyringer`() {
        opprettPerson()
        hendelseDao.opprett(overstyringTidslinje())
        overstyringDao.persisterOverstyringTidslinje(ID, EKSTERN_HENDELSE_ID, FØDSELSNUMMER, ORGNUMMER, BEGRUNNELSE, OVERSTYRTE_DAGER, OID, OPPRETTET)
        val hentetOverstyring = overstyringApiDao.finnOverstyringerAvTidslinjer(FØDSELSNUMMER, ORGNUMMER).first()

        assertEquals(ID, hentetOverstyring.hendelseId)
        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FØDSELSNUMMER, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(OVERSTYRTE_DAGER, hentetOverstyring.overstyrteDager)
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetOverstyring.saksbehandlerIdent)
        assertEquals(OPPRETTET, hentetOverstyring.timestamp)
        assertFalse(hentetOverstyring.ferdigstilt)
    }

    @Test
    fun `Finner opprettede arbeidsforholdoverstyringer`() {
        opprettPerson()
        hendelseDao.opprett(overstyringArbeidsforhold())
        overstyringDao.persisterOverstyringArbeidsforhold(
            ID,
            EKSTERN_HENDELSE_ID,
            FØDSELSNUMMER,
            ORGNUMMER,
            BEGRUNNELSE,
            FORKLARING,
            DEAKTIVERT,
            SKJÆRINGSTIDSPUNKT,
            OID,
            OPPRETTET
        )
        val hentetOverstyring = overstyringApiDao.finnOverstyringerAvArbeidsforhold(FØDSELSNUMMER, ORGNUMMER).single()

        assertEquals(ID, hentetOverstyring.hendelseId)
        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FORKLARING, hentetOverstyring.forklaring)
        assertEquals(DEAKTIVERT, hentetOverstyring.deaktivert)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetOverstyring.skjæringstidspunkt)
        assertEquals(FØDSELSNUMMER, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetOverstyring.saksbehandlerIdent)
        assertEquals(OPPRETTET, hentetOverstyring.timestamp)
        assertFalse(hentetOverstyring.ferdigstilt)
    }

    @Test
    fun `Finner opprettede inntekt- og refusjonsoverstyringer`() {
        opprettPerson()
        overstyrInntektOgRefusjon(ID)
        overstyringDao.persisterOverstyringInntektOgRefusjon(
            ID,
            EKSTERN_HENDELSE_ID,
            FØDSELSNUMMER,
            listOf(
                OverstyrtArbeidsgiver(
                    organisasjonsnummer = ORGNUMMER,
                    begrunnelse = BEGRUNNELSE,
                    forklaring = FORKLARING,
                    månedligInntekt = INNTEKT,
                    fraMånedligInntekt = INNTEKT + 1,
                    refusjonsopplysninger = listOf(Refusjonselement(1.januar, 31.januar, 1000.0)),
                    fraRefusjonsopplysninger = null,
                    subsumsjon = Subsumsjon(paragraf = "87494")
                )
            ),
            OID,
            SKJÆRINGSTIDSPUNKT,
            OPPRETTET
        )
        val hentetOverstyring = overstyringApiDao.finnOverstyringerAvInntektOgRefusjon(FØDSELSNUMMER, ORGNUMMER).first()

        assertEquals(ID, hentetOverstyring.hendelseId)
        assertEquals(FØDSELSNUMMER, hentetOverstyring.fødselsnummer)
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
    }

    @Test
    fun `Finner opprettede skjønnsfastsatte sykepengegrunnlag`() {
        opprettPerson()
        skjønnsfastsettingSykepengegrunnlag(ID)
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            ID,
            EKSTERN_HENDELSE_ID,
            FØDSELSNUMMER,
            listOf(
                SkjønnsfastsattArbeidsgiver(
                    organisasjonsnummer = ORGNUMMER,
                    årlig = INNTEKT,
                    fraÅrlig = INNTEKT + 1,
                    årsak = ÅRSAK,
                    begrunnelse = BEGRUNNELSE,
                    subsumsjon = Subsumsjon(paragraf = "87494"),
                    initierendeVedtaksperiodeId = VEDTAKSPERIODE
                )
            ),
            OID,
            SKJÆRINGSTIDSPUNKT,
            OPPRETTET
        )
        val hentetSkjønnsfastsetting = overstyringApiDao.finnSkjønnsfastsettingSykepengegrunnlag(FØDSELSNUMMER, ORGNUMMER).first()

        assertEquals(ID, hentetSkjønnsfastsetting.hendelseId)
        assertEquals(FØDSELSNUMMER, hentetSkjønnsfastsetting.fødselsnummer)
        assertEquals(ORGNUMMER, hentetSkjønnsfastsetting.organisasjonsnummer)
        assertEquals(BEGRUNNELSE, hentetSkjønnsfastsetting.begrunnelse)
        assertEquals(ÅRSAK, hentetSkjønnsfastsetting.årsak)
        assertEquals(SAKSBEHANDLER_NAVN, hentetSkjønnsfastsetting.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetSkjønnsfastsetting.saksbehandlerIdent)
        assertEquals(INNTEKT, hentetSkjønnsfastsetting.årlig)
        assertEquals(INNTEKT + 1, hentetSkjønnsfastsetting.fraÅrlig)
        assertEquals(SKJÆRINGSTIDSPUNKT, hentetSkjønnsfastsetting.skjæringstidspunkt)
        assertEquals(OPPRETTET, hentetSkjønnsfastsetting.timestamp)
        assertFalse(hentetSkjønnsfastsetting.ferdigstilt)
    }

    @Test
    fun `Finner hendelsesid'er for ikke ferdigstilte overstyringer for vedtaksperiode`() {
        opprettPerson()
        overstyrInntektOgRefusjon(ID)
        overstyringDao.persisterOverstyringInntektOgRefusjon(
            ID,
            EKSTERN_HENDELSE_ID,
            FØDSELSNUMMER,
            listOf(
                OverstyrtArbeidsgiver(
                    organisasjonsnummer = ORGNUMMER,
                    begrunnelse = BEGRUNNELSE,
                    forklaring = FORKLARING,
                    månedligInntekt = INNTEKT,
                    fraMånedligInntekt = INNTEKT + 1,
                    refusjonsopplysninger = null,
                    fraRefusjonsopplysninger = null,
                    subsumsjon = Subsumsjon(paragraf = "87494")
                )
            ),
            OID,
            SKJÆRINGSTIDSPUNKT,
            OPPRETTET
        )
        hendelseDao.opprett(overstyringArbeidsforhold())
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), EKSTERN_HENDELSE_ID)
        val eksternHendelsesIdArbeidsforhold = UUID.randomUUID()
        overstyringDao.persisterOverstyringArbeidsforhold(
            ID,
            eksternHendelsesIdArbeidsforhold,
            FØDSELSNUMMER,
            ORGNUMMER,
            BEGRUNNELSE,
            FORKLARING,
            DEAKTIVERT,
            SKJÆRINGSTIDSPUNKT,
            OID,
            OPPRETTET
        )
        overstyringDao.kobleOverstyringOgVedtaksperiode(listOf(VEDTAKSPERIODE), eksternHendelsesIdArbeidsforhold)

        val aktiveOverstyringer = overstyringDao.finnAktiveOverstyringer(VEDTAKSPERIODE)

        assertEquals(EKSTERN_HENDELSE_ID, aktiveOverstyringer.first())
        assertEquals(eksternHendelsesIdArbeidsforhold, aktiveOverstyringer.last())
    }

    private fun overstyrInntektOgRefusjon(hendelseId: UUID) = hendelseDao.opprett(
        OverstyringInntektOgRefusjon(
            id = hendelseId,
            fødselsnummer = FØDSELSNUMMER,
            oid = OID,
            navn = SAKSBEHANDLER_NAVN,
            epost = SAKSBEHANDLEREPOST,
            ident = SAKSBEHANDLER_IDENT,
            arbeidsgivere = listOf(
                OverstyrtArbeidsgiver(
                    organisasjonsnummer = ORGNUMMER,
                    begrunnelse = BEGRUNNELSE,
                    forklaring = FORKLARING,
                    månedligInntekt = INNTEKT,
                    fraMånedligInntekt = INNTEKT + 1,
                    refusjonsopplysninger = null,
                    fraRefusjonsopplysninger = null,
                    subsumsjon = Subsumsjon(paragraf = "87494")
                )
            ),
            skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
            opprettet = OPPRETTET,
            json = "{}",
            reservasjonDao = reservasjonDao,
            saksbehandlerDao = saksbehandlerDao,
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDao,
            overstyringDao = overstyringDao,
            overstyringMediator = mockk(),
        )
    )

    private fun skjønnsfastsettingSykepengegrunnlag(hendelseId: UUID) = hendelseDao.opprett(
        SkjønnsfastsettingSykepengegrunnlag(
            id = hendelseId,
            fødselsnummer = FØDSELSNUMMER,
            oid = OID,
            navn = SAKSBEHANDLER_NAVN,
            epost = SAKSBEHANDLEREPOST,
            ident = SAKSBEHANDLER_IDENT,
            arbeidsgivere = listOf(
                SkjønnsfastsattArbeidsgiver(
                    organisasjonsnummer = ORGNUMMER,
                    årlig = INNTEKT,
                    fraÅrlig = INNTEKT + 1,
                    årsak = ÅRSAK,
                    begrunnelse = BEGRUNNELSE,
                    subsumsjon = Subsumsjon(paragraf = "87494"),
                    initierendeVedtaksperiodeId = VEDTAKSPERIODE
                )
            ),
            skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
            opprettet = OPPRETTET,
            json = "{}",
            reservasjonDao = reservasjonDao,
            saksbehandlerDao = saksbehandlerDao,
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDao,
            overstyringDao = overstyringDao,
            overstyringMediator = mockk(),
            versjonAvKode = "versjonAvKode",
        )
    )

    private fun overstyringTidslinje() = OverstyringTidslinje(
        id = ID,
        fødselsnummer = FØDSELSNUMMER,
        oid = OID,
        navn = SAKSBEHANDLER_NAVN,
        epost = SAKSBEHANDLEREPOST,
        ident = SAKSBEHANDLER_IDENT,
        orgnummer = ORGNUMMER,
        begrunnelse = BEGRUNNELSE,
        overstyrteDager = OVERSTYRTE_DAGER,
        opprettet = OPPRETTET,
        json = "{}",
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        oppgaveDao = oppgaveDao,
        tildelingDao = tildelingDao,
        overstyringDao = overstyringDao,
        overstyringMediator = mockk(),
    )

    private fun overstyringArbeidsforhold() = OverstyringArbeidsforhold(
        id = ID,
        fødselsnummer = FØDSELSNUMMER,
        oid = OID,
        navn = SAKSBEHANDLER_NAVN,
        epost = SAKSBEHANDLEREPOST,
        ident = SAKSBEHANDLER_IDENT,
        overstyrteArbeidsforhold = listOf(
            OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                orgnummer = GHOST_ORGNUMMER,
                deaktivert = DEAKTIVERT,
                begrunnelse = BEGRUNNELSE,
                forklaring = FORKLARING
            )
        ),
        skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
        opprettet = OPPRETTET,
        json = "{}",
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        oppgaveDao = oppgaveDao,
        tildelingDao = tildelingDao,
        overstyringDao = overstyringDao,
        overstyringMediator = mockk(),
    )
}
