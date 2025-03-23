package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDateTime
import java.util.UUID

class VergemålOgFullmaktbehovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("Vergemål", "Fullmakt"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "behov",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "@final" to true,
                "@behov" to listOf("Vergemål", "Fullmakt"),
                "hendelseId" to json["hendelseId"].asText(),
                "contextId" to json["contextId"].asText(),
                "fødselsnummer" to testPerson.fødselsnummer,
                "aktørId" to testPerson.aktørId,
                "@løsning" to mapOf(
                    "Vergemål" to mapOf(
                        "vergemål" to emptyList<Any>(),
                        "fremtidsfullmakter" to emptyList(),
                        "fullmakter" to emptyList(),
                    ),
                    "Fullmakt" to emptyList<Any>()
                )
            )
        ).toJson()
}
