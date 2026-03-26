package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

import java.time.LocalDate

class FullmaktBehovLøser : AbstractBehovLøser("Fullmakt") {
    var fullmakter: List<Map<String, Any?>> = emptyList()

    override fun løsning(behovJson: JsonNode) = fullmakter

    companion object {
        fun gyldigFullmakt(
            gyldigFraOgMed: LocalDate = LocalDate.now(),
            gyldigTilOgMed: LocalDate = LocalDate.now().plusYears(1),
        ) = mapOf(
            "gyldigFraOgMed" to gyldigFraOgMed.toString(),
            "gyldigTilOgMed" to gyldigTilOgMed.toString(),
        )
    }
}
