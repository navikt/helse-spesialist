package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.VårTestPerson
import no.nav.helse.spesialist.typer.Kjønn

class HentPersoninfoV2BehovLøser(private val person: VårTestPerson) : AbstractBehovLøser("HentPersoninfoV2") {
    override fun løsning(behovJson: JsonNode) = mapOf(
        "fornavn" to person.fornavn,
        "mellomnavn" to person.mellomnavn,
        "etternavn" to person.etternavn,
        "fødselsdato" to person.fødselsdato,
        "kjønn" to when (person.kjønn) {
            Kjønn.Mann -> "Mann"
            Kjønn.Kvinne -> "Kvinne"
            Kjønn.Ukjent -> "Ukjent"
        },
        "adressebeskyttelse" to "Ugradert"
    )
}
