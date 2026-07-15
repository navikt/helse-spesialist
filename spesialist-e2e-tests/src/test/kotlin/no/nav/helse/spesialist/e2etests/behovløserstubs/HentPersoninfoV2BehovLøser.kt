package no.nav.helse.spesialist.e2etests.behovløserstubs

import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.typer.Kjønn
import tools.jackson.databind.JsonNode

class HentPersoninfoV2BehovLøser(
    private val person: Person,
) : AbstractBehovLøser("HentPersoninfoV2") {
    var adressebeskyttelse = "Ugradert"

    override fun løsning(behovJson: JsonNode): Any =
        if (behovJson.has("ident")) {
            // Enkeltpersonforetak: svar med array slik at FlerePersoninfoRiver håndterer det
            behovJson["ident"].toList().map { ident ->
                personinfoMap() + mapOf("ident" to ident.asString())
            }
        } else {
            // Vanlig Personinfo-behov: svar med enkelt objekt slik at PersoninfoløsningRiver håndterer det
            personinfoMap()
        }

    private fun personinfoMap() =
        mapOf(
            "fornavn" to person.fornavn,
            "mellomnavn" to person.mellomnavn,
            "etternavn" to person.etternavn,
            "fødselsdato" to person.fødselsdato,
            "kjønn" to
                when (person.kjønn) {
                    Kjønn.Mann -> "Mann"
                    Kjønn.Kvinne -> "Kvinne"
                    Kjønn.Ukjent -> "Ukjent"
                },
            "adressebeskyttelse" to adressebeskyttelse,
        )
}
