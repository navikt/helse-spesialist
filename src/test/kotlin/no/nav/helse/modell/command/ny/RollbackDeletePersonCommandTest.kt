package no.nav.helse.modell.command.ny

import AbstractEndToEndTest
import kotliquery.sessionOf
import no.nav.helse.api.RollbackDelete
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RollbackDeletePersonCommandTest: AbstractEndToEndTest() {
    @Test
    fun `rollback delete person command legger rollback delete melding på rapid`() {
        val rollbackDelete = RollbackDelete(
            fødselsnummer = "fødselsnummer",
            aktørId = "aktørid"
        )
        val rollbackPersonCommand = RollbackDeletePersonCommand(testRapid, rollbackDelete)
        sessionOf(dataSource).use(rollbackPersonCommand::execute)

        val rollbackMelding = testRapid.inspektør.message(0)

        assertEquals("rollback_person_delete", rollbackMelding["@event_name"].asText())
        assertEquals(rollbackDelete.fødselsnummer, rollbackMelding["fødselsnummer"].asText())
    }
}
