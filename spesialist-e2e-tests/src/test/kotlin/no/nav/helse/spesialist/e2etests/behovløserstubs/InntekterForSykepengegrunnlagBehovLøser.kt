package no.nav.helse.spesialist.e2etests.behovløserstubs

import tools.jackson.databind.JsonNode
import java.time.YearMonth

class InntekterForSykepengegrunnlagBehovLøser(
    private val organisasjonsnumre: List<String>,
) : AbstractBehovLøser("InntekterForSykepengegrunnlag") {
    override fun løsning(behovJson: JsonNode) =
        listOf(
            mapOf(
                "årMåned" to "${YearMonth.now().minusMonths(1)}",
                "inntektsliste" to
                    organisasjonsnumre.map { organisasjonsnummer ->
                        mapOf(
                            "beløp" to 20000,
                            "inntektstype" to "LOENNSINNTEKT",
                            "orgnummer" to organisasjonsnummer,
                        )
                    },
            ),
        )
}
