package no.nav.helse.db.api

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.Adressebeskyttelse.Fortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortroligUtland
import no.nav.helse.modell.person.Adressebeskyttelse.Ugradert
import no.nav.helse.modell.person.Adressebeskyttelse.Ukjent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgPersonApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `skal oppdage at en person har fortrolig adresse`() {
        opprettPerson(adressebeskyttelse = Fortrolig)
        assertTrue(personApiDao.personHarAdressebeskyttelse(FNR, Fortrolig.somApiType()))
    }

    @Test
    fun `person med ugradert adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Ugradert)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FNR, Fortrolig.somApiType()))
    }

    @Test
    fun `person med strengt fortrolig adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = StrengtFortrolig)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FNR, Fortrolig.somApiType()))
    }

    @Test
    fun `person med strengt fortrolig utland adresse er ikke kode 7`() {
        opprettPerson(adressebeskyttelse = StrengtFortroligUtland)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FNR, Fortrolig.somApiType()))
    }

    @Test
    fun `person med ukjent adressebeskyttelse er (kanskje) ikke kode 7`() {
        opprettPerson(adressebeskyttelse = Ukjent)
        assertFalse(personApiDao.personHarAdressebeskyttelse(FNR, Fortrolig.somApiType()))
    }

    @Test
    fun `kan svare på om en person er klar for visning`() {
        opprettMinimalPerson()
        assertPersonenErIkkeKlar()

        oppdaterEnhet(enhetNr = 101)
        assertPersonenErIkkeKlar()

        opprettEgenAnsatt(FNR, false)
        assertPersonenErIkkeKlar()

        oppdaterAdressebeskyttelse(Ugradert)
        assertPersonenErKlar()
    }

    // Denne testen komplementerer den ovenstående, for å vise at både personinfo og info om egen ansatt må finnes
    @Test
    fun `kan svare på om en person er klar for visning, med dataene mottatt i ikke-realistisk rekkefølge`() {
        opprettMinimalPerson()
        assertPersonenErIkkeKlar()

        oppdaterAdressebeskyttelse(Ugradert)
        assertPersonenErIkkeKlar()

        opprettEgenAnsatt(FNR, false)
        assertPersonenErIkkeKlar()

        oppdaterEnhet(enhetNr = 101)

        assertPersonenErKlar()
    }

    @Test
    fun `kan markere at en person er under klargjøring`() {
        assertFalse(personApiDao.klargjøringPågår(FNR))
        personApiDao.personKlargjøres(FNR)
        assertTrue(personApiDao.klargjøringPågår(FNR))
    }

    private fun harDataNødvendigForVisning() = personApiDao.harDataNødvendigForVisning(FNR)
    private fun assertPersonenErIkkeKlar() = assertFalse(harDataNødvendigForVisning())
    private fun assertPersonenErKlar() = assertTrue(harDataNødvendigForVisning())
}

private fun Adressebeskyttelse.somApiType(): no.nav.helse.spesialist.api.person.Adressebeskyttelse {
    return no.nav.helse.spesialist.api.person.Adressebeskyttelse.valueOf(this.name)
}
