package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.Adressebeskyttelse.Fortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortroligUtland
import no.nav.helse.modell.person.Adressebeskyttelse.Ugradert
import no.nav.helse.modell.person.Adressebeskyttelse.Ukjent
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgPersonApiDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `henter adressebeskyttelse Fortrolig`() {
        opprettPerson(adressebeskyttelse = Fortrolig)
        assertEquals(Fortrolig.somApiType(), personApiDao.hentAdressebeskyttelse(FNR))
    }

    @Test
    fun `henter adressebeskyttelse Ugradert`() {
        opprettPerson(adressebeskyttelse = Ugradert)
        assertEquals(Ugradert.somApiType(), personApiDao.hentAdressebeskyttelse(FNR))
    }

    @Test
    fun `henter adressebeskyttelse Strengt Fortrolig`() {
        opprettPerson(adressebeskyttelse = StrengtFortrolig)
        assertEquals(StrengtFortrolig.somApiType(), personApiDao.hentAdressebeskyttelse(FNR))
    }

    @Test
    fun `henter adressebeskyttelse Strengt Fortrolig Utland`() {
        opprettPerson(adressebeskyttelse = StrengtFortroligUtland)
        assertEquals(StrengtFortroligUtland.somApiType(), personApiDao.hentAdressebeskyttelse(FNR))
    }

    @Test
    fun `henter adressebeskyttelse Ukjent`() {
        opprettPerson(adressebeskyttelse = Ukjent)
        assertEquals(Ukjent.somApiType(), personApiDao.hentAdressebeskyttelse(FNR))
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
