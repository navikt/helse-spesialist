package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class HentInfotrygdutbetalingerBehovLøser(
    organisasjonsnummer: String,
) : AbstractBehovLøser("HentInfotrygdutbetalinger") {
    var historikk: List<Map<String, Any?>> =
        listOf(
            mapOf(
                "fom" to "2018-01-01",
                "tom" to "2018-01-31",
                "dagsats" to 1000.0,
                "grad" to "100",
                "typetekst" to "ArbRef",
                "organisasjonsnummer" to organisasjonsnummer,
            ),
        )

    override fun løsning(behovJson: JsonNode) = historikk
}
