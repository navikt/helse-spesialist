package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

class ArbeidsforholdBehovLøser : AbstractBehovLøser("Arbeidsforhold") {
    override fun løsning(behovJson: JsonNode) =
        listOf(
            mapOf(
                "stillingstittel" to "en-stillingstittel",
                "stillingsprosent" to 100,
                "startdato" to LocalDate.now(),
                "sluttdato" to null,
            ),
        )
}
