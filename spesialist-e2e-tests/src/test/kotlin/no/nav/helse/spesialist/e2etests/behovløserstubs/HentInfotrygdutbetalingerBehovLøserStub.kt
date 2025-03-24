package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class HentInfotrygdutbetalingerBehovLøserStub(private val organisasjonsnummer: String) :
    AbstractBehovLøserStub("HentInfotrygdutbetalinger") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "HentInfotrygdutbetalinger" to listOf(
            mapOf(
                "fom" to "2018-01-01",
                "tom" to "2018-01-31",
                "dagsats" to "1000.0",
                "grad" to "100",
                "typetekst" to "ArbRef",
                "organisasjonsnummer" to organisasjonsnummer
            )
        )
    )
}
