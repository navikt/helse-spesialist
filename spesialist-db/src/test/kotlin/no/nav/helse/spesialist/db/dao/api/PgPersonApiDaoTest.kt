package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.Adressebeskyttelse.Fortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortrolig
import no.nav.helse.modell.person.Adressebeskyttelse.StrengtFortroligUtland
import no.nav.helse.modell.person.Adressebeskyttelse.Ugradert
import no.nav.helse.modell.person.Adressebeskyttelse.Ukjent
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPersoninfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgPersonApiDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `henter adressebeskyttelse Fortrolig`() {
        val fødselsnummer = opprettPerson(person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig)).id.value
        assertEquals(Fortrolig.somApiType(), personApiDao.hentAdressebeskyttelse(fødselsnummer))
    }

    @Test
    fun `henter adressebeskyttelse Ugradert`() {
        val fødselsnummer = opprettPerson(person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert)).id.value
        assertEquals(Ugradert.somApiType(), personApiDao.hentAdressebeskyttelse(fødselsnummer))
    }

    @Test
    fun `henter adressebeskyttelse Strengt Fortrolig`() {
        val fødselsnummer = opprettPerson(person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)).id.value
        assertEquals(StrengtFortrolig.somApiType(), personApiDao.hentAdressebeskyttelse(fødselsnummer))
    }

    @Test
    fun `henter adressebeskyttelse Strengt Fortrolig Utland`() {
        val fødselsnummer = opprettPerson(person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortroligUtland)).id.value
        assertEquals(StrengtFortroligUtland.somApiType(), personApiDao.hentAdressebeskyttelse(fødselsnummer))
    }

    @Test
    fun `henter adressebeskyttelse Ukjent`() {
        val fødselsnummer = opprettPerson(person = lagPerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ukjent)).id.value
        assertEquals(Ukjent.somApiType(), personApiDao.hentAdressebeskyttelse(fødselsnummer))
    }

    @Test
    fun `kan svare på om en person er klar for visning`() {
        val person = opprettPerson(person = lagPerson(fødselsdato = null, kjønn = null, info = null, erEgenAnsatt = null))
        assertPersonenErIkkeKlar(person.id)

        person.oppdaterEnhet(101)
        sessionContext.personRepository.lagre(person)
        assertPersonenErIkkeKlar(person.id)

        opprettEgenAnsatt(person.id.value, false)
        assertPersonenErIkkeKlar(person.id)

        person.oppdaterInfo(lagPersoninfo(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert))
        sessionContext.personRepository.lagre(person)
        assertPersonenErKlar(person.id)
    }

    // Denne testen komplementerer den ovenstående, for å vise at både personinfo og info om egen ansatt må finnes
    @Test
    fun `kan svare på om en person er klar for visning, med dataene mottatt i ikke-realistisk rekkefølge`() {
        val person = opprettPerson(person = lagPerson(fødselsdato = null, kjønn = null, info = null, erEgenAnsatt = null, enhet = null))
        assertPersonenErIkkeKlar(person.id)

        person.oppdaterInfo(lagPersoninfo(adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert))
        sessionContext.personRepository.lagre(person)
        assertPersonenErIkkeKlar(person.id)

        opprettEgenAnsatt(person.id.value, false)
        assertPersonenErIkkeKlar(person.id)

        person.oppdaterEnhet(101)
        sessionContext.personRepository.lagre(person)

        assertPersonenErKlar(person.id)
    }

    @Test
    fun `kan markere at en person er under klargjøring`() {
        val person = opprettPerson()
        assertFalse(personApiDao.klargjøringPågår(person.id.value))
        personApiDao.personKlargjøres(person.id.value)
        assertTrue(personApiDao.klargjøringPågår(person.id.value))
    }

    private fun harDataNødvendigForVisning(identitetsnummer: Identitetsnummer) = personApiDao.harDataNødvendigForVisning(identitetsnummer.value)

    private fun assertPersonenErIkkeKlar(identitetsnummer: Identitetsnummer) = assertFalse(harDataNødvendigForVisning(identitetsnummer))

    private fun assertPersonenErKlar(identitetsnummer: Identitetsnummer) = assertTrue(harDataNødvendigForVisning(identitetsnummer))
}

private fun Adressebeskyttelse.somApiType(): no.nav.helse.spesialist.api.person.Adressebeskyttelse =
    no.nav.helse.spesialist.api.person.Adressebeskyttelse
        .valueOf(this.name)
