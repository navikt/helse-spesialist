package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.TestPerson
import no.nav.helse.spesialist.typer.Kjønn

class HentPersoninfoV2BehovMockRiver(private val testPerson: TestPerson) : AbstractBehovMockRiver("HentPersoninfoV2") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "HentPersoninfoV2" to mapOf(
            "fornavn" to testPerson.fornavn,
            "mellomnavn" to testPerson.mellomnavn,
            "etternavn" to testPerson.etternavn,
            "fødselsdato" to testPerson.fødselsdato,
            "kjønn" to when (testPerson.kjønn) {
                Kjønn.Mann -> "Mann"
                Kjønn.Kvinne -> "Kvinne"
                Kjønn.Ukjent -> "Ukjent"
            },
            "adressebeskyttelse" to "Ugradert"
        )
    )
}
