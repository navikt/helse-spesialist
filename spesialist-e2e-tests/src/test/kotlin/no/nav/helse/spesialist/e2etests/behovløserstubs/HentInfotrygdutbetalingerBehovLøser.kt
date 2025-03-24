package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class HentInfotrygdutbetalingerBehovLøser(private val organisasjonsnummer: String) :
    AbstractBehovLøser("HentInfotrygdutbetalinger") {
    override fun løsning(behovJson: JsonNode) = listOf(
        mapOf(
            "fom" to "2018-01-01",
            "tom" to "2018-01-31",
            "dagsats" to "1000.0",
            "grad" to "100",
            "typetekst" to "ArbRef",
            "organisasjonsnummer" to organisasjonsnummer
        )
    )
}
