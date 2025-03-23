package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AvviksvurderingbehovRiver : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("Avviksvurdering"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "behov",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "@final" to true,
            "@behov" to listOf("Avviksvurdering"),
            "hendelseId" to json["hendelseId"].asText(),
            "contextId" to json["contextId"].asText(),
            "fødselsnummer" to json["fødselsnummer"].asText(),
            "@løsning" to mapOf(
                "Avviksvurdering" to mapOf(
                    "avviksvurderingId" to UUID.randomUUID(),
                    "utfall" to "NyVurderingForetatt",
                    "avviksprosent" to 10.0,
                    "harAkseptabeltAvvik" to true,
                    "maksimaltTillattAvvik" to 25.0,
                    "opprettet" to LocalDateTime.now(),
                    "beregningsgrunnlag" to mapOf(
                        "totalbeløp" to 600000.0,
                        "omregnedeÅrsinntekter" to listOf(
                            mapOf(
                                "arbeidsgiverreferanse" to json["Avviksvurdering"]["organisasjonsnummer"].asText(),
                                "beløp" to 600000.0
                            )
                        )
                    ),
                    "sammenligningsgrunnlag" to mapOf(
                        "totalbeløp" to 650_000.0,
                        "innrapporterteInntekter" to listOf(
                            mapOf(
                                "arbeidsgiverreferanse" to json["Avviksvurdering"]["organisasjonsnummer"].asText(),
                                "inntekter" to listOf(
                                    mapOf<String, Any>(
                                        "årMåned" to YearMonth.now(),
                                        "beløp" to 650_000.0
                                    )
                                )
                            )
                        )
                    ),
                )
            )
        )
    ).toJson()
}
