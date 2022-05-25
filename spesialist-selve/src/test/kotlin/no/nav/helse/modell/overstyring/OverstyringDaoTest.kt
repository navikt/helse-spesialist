package no.nav.helse.modell.overstyring

import DatabaseIntegrationTest
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.OverstyringArbeidsforhold
import no.nav.helse.overstyring.Dagtype
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.person.Kjønn
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals

internal class OverstyringDaoTest : DatabaseIntegrationTest() {

    companion object {
        private const val PERSON_FORNAVN = "Per"
        private const val PERSON_ETTERNAVN = "Son"
        private val PERSON_FØDSELSDATO = LocalDate.of(1998, 4, 20)
        private val PERSON_KJØNN = Kjønn.Ukjent
        private const val ARBEIDSGIVER_NAVN = "Skrue Mc Duck"
        private val ID = UUID.randomUUID()
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
        private val OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag,
                grad = 100
            )
        )
    }

    private fun opprettPerson() {
        saksbehandlerDao.opprettSaksbehandler(OID, SAKSBEHANDLER_NAVN, EPOST, SAKSBEHANDLER_IDENT)
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ARBEIDSGIVER_NAVN, BRANSJER)
        val navn_ref = personDao.insertPersoninfo(PERSON_FORNAVN, null, PERSON_ETTERNAVN, PERSON_FØDSELSDATO, PERSON_KJØNN, ADRESSEBESKYTTELSE)
        val infotrygdutbetaling_ref = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        personDao.insertPerson(FØDSELSNUMMER, AKTØR_ID, navn_ref, 420, infotrygdutbetaling_ref)
    }

    @Test
    fun `Finner opprettede tidslinjeoverstyringer`() {
        opprettPerson()
        overstyringDao.persisterOverstyringTidslinje(ID, FØDSELSNUMMER, ORGNUMMER, BEGRUNNELSE, OVERSTYRTE_DAGER, OID)
        val hentetOverstyring = overstyringApiDao.finnOverstyringerAvTidslinjer(FØDSELSNUMMER, ORGNUMMER).first()

        assertEquals(ID, hentetOverstyring.hendelseId)
        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FØDSELSNUMMER, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(OVERSTYRTE_DAGER, hentetOverstyring.overstyrteDager)
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
        assertEquals(SAKSBEHANDLER_IDENT, hentetOverstyring.saksbehandlerIdent)
    }

    @Test
    fun `Finner opprettede arbeidsforholdoverstyringer`() {
        opprettPerson()
        hendelseDao.opprett(OverstyringArbeidsforhold(
            id = ID,
            fødselsnummer = FØDSELSNUMMER,
            oid = OID,
            navn = SAKSBEHANDLER_NAVN,
            epost = SAKSBEHANDLEREPOST,
            ident = SAKSBEHANDLER_IDENT,
            organisasjonsnummer = ORGNUMMER,
            skjæringstidspunkt = SKJÆRINGSTIDSPUNKT,
            overstyrteArbeidsforhold = listOf(
                OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                    orgnummer = GHOST_ORGNUMMER,
                    deaktivert = DEAKTIVERT,
                    begrunnelse = BEGRUNNELSE,
                    forklaring = FORKLARING
                )
            ),
            json = "{}",
            reservasjonDao = reservasjonDao,
            saksbehandlerDao = saksbehandlerDao,
            overstyringDao = overstyringDao
        ))
        overstyringDao.persisterOverstyringArbeidsforhold(
            ID,
            FØDSELSNUMMER,
            ORGNUMMER,
            BEGRUNNELSE,
            FORKLARING,
            DEAKTIVERT,
            SKJÆRINGSTIDSPUNKT,
            OID
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
    }
}
