package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.GraderteAndreYtelserBehovRiver
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.medTransaksjonelleRivers
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFails

internal class GraderteAndreYtelserBehovRiverTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    private val rapid = TestRapid().medTransaksjonelleRivers(inMemoryRepositoriesAndDaos, GraderteAndreYtelserBehovRiver())
    private val testperson = TestPerson()

    @Test
    fun `publiserer kun dager innenfor forespurt periode og bevarer packet`() {
        val graderteAndreYtelser =
            lagreGraderteAndreYtelser(
                dager =
                    listOf(
                        dag(dato = 1.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 50)),
                        dag(dato = 2.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 80), sykdomsgrad = 20),
                        dag(dato = 3.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.OMSORGSPENGER to 20)),
                    ),
                organisasjonsnummer = "123456789",
            )

        rapid.sendTestMessage(behovmelding(fom = 2.januar(), tom = 2.januar()))

        val svar = rapid.inspektør.message(0)
        val løsning = svar["@løsning"]["GraderteAndreYtelserForBeregning"]
        assertEquals("behov-123", svar["@behovId"].asString())
        assertEquals("behandling-123", svar["behandlingId"].asString())
        assertEquals(1, løsning.size())
        assertEquals(graderteAndreYtelser.versjon.toString(), løsning[0]["id"].asString())
        assertEquals("123456789", løsning[0]["organisasjonsnummer"].asString())
        assertEquals(1, løsning[0]["dager"].size())
        assertEquals("2024-01-02", løsning[0]["dager"][0]["dato"].asString())
        assertEquals(80, løsning[0]["dager"][0]["andreYtelserGrader"]["FORELDREPENGER"].asInt())
    }

    @Test
    fun `svarer med tilstanden og versjonen etter at alle eventene er spilt av`() {
        val graderteAndreYtelser =
            lagreGraderteAndreYtelser(
                dager =
                    listOf(
                        dag(dato = 1.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 50)),
                        dag(dato = 2.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 50)),
                    ),
                endringer =
                    listOf(
                        listOf(dag(dato = 2.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 30), sykdomsgrad = 70)),
                        listOf(dag(dato = 2.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 10), sykdomsgrad = 90)),
                    ),
                organisasjonsnummer = "123456789",
            )

        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar()))

        val løsning = rapid.inspektør.message(0)["@løsning"]["GraderteAndreYtelserForBeregning"][0]
        assertEquals(
            graderteAndreYtelser.events
                .last()
                .versjon
                .toString(),
            løsning["id"].asString(),
        )
        val dager = løsning["dager"]
        assertEquals(2, dager.size())
        assertEquals(50, dager[0]["andreYtelserGrader"]["FORELDREPENGER"].asInt())
        assertEquals(10, dager[1]["andreYtelserGrader"]["FORELDREPENGER"].asInt())
        assertEquals(90, dager[1]["sykdomsgrad"].asInt())
    }

    @Test
    fun `publiserer forventet json-struktur - untatt @ feltene satt av R&R`() {
        val arbeidstaker =
            lagreGraderteAndreYtelser(
                dager =
                    listOf(
                        dag(
                            dato = 10.januar(),
                            andreYtelserGrader = mapOf(GraderteAndreYtelserType.PLEIEPENGER to 50),
                            sykdomsgrad = 30,
                            arbeidsgrad = 20,
                        ),
                    ),
                organisasjonsnummer = "123456789",
            )
        val frilans =
            lagreGraderteAndreYtelser(
                dager =
                    listOf(
                        dag(
                            dato = 10.januar(),
                            andreYtelserGrader = mapOf(GraderteAndreYtelserType.SVANGERSKAPSPENGER to 40),
                            sykdomsgrad = 60,
                            arbeidsgrad = 0,
                        ),
                    ),
                yrkesaktivitet = Yrkesaktivitetstype.FRILANS,
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
                    "id": "${arbeidstaker.versjon}",
                    "organisasjonsnummer": "123456789",
                    "dager": [
                      {
                        "dato": "2024-01-10",
                        "andreYtelserGrader": { "PLEIEPENGER": 50 },
                        "sykdomsgrad": 30,
                        "arbeidsgrad": 20
                      }
                    ]
                  },
                  {
                    "id": "${frilans.versjon}",
                    "yrkesaktivitetstype": "FRILANS",
                    "dager": [
                      {
                        "dato": "2024-01-10",
                        "andreYtelserGrader": { "SVANGERSKAPSPENGER": 40 },
                        "sykdomsgrad": 60,
                        "arbeidsgrad": 0
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `publiserer tom liste når ingen dager er innenfor forespurt periode`() {
        lagreGraderteAndreYtelser(
            dager = listOf(dag(dato = 1.februar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 50))),
            organisasjonsnummer = "123456789",
        )

        rapid.sendTestMessage(behovmelding(fom = 1.januar(), tom = 31.januar()))

        val løsning = rapid.inspektør.message(0)["@løsning"]["GraderteAndreYtelserForBeregning"]
        assertEquals(0, løsning.size())
    }

    @Test
    fun `publiserer tom liste når fom er etter tom`() {
        lagreGraderteAndreYtelser(
            dager = listOf(dag(dato = 15.januar(), andreYtelserGrader = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 50))),
            organisasjonsnummer = "123456789",
        )

        rapid.sendTestMessage(behovmelding(fom = 31.januar(), tom = 1.januar()))

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

    private fun dag(
        dato: LocalDate,
        andreYtelserGrader: Map<GraderteAndreYtelserType, Int>,
        sykdomsgrad: Int = 50,
        arbeidsgrad: Int = 0,
    ) = GraderteAndreYtelserDag(
        dato = dato,
        andreYtelserGrader = andreYtelserGrader,
        sykdomsgrad = sykdomsgrad,
        arbeidsgrad = arbeidsgrad,
    )

    private fun lagreGraderteAndreYtelser(
        dager: List<GraderteAndreYtelserDag>,
        endringer: List<List<GraderteAndreYtelserDag>> = emptyList(),
        yrkesaktivitet: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        organisasjonsnummer: String? = null,
    ): GraderteAndreYtelser {
        val saksbehandlerIdent = lagSaksbehandler().ident
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                id =
                    GraderteAndreYtelserId(
                        identitetsnummer = Identitetsnummer.fraString(testperson.fødselsnummer),
                        yrkesaktivitet = yrkesaktivitet,
                        organisasjonsnummer = organisasjonsnummer,
                    ),
                dager = dager,
                saksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = "notat",
            )
        endringer.forEach { graderteAndreYtelser.endre(it, saksbehandlerIdent, "endring") }
        inMemoryRepositoriesAndDaos.sessionFactory.transactionalSessionScope {
            it.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)
        }
        return graderteAndreYtelser
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
}
