package no.nav.helse.opprydding

import no.nav.helse.opprydding.Comparison.AT_LEAST
import no.nav.helse.opprydding.Comparison.EXACTLY
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.UUID

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
        assertTabellinnhold(AT_LEAST, 1)
        testRapid.sendTestMessage(slettemelding("123"))
        assertTabellinnhold(EXACTLY, 0)
    }

    @Test
    fun `sletter kun aktuelt fnr`() {
        opprettPerson("123")
        opprettPerson("1234", sequenceNumber = 2)
        assertTabellinnhold(AT_LEAST, 2)
        testRapid.sendTestMessage(slettemelding("123"))
        assertTabellinnhold(EXACTLY, 1)
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
