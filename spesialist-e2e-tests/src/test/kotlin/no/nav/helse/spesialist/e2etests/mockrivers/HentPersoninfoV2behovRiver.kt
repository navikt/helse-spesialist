package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDateTime
import java.util.UUID

class HentPersoninfoV2behovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("HentPersoninfoV2"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "behov",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "@final" to true,
            "@behov" to listOf("HentPersoninfoV2"),
            "hendelseId" to json["hendelseId"].asText(),
            "contextId" to json["contextId"].asText(),
            "fødselsnummer" to json["fødselsnummer"].asText(),
            "aktørId" to testPerson.aktørId,
            "@løsning" to mapOf(
                "HentPersoninfoV2" to mapOf(
                    "fornavn" to testPerson.fornavn,
                    "mellomnavn" to testPerson.mellomnavn,
                    "etternavn" to testPerson.etternavn,
                    "fødselsdato" to testPerson.fødselsdato,
                    "kjønn" to when (testPerson.kjønn) {
                        Kjønn.Mann -> "Mann"
                        Kjønn.Kvinne -> "Kvinne"
                        Kjønn.Ukjent -> "Ukjent"
                    },
                    "adressebeskyttelse" to "Ugradert"
                )
            )
        )
    ).toJson()
}
