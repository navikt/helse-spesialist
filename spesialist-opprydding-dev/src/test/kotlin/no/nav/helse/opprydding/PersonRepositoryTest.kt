package no.nav.helse.opprydding

import no.nav.helse.opprydding.Comparison.AT_LEAST
import no.nav.helse.opprydding.Comparison.EXACTLY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonRepositoryTest : AbstractDatabaseTest() {
    @Test
    fun `sletting av person`() {
        opprettPerson(FØDSELSNUMMER)
        assertTabellinnhold(AT_LEAST, 1)
        personRepository.slett(FØDSELSNUMMER)
        assertTabellinnhold(EXACTLY, 0)
    }

    @Test
    fun `antall tabeller - du må antakelig rette opp i sletting i dev dersom du har lagt til eller fjernet tabeller`() {
        assertEquals(75, finnTabeller().size)
    }
}
