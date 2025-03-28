package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AvviksvurderingBehovLøser : AbstractBehovLøser("Avviksvurdering") {
    override fun løsning(behovJson: JsonNode) = mapOf(
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
                    "arbeidsgiverreferanse" to behovJson["organisasjonsnummer"].asText(),
                    "beløp" to 600000.0
                )
            )
        ),
        "sammenligningsgrunnlag" to mapOf(
            "totalbeløp" to 650_000.0,
            "innrapporterteInntekter" to listOf(
                mapOf(
                    "arbeidsgiverreferanse" to behovJson["organisasjonsnummer"].asText(),
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
}
