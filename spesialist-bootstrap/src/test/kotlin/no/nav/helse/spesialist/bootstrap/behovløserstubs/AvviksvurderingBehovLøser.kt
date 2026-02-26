package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class AvviksvurderingBehovLøser : AbstractBehovLøser("Avviksvurdering") {
    override fun løsning(behovJson: JsonNode) =
        mapOf(
            "avviksvurderingId" to UUID.randomUUID(),
            "utfall" to "NyVurderingForetatt",
            "avviksprosent" to AVVIKSPROSENT,
            "harAkseptabeltAvvik" to true,
            "maksimaltTillattAvvik" to 25.0,
            "opprettet" to LocalDateTime.now(),
            "beregningsgrunnlag" to
                mapOf(
                    "totalbeløp" to 600000.0,
                    "omregnedeÅrsinntekter" to
                        listOf(
                            mapOf(
                                "arbeidsgiverreferanse" to behovJson["organisasjonsnummer"].asText(),
                                "beløp" to 600000.0,
                            ),
                        ),
                ),
            "sammenligningsgrunnlag" to
                mapOf(
                    "totalbeløp" to SAMMENLIGNINGSGRUNNLAG_TOTALBELØP,
                    "innrapporterteInntekter" to
                        listOf(
                            mapOf(
                                "arbeidsgiverreferanse" to behovJson["organisasjonsnummer"].asText(),
                                "inntekter" to
                                    listOf(
                                        mapOf<String, Any>(
                                            "årMåned" to YearMonth.now(),
                                            "beløp" to SAMMENLIGNINGSGRUNNLAG_TOTALBELØP,
                                        ),
                                    ),
                            ),
                        ),
                ),
        )

    companion object {
        const val AVVIKSPROSENT = 10.0
        const val SAMMENLIGNINGSGRUNNLAG_TOTALBELØP = 650_000.0
    }
}
