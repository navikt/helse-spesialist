package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import java.time.YearMonth

class InntekterForSykepengegrunnlagBehovLøser(
    private val organisasjonsnummer: String,
) : AbstractBehovLøser("InntekterForSykepengegrunnlag") {
    override fun løsning(behovJson: JsonNode) =
        listOf(
            mapOf(
                "årMåned" to "${YearMonth.now().minusMonths(1)}",
                "inntektsliste" to
                    listOf(
                        mapOf(
                            "beløp" to 20000,
                            "inntektstype" to "LOENNSINNTEKT",
                            "orgnummer" to organisasjonsnummer,
                        ),
                    ),
            ),
        )
}
