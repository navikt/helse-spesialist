package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import no.nav.helse.spesialist.kafka.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OppgavelisteMedGraphQLE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `minimal oppgave dukker opp i listen`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        val response = callGraphQL(
            operationName = "OppgaveFeed",
            variables = mapOf(
                "offset" to 0,
                "limit" to 10000,
                "filtrering" to mapOf(
                    "egenskaper" to emptyList<String>(),
                    "ekskluderteEgenskaper" to emptyList<String>(),
                    "ingenUkategoriserteEgenskaper" to false,
                    "tildelt" to null,
                    "egneSaker" to false,
                    "egneSakerPaVent" to false
                ),
                "sortering" to listOf(
                    mapOf(
                        "nokkel" to "OPPRETTET",
                        "stigende" to false
                    )
                )
            )
        )
        val responseData = response["data"]["oppgaveFeed"]

        assertAtLeast(1, responseData["totaltAntallOppgaver"].asInt())
        assertAtLeast(1, responseData["oppgaver"].size())

        val vedtaksperiodeId = førsteVedtaksperiode().vedtaksperiodeId

        val oppgaverForVedtaksperiode = responseData["oppgaver"]
            .filter { it["vedtaksperiodeId"].asText() == vedtaksperiodeId.toString() }
        assertEquals(1, oppgaverForVedtaksperiode.size) {
            "Fikk uventet antall oppgaver for vedtaksperioden (vedtaksperiodeId: $vedtaksperiodeId)"
        }
        val oppgave = oppgaverForVedtaksperiode.single()

        // Sjekk genererte felter
        assertTrue(oppgave["id"]?.takeUnless { it.isNull }?.isTextual == true)
        val tiMinutterSiden = LocalDateTime.now().minusMinutes(10)
        assertAfter(tiMinutterSiden, LocalDateTime.parse(oppgave["opprettet"].asText()))
        assertAfter(tiMinutterSiden, LocalDateTime.parse(oppgave["opprinneligSoknadsdato"].asText()))

        val oppgaveUtenGenererteFelter = (oppgave as ObjectNode).apply {
            remove("id")
            remove("opprettet")
            remove("opprinneligSoknadsdato")
        }

        @Language("JSON")
        val expectedOppgaveJson = """
            {
              "tidsfrist": null,
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "navn": {
                "fornavn": "${testContext.person.fornavn}",
                "etternavn": "${testContext.person.etternavn}",
                "mellomnavn": ${testContext.person.mellomnavn?.let { "\"$it\"" }}
              },
              "aktorId": "${testContext.person.aktørId}",
              "tildeling": null,
              "egenskaper": [
                {
                  "egenskap": "ARBEIDSTAKER",
                  "kategori": "Inntektsforhold"
                },
                {
                  "egenskap": "EN_ARBEIDSGIVER",
                  "kategori": "Inntektskilde"
                },
                {
                  "egenskap": "FORSTEGANGSBEHANDLING",
                  "kategori": "Periodetype"
                },
                {
                  "egenskap": "RISK_QA",
                  "kategori": "Ukategorisert"
                },
                {
                  "egenskap": "SOKNAD",
                  "kategori": "Oppgavetype"
                },
                {
                  "egenskap": "UTBETALING_TIL_ARBEIDSGIVER",
                  "kategori": "Mottaker"
                }
              ],
              "periodetype": "FORSTEGANGSBEHANDLING",
              "oppgavetype": "SOKNAD",
              "mottaker": "ARBEIDSGIVER",
              "antallArbeidsforhold": "ET_ARBEIDSFORHOLD",
              "paVentInfo": null
            }
        """.trimIndent()
        assertJsonEquals(expectedOppgaveJson, oppgaveUtenGenererteFelter)
    }

    @Test
    fun `maksimal oppgave dukker opp i listen`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerOppgavePåVent(
                notatTekst = "Min notattekst",
                frist = LocalDate.now().plusDays(1337),
                arsaker = mapOf("arsak1" to "Min første årsak", "arsak2" to "Min andre årsak")
            )
            oppdater()
            saksbehandlerKommentererLagtPåVent(tekst = "Her er én kommentar")
            oppdater()
            // TODO: Feilregistrerer korrekt i testen, men query'en gir alltid null tilbake uansett (!)
            saksbehandlerFeilregistrererFørsteKommentarPåHistorikkinnslag()
            saksbehandlerKommentererLagtPåVent(tekst = "Og her er en annen kommentar")
        }

        // When:
        val response = callGraphQL(
            operationName = "OppgaveFeed",
            variables = mapOf(
                "offset" to 0,
                "limit" to 10000,
                "filtrering" to mapOf(
                    "egenskaper" to emptyList<String>(),
                    "ekskluderteEgenskaper" to emptyList<String>(),
                    "ingenUkategoriserteEgenskaper" to false,
                    "tildelt" to null,
                    "egneSaker" to false,
                    "egneSakerPaVent" to false
                ),
                "sortering" to listOf(
                    mapOf(
                        "nokkel" to "OPPRETTET",
                        "stigende" to false
                    )
                )
            )
        )
        val responseData = response["data"]["oppgaveFeed"]

        assertAtLeast(1, responseData["totaltAntallOppgaver"].asInt())
        assertAtLeast(1, responseData["oppgaver"].size())

        val vedtaksperiodeId = førsteVedtaksperiode().vedtaksperiodeId

        val oppgaverForVedtaksperiode = responseData["oppgaver"]
            .filter { it["vedtaksperiodeId"].asText() == vedtaksperiodeId.toString() }
        assertEquals(1, oppgaverForVedtaksperiode.size) {
            "Fikk uventet antall oppgaver for vedtaksperioden (vedtaksperiodeId: $vedtaksperiodeId)"
        }
        val oppgave = oppgaverForVedtaksperiode.single()

        // Sjekk genererte felter
        assertIsTextual(oppgave["id"])
        val tiMinutterSiden = LocalDateTime.now().minusMinutes(10)
        assertAfter(tiMinutterSiden, LocalDateTime.parse(oppgave["opprettet"].asText()))
        assertAfter(tiMinutterSiden, LocalDateTime.parse(oppgave["opprinneligSoknadsdato"].asText()))
        assertIsNumber(oppgave["paVentInfo"]["dialogRef"])
        assertAfter(tiMinutterSiden, LocalDateTime.parse(oppgave["paVentInfo"]["opprettet"].asText()))
        oppgave["paVentInfo"]["kommentarer"].forEach { kommentar ->
            assertIsNumber(kommentar["id"])
            assertAfter(tiMinutterSiden, LocalDateTime.parse(kommentar["opprettet"].asText()))
        }

        val oppgaveUtenGenererteFelter = (oppgave as ObjectNode).apply {
            remove("id")
            remove("opprettet")
            remove("opprinneligSoknadsdato")
            (get("paVentInfo") as ObjectNode).apply {
                remove("dialogRef")
                remove("opprettet")
                get("kommentarer").forEach { kommentar ->
                    (kommentar as ObjectNode).apply {
                        remove("id")
                        remove("opprettet")
                    }
                }
            }
        }

        // Sjekk mappede felter
        @Language("JSON")
        val expectedOppgaveJson = """
            {
              "tidsfrist": "${LocalDate.now().plusDays(1337)}",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "navn": {
                "fornavn": "${testContext.person.fornavn}",
                "etternavn": "${testContext.person.etternavn}",
                "mellomnavn": ${testContext.person.mellomnavn?.let { "\"$it\"" }}
              },
              "aktorId": "${testContext.person.aktørId}",
              "tildeling": {
                "navn" : "${saksbehandler.navn}",
                "epost" : "${saksbehandler.epost}",
                "oid" : "${saksbehandler.id().value}"
              },
              "egenskaper": [
                {
                  "egenskap": "ARBEIDSTAKER",
                  "kategori": "Inntektsforhold"
                },
                {
                  "egenskap": "EN_ARBEIDSGIVER",
                  "kategori": "Inntektskilde"
                },
                {
                  "egenskap": "FORSTEGANGSBEHANDLING",
                  "kategori": "Periodetype"
                },
                {
                  "egenskap" : "PA_VENT",
                  "kategori" : "Status"
                }, {
                  "egenskap": "RISK_QA",
                  "kategori": "Ukategorisert"
                },
                {
                  "egenskap": "SOKNAD",
                  "kategori": "Oppgavetype"
                },
                {
                  "egenskap": "UTBETALING_TIL_ARBEIDSGIVER",
                  "kategori": "Mottaker"
                }
              ],
              "periodetype": "FORSTEGANGSBEHANDLING",
              "oppgavetype": "SOKNAD",
              "mottaker": "ARBEIDSGIVER",
              "antallArbeidsforhold": "ET_ARBEIDSFORHOLD",
              "paVentInfo": {
                "arsaker" : [ "Min første årsak", "Min andre årsak" ],
                "tekst" : "Min notattekst",
                "saksbehandler" : "${saksbehandler.ident}",
                "tidsfrist" : "${LocalDate.now().plusDays(1337)}",
                "kommentarer" : [
                  {
                    "tekst" : "Her er én kommentar",
                    "saksbehandlerident" : "${saksbehandler.ident}",
                    "feilregistrert_tidspunkt" : null
                  },
                  {
                    "tekst" : "Og her er en annen kommentar",
                    "saksbehandlerident" : "${saksbehandler.ident}",
                    "feilregistrert_tidspunkt" : null
                  }
                ]
              }
            }
        """.trimIndent()
        assertJsonEquals(expectedOppgaveJson, oppgaveUtenGenererteFelter)
    }

    private fun assertIsNumber(actual: JsonNode?) {
        assertTrue(actual?.takeUnless { it.isNull }?.isNumber == true)
    }

    private fun assertIsTextual(actual: JsonNode?) {
        assertTrue(actual?.takeUnless { it.isNull }?.isTextual == true)
    }

    private fun assertAfter(expectedAfter: LocalDateTime, actual: LocalDateTime) {
        assertTrue(actual.isAfter(expectedAfter)) { "Forventet tidspunkt etter $expectedAfter, men var $actual" }
    }

    private fun assertAtLeast(expectedMinimum: Int, actual: Int) {
        assertTrue(actual >= expectedMinimum) { "Forventet minst $expectedMinimum, men var $actual" }
    }

    private fun assertJsonEquals(expectedJson: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJson)),
            writer.writeValueAsString(actualJsonNode)
        )
    }
}
