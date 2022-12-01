package no.nav.helse.spesialist.api.person

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PersonApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `skal oppdage at en person har fortrolig adresse`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        assertTrue(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `person med ugradert adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Ugradert)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `person med strengt fortrolig adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `person med strengt fortrolig utland adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.StrengtFortroligUtland)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `person med ukjent adressebeskyttelse er (kanskje) ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Ukjent)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `henter fødselsnummer for person med gitt aktørId`() {
        opprettPerson()
        assertEquals(FØDSELSNUMMER, personApiDao.finnFødselsnummer(AKTØRID.toLong()))
    }

    @Test
    fun `kan svare på om en person er klar for visning, hvilket vil si om spesialist har preppet ferdig minst en gang`() {
        val personId = opprettPerson()
        assertPersonenErIkkeKlar()

        val vedtakId = opprettVedtak(personId, opprettArbeidsgiver())
        assertPersonenErIkkeKlar()

        klargjørVedtak(vedtakId, PERIODE, Oppgavetype.SØKNAD)
        assertPersonenErKlar()
    }

    private fun personenErKlar() = personApiDao.spesialistHarPersonKlarForVisningISpeil(FØDSELSNUMMER)
    private fun assertPersonenErIkkeKlar() = assertFalse(personenErKlar())
    private fun assertPersonenErKlar() = assertTrue(personenErKlar())
}
