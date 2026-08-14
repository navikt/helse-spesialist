package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.GraderteAndreYtelserBehovRiver
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.medTransaksjonelleRivers
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
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
    fun `publiserer overlappende graderte andre ytelser som flat liste og bevarer packet`() {
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.FORELDREPENGER,
            perioder =
                listOf(
                    GraderteAndreYtelserPeriode((1.januar() tilOgMed 2.januar()), 50),
                    GraderteAndreYtelserPeriode((5.januar() tilOgMed 6.januar()), 80),
                ),
        )
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.SVANGERSKAPSPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((3.januar() tilOgMed 4.januar()), 40)),
        )
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.OMSORGSPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((1.februar() tilOgMed 2.februar()), 20)),
        )
        lagreFjernetGradertYtelse(
            type = GraderteAndreYtelserType.PLEIEPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((7.januar() tilOgMed 8.januar()), 60)),
        )

        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar()))

        val svar = rapid.inspektør.message(0)
        val løsning = svar["@løsning"]["GraderteAndreYtelserForBeregning"]
        assertEquals("behov-123", svar["@behovId"].asString())
        assertEquals("behandling-123", svar["behandlingId"].asString())
        assertEquals(3, løsning.size())
        assertLøsningsrad(løsning[0], "FORELDREPENGER", 1.januar(), 2.januar(), 50)
        assertLøsningsrad(løsning[1], "FORELDREPENGER", 5.januar(), 6.januar(), 80)
        assertLøsningsrad(løsning[2], "SVANGERSKAPSPENGER", 3.januar(), 4.januar(), 40)
    }

    @Test
    fun `publiserer forventet json-struktur - untatt @ feltene satt av R&R`() {
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.PLEIEPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((1.november2023() tilOgMed 1.mars2024()), 50)),
        )
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.SVANGERSKAPSPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((10.januar() tilOgMed 12.januar()), 20)),
        )

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
                "GraderteAndreYtelserForBeregning": [
                  {
                    "ytelse": "PLEIEPENGER",
                    "fom": "2024-01-01",
                    "tom": "2024-01-31",
                    "grad": 50
                  },
                  {
                    "ytelse": "SVANGERSKAPSPENGER",
                    "fom": "2024-01-10",
                    "tom": "2024-01-12",
                    "grad": 20
                  }
                ]
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `avgrenser perioder i løsningen til forespurt fom og tom`() {
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.PLEIEPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((1.november2023() tilOgMed 1.mars2024()), 50)),
        )

        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar()))

        val løsning = rapid.inspektør.message(0)["@løsning"]["GraderteAndreYtelserForBeregning"]
        assertEquals(1, løsning.size())
        assertLøsningsrad(løsning[0], "PLEIEPENGER", 1.januar(), 31.januar(), 50)
    }

    @Test
    fun `publiserer tom liste når ingen perioder overlapper`() {
        lagreGraderteAndreYtelser(
            type = GraderteAndreYtelserType.FORELDREPENGER,
            perioder = listOf(GraderteAndreYtelserPeriode((1.februar() tilOgMed 2.februar()), 50)),
        )

        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar()))

        val løsning = rapid.inspektør.message(0)["@løsning"]["GraderteAndreYtelserForBeregning"]
        assertEquals(0, løsning.size())
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

    private fun lagreGraderteAndreYtelser(
        type: GraderteAndreYtelserType,
        perioder: List<GraderteAndreYtelserPeriode>,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(testperson.fødselsnummer)
        val saksbehandlerIdent = lagSaksbehandler().ident
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            it.graderteAndreYtelserRepository.lagre(
                GraderteAndreYtelser.ny(
                    identitetsnummer = identitetsnummer,
                    saksbehandlerIdent = saksbehandlerIdent,
                    notatTilBeslutter = "notat",
                    totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                    graderteAndreYtelserPerioder = perioder,
                    graderteAndreYtelserType = type,
                ),
            )
        }
    }

    private fun lagreFjernetGradertYtelse(
        type: GraderteAndreYtelserType,
        perioder: List<GraderteAndreYtelserPeriode>,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(testperson.fødselsnummer)
        val saksbehandlerIdent = lagSaksbehandler().ident
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = "notat",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserPerioder = perioder,
                graderteAndreYtelserType = type,
            )
        graderteAndreYtelser.fjern(
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = "fjernet",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
        )
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            it.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)
        }
    }

    private fun assertLøsningsrad(
        jsonNode: JsonNode,
        ytelse: String,
        fom: LocalDate,
        tom: LocalDate,
        grad: Int,
    ) {
        assertEquals(ytelse, jsonNode["ytelse"].asString())
        assertEquals(fom.toString(), jsonNode["fom"].asString())
        assertEquals(tom.toString(), jsonNode["tom"].asString())
        assertEquals(grad, jsonNode["grad"].asInt())
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

    private fun Int.februar() = LocalDate.of(2024, 2, this)

    private fun Int.november2023() = LocalDate.of(2023, 11, this)

    private fun Int.mars2024() = LocalDate.of(2024, 3, this)
}
