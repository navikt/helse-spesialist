package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.typer.Kjønn

class HentPersoninfoV2BehovLøser(private val person: Person) : AbstractBehovLøser("HentPersoninfoV2") {
    var adressebeskyttelse = "Ugradert"

    override fun løsning(behovJson: JsonNode): Any =
        if (behovJson != null && behovJson.has("ident")) {
            // Enkeltpersonforetak: svar med array slik at FlerePersoninfoRiver håndterer det
            behovJson["ident"].map { ident ->
                personinfoMap() + mapOf("ident" to ident.asText())
            }
        } else {
            // Vanlig Personinfo-behov: svar med enkelt objekt slik at PersoninfoløsningRiver håndterer det
            personinfoMap()
        }

    private fun personinfoMap() = mapOf(
        "fornavn" to person.fornavn,
        "mellomnavn" to person.mellomnavn,
        "etternavn" to person.etternavn,
        "fødselsdato" to person.fødselsdato,
        "kjønn" to when (person.kjønn) {
            Kjønn.Mann -> "Mann"
            Kjønn.Kvinne -> "Kvinne"
            Kjønn.Ukjent -> "Ukjent"
        },
        "adressebeskyttelse" to adressebeskyttelse,
    )
}
