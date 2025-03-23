package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDateTime
import java.util.UUID

class HentInfotrygdutbetalingerbehovRiver(private val testPerson: TestPerson) : AbstractMockRiver() {
    override fun precondition(jsonMessage: JsonMessage) {
        jsonMessage.requireAll("@behov", listOf("HentInfotrygdutbetalinger"))
        jsonMessage.forbid("@løsning")
    }

    override fun responseFor(json: JsonNode) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "behov",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "@final" to true,
            "@behov" to listOf("HentInfotrygdutbetalinger"),
            "hendelseId" to json["hendelseId"].asText(),
            "contextId" to json["contextId"].asText(),
            "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
            "fødselsnummer" to testPerson.fødselsnummer,
            "aktørId" to testPerson.aktørId,
            "orgnummer" to testPerson.orgnummer,
            "HentInfotrygdutbetalinger" to mapOf(
                "historikkFom" to json["HentInfotrygdutbetalinger"]["historikkFom"].asText(),
                "historikkTom" to json["HentInfotrygdutbetalinger"]["historikkTom"].asText()
            ),
            "@løsning" to mapOf(
                "HentInfotrygdutbetalinger" to listOf(
                    mapOf(
                        "fom" to "2018-01-01",
                        "tom" to "2018-01-31",
                        "dagsats" to "1000.0",
                        "grad" to "100",
                        "typetekst" to "ArbRef",
                        "organisasjonsnummer" to testPerson.orgnummer
                    )
                )
            )
        )
    ).toJson()
}
