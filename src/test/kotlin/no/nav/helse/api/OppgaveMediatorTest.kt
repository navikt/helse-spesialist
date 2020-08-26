package no.nav.helse.api

import AbstractEndToEndTest
import no.nav.helse.TestPerson
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class OppgaveMediatorTest : AbstractEndToEndTest() {

    private lateinit var mediator:OppgaveMediator

    @BeforeAll
    fun setup() {
        mediator = OppgaveMediator(dataSource)
    }

    @Test
    fun `henter oppgave med fødselsnummer`() {
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.sendGodkjenningMessage(eventId = eventId)
        person.sendPersoninfo(eventId = eventId)

        val oppgave = mediator.hentOppgave(person.fødselsnummer)

        assertEquals(eventId, oppgave?.eventId)
    }

    @Test
    fun `henter kun saksbehandleroppgaver`() {
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)
        person.sendGodkjenningMessage(eventId = eventId)

        val oppgave = mediator.hentOppgave(person.fødselsnummer)

        assertNull(oppgave)
    }
}
