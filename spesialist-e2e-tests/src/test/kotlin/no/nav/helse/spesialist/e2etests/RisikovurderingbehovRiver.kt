package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDateTime
import java.util.UUID

class RisikovurderingbehovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("Risikovurdering"))
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
                "@behov" to listOf("Risikovurdering"),
                "contextId" to json["contextId"].asText(),
                "hendelseId" to json["hendelseId"].asText(),
                "Risikovurdering" to mapOf(
                    "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "førstegangsbehandling" to "FORLENGELSE",
                    "kunRefusjon" to true,
                    "inntekt" to mapOf(
                        "omregnetÅrsinntekt" to "123456.7",
                        "inntektskilde" to "Arbeidsgiver"
                    )
                ),
                "@løsning" to mapOf(
                    "Risikovurdering" to mapOf(
                        "kanGodkjennesAutomatisk" to false,
                        "funn" to emptyList<Any>(),
                        "kontrollertOk" to emptyList<Any>(),
                    )
                )
            )
        ).toJson()
}
