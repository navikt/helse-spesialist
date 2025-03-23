package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDateTime
import java.util.UUID

class ÅpneOppgaverbehovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("ÅpneOppgaver"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "behov",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "aktørId" to testPerson.aktørId,
                "fødselsnummer" to testPerson.fødselsnummer,
                "@final" to true,
                "@behov" to listOf("ÅpneOppgaver"),
                "contextId" to json["contextId"].asText(),
                "hendelseId" to json["hendelseId"].asText(),
                "@løsning" to mapOf(
                    "ÅpneOppgaver" to mapOf(
                        "antall" to 0,
                        "oppslagFeilet" to false
                    )
                )
            )
        ).toJson()
}
