package no.nav.helse.api

import no.nav.helse.TestPerson
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class OppgaveMediatorTest {

    val dataSource = setupDataSourceMedFlyway()
    val mediator = OppgaveMediator(dataSource)

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
