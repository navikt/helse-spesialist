package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveSorteringsfelt
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class OppgavelisteMedRESTE2ETest : AbstractOppgavelisteE2ETest() {
    override fun assertMinimalOppgaveJson(oppgave: JsonNode) {
        // Sjekk genererte felter
        assertTrue(oppgave["id"]?.takeUnless { it.isNull }?.isTextual == true)
        val tiMinutterSiden = Instant.now().minusSeconds(60*10)
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["opprettetTidspunkt"].asText()))
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["opprinneligSoeknadstidspunkt"].asText()))

        val oppgaveUtenGenererteFelter = (oppgave as ObjectNode).apply {
            remove("id")
            remove("opprettetTidspunkt")
            remove("opprinneligSoeknadstidspunkt")
        }

        @Language("JSON")
        val expectedOppgaveJson = """
                {
                  "aktorId": "${testContext.person.aktørId}",
                  "navn": {
                    "fornavn": "${testContext.person.fornavn}",
                    "etternavn": "${testContext.person.etternavn}",
                    "mellomnavn": ${testContext.person.mellomnavn?.let { "\"$it\"" }}
                  },
                  "egenskaper": [ "ARBEIDSTAKER", "EN_ARBEIDSGIVER", "FORSTEGANGSBEHANDLING", "RISK_QA", "SOKNAD", "UTBETALING_TIL_ARBEIDSGIVER" ],
                  "tildeling": null,
                  "paVentInfo": null
                }
            """.trimIndent()
        assertJsonEquals(expectedOppgaveJson, oppgaveUtenGenererteFelter)
    }

    override fun assertMaksimalOppgaveJson(oppgave: JsonNode) {
        // Sjekk genererte felter
        assertTrue(oppgave["id"]?.takeUnless { it.isNull }?.isTextual == true)
        val tiMinutterSidenLocalDateTime = LocalDateTime.now().minusSeconds(60*10)
        val tiMinutterSiden = Instant.now().minusSeconds(60*10)
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["opprettetTidspunkt"].asText()))
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["opprinneligSoeknadstidspunkt"].asText()))
        assertIsNumber(oppgave["paVentInfo"]["dialogRef"])
        assertAfter(tiMinutterSidenLocalDateTime, LocalDateTime.parse(oppgave["paVentInfo"]["opprettet"].asText()))
        oppgave["paVentInfo"]["kommentarer"].forEach { kommentar ->
            assertIsNumber(kommentar["id"])
            assertAfter(tiMinutterSidenLocalDateTime, LocalDateTime.parse(kommentar["opprettet"].asText()))
        }

        val oppgaveUtenGenererteFelter = (oppgave as ObjectNode).apply {
            remove("id")
            remove("opprettetTidspunkt")
            remove("opprinneligSoeknadstidspunkt")
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
                  "aktorId": "${testContext.person.aktørId}",
                  "navn": {
                    "fornavn": "${testContext.person.fornavn}",
                    "etternavn": "${testContext.person.etternavn}",
                    "mellomnavn": ${testContext.person.mellomnavn?.let { "\"$it\"" }}
                  },
                  "egenskaper": [ "ARBEIDSTAKER", "EN_ARBEIDSGIVER", "FORSTEGANGSBEHANDLING", "PA_VENT", "RISK_QA", "SOKNAD", "UTBETALING_TIL_ARBEIDSGIVER" ],
                  "tildeling": {
                    "navn" : "${saksbehandler.navn}",
                    "epost" : "${saksbehandler.epost}",
                    "oid" : "${saksbehandler.id().value}"
                  },
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
        ingenAvEgenskapene: Set<Egenskap>,
        sorteringsfelt: ApiOppgaveSorteringsfelt?
    ): JsonNode? {
        val minstEnAvEgenskapene = egenskaper.groupBy { it.kategori }.map { it.value }
        // When:
        val response = callHttpGet("api/oppgaver" + buildList {
            minstEnAvEgenskapene.forEach { egenskaper ->
                add("minstEnAvEgenskapene=${egenskaper.tilKommaseparert()}")
            }
            if (ingenAvEgenskapene.isNotEmpty()) {
                add("ingenAvEgenskapene=${ingenAvEgenskapene.tilKommaseparert()}")
            }
            if (tildelt != null) {
                add("erTildelt=${tildelt}")
            }
            if (fane in setOf(Fane.MINE_OPPGAVER, Fane.PÅ_VENT)) {
                add("tildeltTilOid=${saksbehandler.id().value}")
            }
            when (fane) {
                Fane.TIL_GODKJENNING -> {}
                Fane.MINE_OPPGAVER -> add("erPaaVent=false")
                Fane.PÅ_VENT -> add("erPaaVent=true")
            }
            add("sidetall=1")
            add("sidestoerrelse=1000")
            if (sorteringsfelt != null) {
                add("sorteringsfelt=$sorteringsfelt")
            }
        }.joinToString(prefix = "?", separator = "&"))

        // Then:
        if (forventetDukketOpp) {
            assertAtLeast(1, response["totaltAntall"].asLong())
            assertAtLeast(1, response["totaltAntallSider"].asLong())
            assertEquals(1, response["sidetall"].asInt())
            assertEquals(1000, response["sidestoerrelse"].asInt())
            assertAtLeast(1, response["elementer"].size())
        }

        val aktørId = testContext.person.aktørId

        val oppgaverForPerson = response["elementer"]
            .filter { it["aktorId"].asText() == aktørId }
        assertEquals(if (forventetDukketOpp) 1 else 0, oppgaverForPerson.size) {
            "Fikk uventet antall oppgaver for personen (aktørId: $aktørId)"
        }

        return if (forventetDukketOpp) oppgaverForPerson.single() else null
    }

    private fun Collection<Egenskap>.tilKommaseparert(): String =
        joinToString(separator = ",") { it.name.replace('Ø', 'O') }
}
