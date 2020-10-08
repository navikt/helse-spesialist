package no.nav.helse.modell.overstyring

import DatabaseIntegrationTest
import no.nav.helse.mediator.meldinger.Kjønn
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

internal class OverstyringDaoTest : DatabaseIntegrationTest() {

    companion object {
        private const val PERSON_FORNAVN = "Per"
        private const val PERSON_ETTERNAVN = "Son"
        private val PERSON_FØDSELSDATO = LocalDate.of(1998, 4, 20)
        private val PERSON_KJØNN = Kjønn.Ukjent
        private const val ARBEIDSGIVER_NAVN = "Skrue Mc Duck"
        private val ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val AKTØR_ID = "100000234234"
        private val OID = UUID.randomUUID()
        private const val SAKSBEHANDLER_NAVN = "Saks Behandler"
        private const val EPOST = "saks.behandler@nav.no"
        private const val ORGNUMMER = "987654321"
        private const val BEGRUNNELSE = "Begrunnelse"
        private val OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = LocalDate.of(2020, 1, 1),
                type = Dagtype.Sykedag,
                grad = 100
            )
        )
    }

    @Test
    fun `Finner opprettede overstyringer`() {
        saksbehandlerDao.opprettSaksbehandler(OID, SAKSBEHANDLER_NAVN, EPOST)
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ARBEIDSGIVER_NAVN)
        val navn_ref = personDao.insertPersoninfo(PERSON_FORNAVN, null, PERSON_ETTERNAVN, PERSON_FØDSELSDATO, PERSON_KJØNN)
        val infotrygdutbetaling_ref = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        personDao.insertPerson(FØDSELSNUMMER, AKTØR_ID, navn_ref, 420, infotrygdutbetaling_ref)
        overstyringDao.persisterOverstyring(ID, FØDSELSNUMMER, ORGNUMMER, BEGRUNNELSE, OVERSTYRTE_DAGER, OID)
        val hentetOverstyring = overstyringDao.finnOverstyring(FØDSELSNUMMER, ORGNUMMER).first()

        assertEquals(ID, hentetOverstyring.hendelseId)
        assertEquals(BEGRUNNELSE, hentetOverstyring.begrunnelse)
        assertEquals(FØDSELSNUMMER, hentetOverstyring.fødselsnummer)
        assertEquals(ORGNUMMER, hentetOverstyring.organisasjonsnummer)
        assertEquals(OVERSTYRTE_DAGER, hentetOverstyring.overstyrteDager)
        assertEquals(SAKSBEHANDLER_NAVN, hentetOverstyring.saksbehandlerNavn)
    }

}
