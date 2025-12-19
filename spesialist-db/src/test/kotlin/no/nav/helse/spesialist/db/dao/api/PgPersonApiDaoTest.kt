package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPersoninfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

internal class PgPersonApiDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `kan svare på om en person er klar for visning`() {
        val person = opprettPerson(person = lagPerson(fødselsdato = null, kjønn = null, info = null, erEgenAnsatt = null))
        assertPersonenErIkkeKlar(person.id)

        person.oppdaterEnhet(101)
        sessionContext.personRepository.lagre(person)
        assertPersonenErIkkeKlar(person.id)

        val oppdatertMedEnhet = sessionContext.personRepository.finn(person.id) ?: error("Fant ikke person")
        oppdatertMedEnhet.oppdaterEgenAnsattStatus(false, Instant.now())
        sessionContext.personRepository.lagre(oppdatertMedEnhet)
        assertPersonenErIkkeKlar(oppdatertMedEnhet.id)

        val oppdatertMedEgenAnsattStatus = sessionContext.personRepository.finn(person.id) ?: error("Fant ikke person")
        oppdatertMedEgenAnsattStatus.oppdaterInfo(lagPersoninfo())
        sessionContext.personRepository.lagre(oppdatertMedEgenAnsattStatus)
        assertPersonenErKlar(oppdatertMedEgenAnsattStatus.id)
    }

    // Denne testen komplementerer den ovenstående, for å vise at både personinfo og info om egen ansatt må finnes
    @Test
    fun `kan svare på om en person er klar for visning, med dataene mottatt i ikke-realistisk rekkefølge`() {
        val person = opprettPerson(person = lagPerson(fødselsdato = null, kjønn = null, info = null, erEgenAnsatt = null, enhet = null))
        assertPersonenErIkkeKlar(person.id)

        person.oppdaterInfo(lagPersoninfo())
        sessionContext.personRepository.lagre(person)
        assertPersonenErIkkeKlar(person.id)

        val oppdatertMedPersoninfo = sessionContext.personRepository.finn(person.id) ?: error("Fant ikke person")
        oppdatertMedPersoninfo.oppdaterEgenAnsattStatus(false, Instant.now())
        sessionContext.personRepository.lagre(oppdatertMedPersoninfo)
        assertPersonenErIkkeKlar(oppdatertMedPersoninfo.id)

        val oppdatertMedEgenAnsattStatus = sessionContext.personRepository.finn(person.id) ?: error("Fant ikke person")
        oppdatertMedEgenAnsattStatus.oppdaterEnhet(101)
        sessionContext.personRepository.lagre(oppdatertMedEgenAnsattStatus)

        assertPersonenErKlar(oppdatertMedEgenAnsattStatus.id)
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
