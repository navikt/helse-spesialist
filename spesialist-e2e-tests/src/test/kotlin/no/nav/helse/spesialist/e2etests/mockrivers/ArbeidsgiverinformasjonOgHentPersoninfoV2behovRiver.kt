package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDateTime
import java.util.UUID

class ArbeidsgiverinformasjonOgHentPersoninfoV2behovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("Arbeidsgiverinformasjon", "HentPersoninfoV2"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "behov",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "@final" to true,
            "@behov" to listOf("Arbeidsgiverinformasjon", "HentPersoninfoV2"),
            "hendelseId" to json["hendelseId"].asText(),
            "contextId" to json["contextId"].asText(),
            "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
            "fødselsnummer" to testPerson.fødselsnummer,
            "aktørId" to testPerson.aktørId,
            "orgnummer" to testPerson.orgnummer,
            "@løsning" to mapOf(
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
        )
    ).toJson()
}
