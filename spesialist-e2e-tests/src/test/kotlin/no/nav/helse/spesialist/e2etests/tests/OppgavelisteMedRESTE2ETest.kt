package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.modell.oppgave.Egenskap
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime

class OppgavelisteMedRESTE2ETest : AbstractOppgavelisteE2ETest() {
    override fun assertMinimalOppgaveJson(oppgave: JsonNode) {
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
                  "vedtaksperiodeId": "${førsteVedtaksperiode().vedtaksperiodeId}",
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

    override fun assertMaksimalOppgaveJson(oppgave: JsonNode) {
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
                  "vedtaksperiodeId": "${førsteVedtaksperiode().vedtaksperiodeId}",
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

    override fun hentOgAssertOppgaveIOppgaveliste(
        fane: Fane,
        forventetDukketOpp: Boolean,
        tildelt: Boolean?,
        egenskaper: Set<Egenskap>,
        ingenAvEgenskapene: Set<Egenskap>
    ): JsonNode? {
        val minstEnAvEgenskapene = egenskaper.groupBy { it.kategori }.map { it.value }
        // When:
        val response = callHttpGet(buildString {
            append("api/oppgaver")
            append("?pageNumber=1")
            append("&pageSize=1000")
            minstEnAvEgenskapene.forEachIndexed { index, egenskaper ->
                append("&minstEnAvEgenskapene[$index]=${egenskaper.tilKommaseparert()}")
            }
            append("&ingenAvEgenskapene=${ingenAvEgenskapene.tilKommaseparert()}")
            append("&erTildelt=${tildelt}")
            append("&erPaaVent=${fane == Fane.PÅ_VENT}")
            if (fane in setOf(Fane.MINE_OPPGAVER, Fane.PÅ_VENT)) {
                append("&tildeltTilOid=${saksbehandler.id().value}")
            }
        })

        // Then:
        if (forventetDukketOpp) {
            assertAtLeast(1, response["totaltAntallOppgaver"].asInt())
            assertAtLeast(1, response["oppgaver"].size())
        }

        val vedtaksperiodeId = førsteVedtaksperiode().vedtaksperiodeId

        val oppgaverForVedtaksperiode = response["oppgaver"]
            .filter { it["vedtaksperiodeId"].asText() == vedtaksperiodeId.toString() }
        assertEquals(if (forventetDukketOpp) 1 else 0, oppgaverForVedtaksperiode.size) {
            "Fikk uventet antall oppgaver for vedtaksperioden (vedtaksperiodeId: $vedtaksperiodeId)"
        }

        return if (forventetDukketOpp) oppgaverForVedtaksperiode.single() else null
    }

    private fun Collection<Egenskap>.tilKommaseparert(): String =
        joinToString(separator = ",") { it.name.replace('Ø', 'O') }
}
