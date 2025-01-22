package no.nav.helse.db.api

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgPersonApiDaoTest : DatabaseIntegrationTest() {

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
    fun `kan svare på om en person er klar for visning`() {
        val personId = opprettMinimalPerson()
        assertPersonenErIkkeKlar()

        oppdaterEnhet(personId, 101)
        assertPersonenErIkkeKlar()

        opprettEgenAnsatt(personId, false)
        assertPersonenErIkkeKlar()

        oppdaterPersoninfo(Ugradert)
        assertPersonenErKlar()
    }

    // Denne testen komplementerer den ovenstående, for å vise at både personinfo og info om egen ansatt må finnes
    @Test
    fun `kan svare på om en person er klar for visning, med dataene mottatt i ikke-realistisk rekkefølge`() {
        val personId = opprettMinimalPerson()
        assertPersonenErIkkeKlar()

        oppdaterPersoninfo(Ugradert)
        assertPersonenErIkkeKlar()

        opprettEgenAnsatt(personId, false)
        assertPersonenErIkkeKlar()

        oppdaterEnhet(personId, 101)

        assertPersonenErKlar()
    }

    @Test
    fun `kan markere at en person er under klargjøring`() {
        assertFalse(personApiDao.klargjøringPågår(FØDSELSNUMMER))
        personApiDao.personKlargjøres(FØDSELSNUMMER)
        assertTrue(personApiDao.klargjøringPågår(FØDSELSNUMMER))
    }

    private fun harDataNødvendigForVisning() = personApiDao.harDataNødvendigForVisning(FØDSELSNUMMER)
    private fun assertPersonenErIkkeKlar() = assertFalse(harDataNødvendigForVisning())
    private fun assertPersonenErKlar() = assertTrue(harDataNødvendigForVisning())
}
