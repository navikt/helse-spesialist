package no.nav.helse.modell.command.ny

import kotliquery.sessionOf
import no.nav.helse.api.Rollback
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RollbackPersonCommandTest {
    @Test
    fun `rollback person command legger rollback melding på rapid`() {
        val dataSource = setupDataSourceMedFlyway()
        val testRapid = TestRapid()
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
