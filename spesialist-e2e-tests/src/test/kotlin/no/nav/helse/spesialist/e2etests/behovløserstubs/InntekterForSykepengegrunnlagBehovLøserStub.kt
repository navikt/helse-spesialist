package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import java.time.YearMonth

class InntekterForSykepengegrunnlagBehovLøserStub(private val organisasjonsnummer: String) :
    AbstractBehovLøserStub("InntekterForSykepengegrunnlag") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "InntekterForSykepengegrunnlag" to listOf(
            mapOf(
                "årMåned" to "${YearMonth.now().minusMonths(1)}",
                "inntektsliste" to listOf(
                    mapOf(
                        "beløp" to 20000,
                        "inntektstype" to "LOENNSINNTEKT",
                        "orgnummer" to organisasjonsnummer
                    )
                )
            )
        )
    )
}
