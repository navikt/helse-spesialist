package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.test.TestPerson
import java.time.YearMonth

class InntekterForSykepengegrunnlagBehovMockRiver(private val testPerson: TestPerson) :
    AbstractBehovMockRiver("InntekterForSykepengegrunnlag") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
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
}
