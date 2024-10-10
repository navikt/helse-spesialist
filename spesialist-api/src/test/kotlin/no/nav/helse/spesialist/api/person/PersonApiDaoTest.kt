package no.nav.helse.spesialist.api.person

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PersonApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `skal oppdage at en person har fortrolig adresse`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        assertTrue(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `person med ugradert adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Ugradert)
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

        klargjørVedtak(vedtakId = vedtakId, periode = PERIODE)
        assertPersonenErKlar()
    }

    @Test
    fun `kan svare på om en person er klar for visning`() {
        val personId = opprettMinimalPerson()
        assertFalse(harDataNødvendigForVisning())

        oppdaterEnhet(personId, 101)
        assertFalse(harDataNødvendigForVisning())

        opprettEgenAnsatt(personId, false)
        assertFalse(harDataNødvendigForVisning())

        oppdaterPersoninfo(Ugradert)
        assertTrue(harDataNødvendigForVisning())
    }

    // Denne testen komplementerer den ovenstående, for å vise at både personinfo og info om egen ansatt må finnes
    @Test
    fun `kan svare på om en person er klar for visning, med dataene mottatt i ikke-realistisk rekkefølge`() {
        val personId = opprettMinimalPerson()
        assertFalse(harDataNødvendigForVisning())

        oppdaterPersoninfo(Ugradert)
        assertFalse(harDataNødvendigForVisning())

        opprettEgenAnsatt(personId, false)
        assertFalse(harDataNødvendigForVisning())

        oppdaterEnhet(personId, 101)

        assertTrue(harDataNødvendigForVisning())
    }

    @Test
    fun `kan markere at en person er under klargjøring`() {
        assertFalse(personApiDao.klargjøringPågår(FØDSELSNUMMER))
        personApiDao.personKlargjøres(FØDSELSNUMMER)
        assertTrue(personApiDao.klargjøringPågår(FØDSELSNUMMER))
    }

    @Test
    fun `feiler ikke når det fins mer enn ett vedtak`() {
        val personId = opprettPerson()
        assertPersonenErIkkeKlar()

        val arbeidsgiverId = opprettArbeidsgiver()
        val vedtakId = opprettVedtak(personId, arbeidsgiverId)
        assertPersonenErIkkeKlar()

        klargjørVedtak(vedtakId = vedtakId, periode = PERIODE)
        assertPersonenErKlar()
        val periode2 = Periode(UUID.randomUUID(), LocalDate.of(2021, 2, 1), LocalDate.of(2021, 2, 12))
        val vedtakId2 = opprettVedtak(personId, arbeidsgiverId, periode2)
        klargjørVedtak(vedtakId = vedtakId2, periode = periode2)
        assertPersonenErKlar()
    }

    private fun personenErKlar() = personApiDao.spesialistHarPersonKlarForVisningISpeil(FØDSELSNUMMER)
    private fun harDataNødvendigForVisning() = personApiDao.harDataNødvendigForVisning(FØDSELSNUMMER)
    private fun assertPersonenErIkkeKlar() = assertFalse(personenErKlar())
    private fun assertPersonenErKlar() = assertTrue(personenErKlar())
}
