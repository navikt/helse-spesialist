package no.nav.helse.opprydding

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest: AbstractDatabaseTest() {
    private lateinit var testRapid: TestRapid

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        SlettPersonRiver(testRapid, personRepository)
    }

    @Test
    fun `slettemelding medfører at person slettes fra databasen`() {
        opprettPerson("123")
        assertTabellinnhold { (it >= 1) to ">= 1" }
        testRapid.sendTestMessage(slettemelding("123"))
        assertTabellinnhold { (it == 0) to "0" }
    }

    @Test
    fun `sletter kun aktuelt fnr`() {
        opprettPerson("123")
        opprettPerson("1234", sequenceNumber = 2)
        assertTabellinnhold { (it >= 2) to ">= 2" }
        testRapid.sendTestMessage(slettemelding("123"))
        assertTabellinnhold { (it == 1) to "1" }
    }

    @Language("JSON")
    private fun slettemelding(fødselsnummer: String) = """
        {
          "@event_name": "slett_person",
          "@id": "${UUID.randomUUID()}",
          "opprettet": "${LocalDateTime.now()}",
          "fødselsnummer": "$fødselsnummer"
        }
    """.trimIndent()
}