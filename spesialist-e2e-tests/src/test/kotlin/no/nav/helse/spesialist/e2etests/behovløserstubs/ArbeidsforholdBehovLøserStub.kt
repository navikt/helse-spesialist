package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

class ArbeidsforholdBehovLøserStub : AbstractBehovLøserStub("Arbeidsforhold") {
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
