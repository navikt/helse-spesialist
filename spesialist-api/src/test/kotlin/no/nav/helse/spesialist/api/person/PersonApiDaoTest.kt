package no.nav.helse.spesialist.api.person

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.person.Adressebeskyttelse.Ugradert
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PersonApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `skal oppdage at en person har fortrolig adresse`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        assertTrue(personApiDao.personHarAdressebeskyttelse(FØDSELSNUMMER, Adressebeskyttelse.Fortrolig))
    }

    @Test
    fun `person skal holdes igjen`() {
        val fødselsnummer = lagFødselsnummer()
        opprettPersonSomSkalHoldesIgjen(fødselsnummer)
        assertTrue(personApiDao.skalHoldesIgjen(fødselsnummer, UUID.randomUUID()))
    }

    @Test
    fun `person skal ikke holdes igjen hvis saksbehandler har fått tilgang eksplisitt`() {
        val fødselsnummer = lagFødselsnummer()
        opprettPersonSomSkalHoldesIgjen(fødselsnummer)
        val saksbehandlerOid = UUID.randomUUID()
        giSaksbehandlerTilgangTilPersonSomHoldesIgjen(fødselsnummer, saksbehandlerOid)
        assertFalse(personApiDao.skalHoldesIgjen(fødselsnummer, saksbehandlerOid))
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

    private fun opprettPersonSomSkalHoldesIgjen(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = """INSERT INTO person_som_skal_holdes_igjen(fodselsnummer, opprettet) VALUES (:fodselsnummer, :opprettet)"""
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer, "opprettet" to LocalDateTime.now())).asExecute)
        }
    }

    private fun giSaksbehandlerTilgangTilPersonSomHoldesIgjen(fødselsnummer: String, saksbehandlerOid: UUID) {
        @Language("PostgreSQL")
        val query = """UPDATE person_som_skal_holdes_igjen SET oider_som_kan_sla_opp = ARRAY[:oid] WHERE fodselsnummer = :fodselsnummer"""
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer, "oid" to saksbehandlerOid)).asExecute)
        }
    }

    private fun harDataNødvendigForVisning() = personApiDao.harDataNødvendigForVisning(FØDSELSNUMMER)
    private fun assertPersonenErIkkeKlar() = assertFalse(harDataNødvendigForVisning())
    private fun assertPersonenErKlar() = assertTrue(harDataNødvendigForVisning())
}
