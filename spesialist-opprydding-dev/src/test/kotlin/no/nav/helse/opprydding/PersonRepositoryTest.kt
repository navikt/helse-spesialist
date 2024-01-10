package no.nav.helse.opprydding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest: AbstractDatabaseTest() {

    @Test
    fun `sletting av person`() {
        opprettPerson(FØDSELSNUMMER)
        assertTabellinnhold { (it >= 1) to ">= 1" }
        personRepository.slett(FØDSELSNUMMER)
        assertTabellinnhold { (it == 0) to "0" }
    }

    @Test
    fun `antall tabeller - du må antakelig rette opp i sletting i dev dersom du har lagt til eller fjernet tabeller`() {
        assertEquals(73, finnTabeller().size)
    }
}
