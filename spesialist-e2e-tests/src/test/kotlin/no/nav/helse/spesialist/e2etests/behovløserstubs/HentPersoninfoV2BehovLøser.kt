package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.typer.Kjønn

class HentPersoninfoV2BehovLøser(private val testPerson: TestPerson) : AbstractBehovLøser("HentPersoninfoV2") {
    override fun løsning(behovJson: JsonNode) = mapOf(
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
}
