package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.opprydding.Comparison.AT_LEAST
import no.nav.helse.opprydding.Comparison.EXACTLY
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest: AbstractDatabaseTest() {
    private lateinit var testRapid: TestRapid
    private val commandContextDao = CommandContextDao(dataSource)

    @BeforeEach
    fun beforeEach() {
        testRapid = TestRapid()
        SlettPersonRiver(testRapid, personRepository, commandContextDao)
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

    @Test
    fun `sender ut avbryt-melding for aktive kommandokjeder`() {
        opprettPerson("123")
        assertTabellinnhold(AT_LEAST, 1)
        testRapid.sendTestMessage(slettemelding("123"))
        val sendtMelding = testRapid.inspektør.message(0)
        assertEquals("kommandokjede_avbrutt", sendtMelding["@event_name"].asText())
    }

    @Test
    fun `sender kvittering etter sletting`() {
        opprettPerson("123")
        assertTabellinnhold(AT_LEAST, 1)
        testRapid.sendTestMessage(slettemelding("123"))
        testRapid.inspektør.message(1).run {
            assertEquals("person_slettet", path("@event_name").asText())
            assertEquals("123", path("fødselsnummer").asText())
        }
    }

    @Language("JSON")
    private fun slettemelding(fødselsnummer: String) = """
        {
          "@event_name": "slett_person",
          "@id": "${UUID.randomUUID()}",
          "fødselsnummer": "$fødselsnummer"
        }
    """.trimIndent()
}
