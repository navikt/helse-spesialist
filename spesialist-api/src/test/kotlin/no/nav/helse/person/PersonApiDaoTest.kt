package no.nav.helse.person

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PersonApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `skal oppdage at en person har fortrolig adresse`() {
        person(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        assertTrue(personApiDao.personErKode7(FØDSELSNUMMER))
    }

    @Test
    fun `person med ugradert adresse er ikke kode 7`() {
        person(adressebeskyttelse = Adressebeskyttelse.Ugradert)
        assertFalse(personApiDao.personErKode7(FØDSELSNUMMER))
    }

    @Test
    fun `person med strengt fortrolig adresse er ikke kode 7`() {
        person(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        assertFalse(personApiDao.personErKode7(FØDSELSNUMMER))
    }

    @Test
    fun `person med strengt fortrolig utland adresse er ikke kode 7`() {
        person(adressebeskyttelse = Adressebeskyttelse.StrengtFortroligUtland)
        assertFalse(personApiDao.personErKode7(FØDSELSNUMMER))
    }

    @Test
    fun `person med ukjent adressebeskyttelse er (kanskje) ikke kode 7`() {
        person(adressebeskyttelse = Adressebeskyttelse.Ukjent)
        assertFalse(personApiDao.personErKode7(FØDSELSNUMMER))
    }
}
