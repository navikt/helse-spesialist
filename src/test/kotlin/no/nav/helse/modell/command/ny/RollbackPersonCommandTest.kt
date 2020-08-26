package no.nav.helse.modell.command.ny

import AbstractEndToEndTest
import kotliquery.sessionOf
import no.nav.helse.api.Rollback
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RollbackPersonCommandTest: AbstractEndToEndTest(){
    @Test
    fun `rollback person command legger rollback melding på rapid`() {
        val rollback = Rollback(
            fødselsnummer = "fødselsnummer",
            aktørId = "aktørid",
            personVersjon = 1L
        )
        val rollbackPersonCommand = RollbackPersonCommand(testRapid, rollback)
        sessionOf(dataSource).use(rollbackPersonCommand::execute)

        val rollbackMelding = testRapid.inspektør.message(0)

        assertEquals("rollback_person", rollbackMelding["@event_name"].asText())
        assertEquals(rollback.fødselsnummer, rollbackMelding["fødselsnummer"].asText())
    }
}
