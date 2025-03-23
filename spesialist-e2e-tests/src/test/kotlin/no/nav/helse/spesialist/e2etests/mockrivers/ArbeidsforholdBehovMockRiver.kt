package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

class ArbeidsforholdBehovMockRiver : AbstractBehovMockRiver("Arbeidsforhold") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Arbeidsforhold" to listOf(
            mapOf(
                "stillingstittel" to "en-stillingstittel",
                "stillingsprosent" to 100,
                "startdato" to LocalDate.now(),
                "sluttdato" to null,
            )
        )
    )
}
