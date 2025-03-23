package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.typer.Kjønn

class ArbeidsgiverinformasjonOgHentPersoninfoV2BehovMockRiver(private val testPerson: TestPerson) :
    AbstractBehovMockRiver("Arbeidsgiverinformasjon", "HentPersoninfoV2") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Arbeidsgiverinformasjon" to json["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
            mapOf(
                "orgnummer" to it.asText(),
                "navn" to "Navn for ${it.asText()}",
                "bransjer" to listOf("Bransje for ${it.asText()}")
            )
        },
        "HentPersoninfoV2" to json["HentPersoninfoV2"]["ident"].map {
            mapOf(
                "ident" to it.asText(),
                "fornavn" to testPerson.fornavn,
                "etternavn" to testPerson.etternavn,
                "fødselsdato" to testPerson.fødselsdato,
                "kjønn" to when (testPerson.kjønn) {
                    Kjønn.Mann -> "Mann"
                    Kjønn.Kvinne -> "Kvinne"
                    Kjønn.Ukjent -> "Ukjent"
                },
                "adressebeskyttelse" to "Ugradert"
            )
        }
    )
}
