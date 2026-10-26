package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.GraderteAndreYtelserBehovRiver
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.medTransaksjonelleRivers
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFails

internal class GraderteAndreYtelserBehovRiverTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    private val rapid = TestRapid().medTransaksjonelleRivers(inMemoryRepositoriesAndDaos, GraderteAndreYtelserBehovRiver())
    private val testperson = TestPerson()

    @Test
    fun `svarer med tom løsning og bevarer packet`() {
        lagreGraderteAndreYtelser(type = GraderteAndreYtelserType.PLEIEPENGER)

        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar()))

        assertPublisertJson(
            """
            {
              "@event_name": "behov",
              "@behov": ["GraderteAndreYtelserForBeregning"],
              "@behovId": "behov-123",
              "fødselsnummer": "${testperson.fødselsnummer}",
              "behandlingId": "behandling-123",
              "GraderteAndreYtelserForBeregning": {
                "fom": "2024-01-01",
                "tom": "2024-01-31"
              },
              "@løsning": {
                "GraderteAndreYtelserForBeregning": []
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `ignorerer meldinger som allerede har løsning`() {
        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar(), medLøsning = true))

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `feiler ved manglende tom-dato`() {
        assertFails {
            rapid.sendTestMessage(behovmeldingUtenTom())
        }
    }

    private fun lagreGraderteAndreYtelser(type: GraderteAndreYtelserType) {
        val identitetsnummer = Identitetsnummer.fraString(testperson.fødselsnummer)
        val saksbehandlerIdent = lagSaksbehandler().ident
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            it.graderteAndreYtelserRepository.lagre(
                GraderteAndreYtelser.ny(
                    identitetsnummer = identitetsnummer,
                    saksbehandlerIdent = saksbehandlerIdent,
                    notatTilBeslutter = "notat",
                    totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                    graderteAndreYtelserType = type,
                ),
            )
        }
    }

    private fun assertPublisertJson(
        @Language("JSON") forventetJson: String,
    ) {
        val faktiskJson = rapid.inspektør.message(0).deepCopy() as ObjectNode
        faktiskJson.remove("system_read_count")
        faktiskJson.remove("system_participating_services")
        faktiskJson.remove("@forårsaket_av")
        faktiskJson.remove("@opprettet")
        faktiskJson.remove("@opprettetUTC")
        faktiskJson.remove("@sendt")
        faktiskJson.remove("@id")
        val forventet = objectMapper.readTree(forventetJson)
        assertEquals(
            objectMapper.writeValueAsString(forventet),
            objectMapper.writeValueAsString(faktiskJson),
        )
    }

    @Language("JSON")
    private fun behovmelding(
        fom: LocalDate,
        tom: LocalDate,
        medLøsning: Boolean = false,
    ): String {
        val løsning =
            if (medLøsning) {
                """,
                  "@løsning": {
                    "GraderteAndreYtelserForBeregning": []
                  }"""
            } else {
                ""
            }
        return """
            {
              "@event_name": "behov",
              "@behov": ["GraderteAndreYtelserForBeregning"],
              "@behovId": "behov-123",
              "@id": "48d9f7b4-54d0-4be4-84b1-d950091d1c2f",
              "@opprettet": "2024-01-01T00:00:00",
              "@opprettetUTC": "2024-01-01T00:00:00Z",
              "@sendt": "2024-01-01T11:59:59Z",
              "fødselsnummer": "${testperson.fødselsnummer}",
              "behandlingId": "behandling-123",
              "GraderteAndreYtelserForBeregning": {
                "fom": "$fom",
                "tom": "$tom"
              }$løsning
            }
            """.trimIndent()
    }

    @Language("JSON")
    private fun behovmeldingUtenTom(): String =
        """
        {
          "@event_name": "behov",
          "@behov": ["GraderteAndreYtelserForBeregning"],
          "@id": "48d9f7b4-54d0-4be4-84b1-d950091d1c2f",
          "@opprettet": "2024-01-01T00:00:00",
          "fødselsnummer": "${testperson.fødselsnummer}",
          "GraderteAndreYtelserForBeregning": {
            "fom": "2024-01-01"
          }
        }
        """.trimIndent()

    private fun Int.januar() = LocalDate.of(2024, 1, this)
}
