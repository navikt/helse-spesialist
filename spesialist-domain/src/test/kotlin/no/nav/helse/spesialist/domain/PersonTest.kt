package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPersoninfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PersonTest {
    @Test
    fun `kan svare på om en person er klar for visning`() {
        val person = lagPerson(fødselsdato = null, kjønn = null, info = null, erEgenAnsatt = null)
        person.assertErIkkeKlar()

        person.oppdaterEnhet(101)
        person.assertErIkkeKlar()

        person.oppdaterEgenAnsattStatus(false, Instant.now())
        person.assertErIkkeKlar()

        person.oppdaterInfo(lagPersoninfo())
        person.assertErKlar()
    }

    // Denne testen komplementerer den ovenstående, for å vise at både personinfo og info om egen ansatt må finnes
    @Test
    fun `kan svare på om en person er klar for visning, med dataene mottatt i ikke-realistisk rekkefølge`() {
        val person = lagPerson(fødselsdato = null, kjønn = null, info = null, erEgenAnsatt = null, enhet = null)
        person.assertErIkkeKlar()

        person.oppdaterInfo(lagPersoninfo())
        person.assertErIkkeKlar()

        person.oppdaterEgenAnsattStatus(false, Instant.now())
        person.assertErIkkeKlar()

        person.oppdaterEnhet(101)
        person.assertErKlar()
    }

    private fun Person.assertErIkkeKlar() = assertFalse(harDataNødvendigForVisning())

    private fun Person.assertErKlar() = assertTrue(harDataNødvendigForVisning())
}
