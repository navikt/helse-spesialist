package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class InntekterForSykepengegrunnlagbehovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("InntekterForSykepengegrunnlag"))
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
                "@behov" to listOf("InntekterForSykepengegrunnlag"),
                "contextId" to json["contextId"].asText(),
                "hendelseId" to json["hendelseId"].asText(),
                "orgnummer" to testPerson.orgnummer,
                "@løsning" to mapOf(
                    "InntekterForSykepengegrunnlag" to listOf(
                        mapOf(
                            "årMåned" to "${YearMonth.now().minusMonths(1)}",
                            "inntektsliste" to listOf(
                                mapOf(
                                    "beløp" to 20000,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to testPerson.orgnummer
                                )
                            )
                        )
                    )
                )
            )
        ).toJson()
}
