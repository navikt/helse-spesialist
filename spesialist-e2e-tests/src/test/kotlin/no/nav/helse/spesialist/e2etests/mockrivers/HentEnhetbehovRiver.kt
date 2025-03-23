package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDateTime
import java.util.UUID

class HentEnhetbehovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("HentEnhet"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "behov",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "@final" to true,
                "@behov" to listOf("HentEnhet"),
                "hendelseId" to json["hendelseId"].asText(),
                "contextId" to json["contextId"].asText(),
                "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                "fødselsnummer" to testPerson.fødselsnummer,
                "aktørId" to testPerson.aktørId,
                "orgnummer" to testPerson.orgnummer,
                "@løsning" to mapOf(
                    "HentEnhet" to "0301"
                )
            )
        ).toJson()
}
