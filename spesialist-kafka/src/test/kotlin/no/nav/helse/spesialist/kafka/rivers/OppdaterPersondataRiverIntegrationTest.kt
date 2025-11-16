package no.nav.helse.spesialist.kafka.rivers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.spesialist.kafka.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdaterPersondataRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    @Test
    fun `oppdater persondata blir prosessert riktig`() {
        // Given:
        val person = lagPerson()
            .also(sessionContext.personRepository::lagre)

        val vedtaksperiode = lagVedtaksperiode(identitetsnummer = person.identitetsnummer)
            .also(sessionContext.vedtaksperiodeRepository::lagre)

        val behandling = lagBehandling(vedtaksperiodeId = vedtaksperiode.id)
            .also(sessionContext.behandlingRepository::lagre)

        // When:
        testRapid.sendTestMessage(oppdaterPersondataMelding(person))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(2, meldinger.size)
        meldinger.forEach {
            assertEquals(person.identitetsnummer.value, it.key)
        }
        val behovMelding = meldinger.first { it.json["@event_name"].asText() == "behov" }
        val actualJsonNode = behovMelding.json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "behov",
              "@behov": [
                "HentInfotrygdutbetalinger"
              ],
              "HentInfotrygdutbetalinger": {
                "historikkFom": "${behandling.fom.minusYears(3)}",
                "historikkTom": "${LocalDate.now()}"
              },
              "fødselsnummer": "${person.identitetsnummer.value}",
              "hendelseId": "bff52e44-d009-43c8-af43-a14a38b66cfb"
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, (actualJsonNode as ObjectNode).apply {
            remove("@behovId")
            remove("contextId")
        })
    }

    @Test
    fun `oppdater persondata hopper over behov når det ikke finnes noen behandlinger`() {
        // Given:
        val person = lagPerson()
            .also(sessionContext.personRepository::lagre)

        // When:
        testRapid.sendTestMessage(oppdaterPersondataMelding(person))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        meldinger.forEach {
            assertEquals(person.identitetsnummer.value, it.key)
        }
        assertEquals(null, meldinger.find { it.json["@event_name"].asText() == "behov" }?.json)
    }

    private fun assertJsonEquals(expectedJson: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJson)),
            writer.writeValueAsString(actualJsonNode)
        )
    }

    @Language("JSON")
    private fun oppdaterPersondataMelding(person: Person) =
        """
    {
      "@event_name": "oppdater_persondata",
      "fødselsnummer": "${person.identitetsnummer.value}",
      "@id": "bff52e44-d009-43c8-af43-a14a38b66cfb",
      "@opprettet": "2025-08-22T10:12:25.424748984",
      "system_read_count": 1,
      "system_participating_services": [
        {
          "id": "bff52e44-d009-43c8-af43-a14a38b66cfb",
          "time": "2025-08-22T10:12:25.424748984",
          "service": "spesialist",
          "instance": "spesialist-8944b8c68-zlhkj",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.19-14.16-da17e2f"
        },
        {
          "id": "bff52e44-d009-43c8-af43-a14a38b66cfb",
          "time": "2025-08-22T10:12:25.433034589",
          "service": "spesialist",
          "instance": "spesialist-8944b8c68-zlhkj",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.19-14.16-da17e2f"
        }
      ]
    }
    """.trimIndent()
}
