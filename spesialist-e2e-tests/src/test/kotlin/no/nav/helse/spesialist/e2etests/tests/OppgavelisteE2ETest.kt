package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveSorteringsfelt
import no.nav.helse.spesialist.application.testing.assertAfter
import no.nav.helse.spesialist.application.testing.assertAtLeast
import no.nav.helse.spesialist.application.testing.assertIsNumber
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class OppgavelisteE2ETest : AbstractE2EIntegrationTest() {
    fun assertMinimalOppgaveJson(oppgave: JsonNode) {
        // Sjekk genererte felter
        assertTrue(oppgave["id"]?.takeUnless { it.isNull }?.isTextual == true)
        val tiMinutterSiden = Instant.now().minusSeconds(60 * 10)
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["opprettetTidspunkt"].asText()))
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["behandlingOpprettetTidspunkt"].asText()))
        assertDoesNotThrow { oppgave["personPseudoId"].asUUID() }

        @Language("JSON")
        val expectedOppgaveJson =
            """
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
        assertJsonEquals(
            expectedOppgaveJson,
            oppgave,
            "id",
            "opprettetTidspunkt",
            "opprinneligSoeknadstidspunkt",
            "behandlingOpprettetTidspunkt",
            "personPseudoId",
        )
    }

    fun assertMaksimalOppgaveJson(oppgave: JsonNode) {
        // Sjekk genererte felter
        assertTrue(oppgave["id"]?.takeUnless { it.isNull }?.isTextual == true)
        val tiMinutterSidenLocalDateTime = LocalDateTime.now().minusSeconds(60 * 10)
        val tiMinutterSiden = Instant.now().minusSeconds(60 * 10)
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["opprettetTidspunkt"].asText()))
        assertAfter(tiMinutterSiden, Instant.parse(oppgave["behandlingOpprettetTidspunkt"].asText()))
        assertIsNumber(oppgave["paVentInfo"]["dialogRef"])
        assertAfter(tiMinutterSidenLocalDateTime, LocalDateTime.parse(oppgave["paVentInfo"]["opprettet"].asText()))
        oppgave["paVentInfo"]["kommentarer"].forEach { kommentar ->
            assertIsNumber(kommentar["id"])
            assertAfter(tiMinutterSidenLocalDateTime, LocalDateTime.parse(kommentar["opprettet"].asText()))
        }
        assertDoesNotThrow { oppgave["personPseudoId"].asUUID() }

        // Sjekk mappede felter
        @Language("JSON")
        val expectedOppgaveJson =
            """
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
                "oid" : "${saksbehandler.id.value}"
              },
              "paVentInfo": {
                "arsaker" : [ "Min første årsak", "Min andre årsak" ],
                "tekst" : "Min notattekst",
                "saksbehandler" : "${saksbehandler.ident.value}",
                "tidsfrist" : "${LocalDate.now().plusDays(1337)}",
                "kommentarer" : [
                  {
                    "tekst" : "Her er én kommentar",
                    "saksbehandlerident" : "${saksbehandler.ident.value}",
                    "feilregistrert_tidspunkt" : null
                  },
                  {
                    "tekst" : "Og her er en annen kommentar",
                    "saksbehandlerident" : "${saksbehandler.ident.value}",
                    "feilregistrert_tidspunkt" : null
                  }
                ]
              }
            }
            """.trimIndent()
        assertJsonEquals(
            expectedOppgaveJson,
            oppgave,
            "id",
            "opprettetTidspunkt",
            "opprinneligSoeknadstidspunkt",
            "behandlingOpprettetTidspunkt",
            "paVentInfo.dialogRef",
            "paVentInfo.opprettet",
            "paVentInfo.kommentarer.id",
            "paVentInfo.kommentarer.opprettet",
            "personPseudoId",
        )
    }

    fun hentOgAssertOppgaveIOppgaveliste(
        fane: Fane,
        forventetDukketOpp: Boolean,
        tildelt: Boolean? = null,
        egenskaper: Set<Egenskap> = emptySet(),
        ingenAvEgenskapene: Set<Egenskap> = emptySet(),
        sorteringsfelt: ApiOppgaveSorteringsfelt? = null,
    ): JsonNode? {
        val minstEnAvEgenskapene = egenskaper.groupBy { it.kategori }.map { it.value }
        // When:
        val response =
            callHttpGet(
                "api/oppgaver" +
                    buildList {
                        minstEnAvEgenskapene.forEach { egenskaper ->
                            add("minstEnAvEgenskapene=${egenskaper.tilKommaseparert()}")
                        }
                        if (ingenAvEgenskapene.isNotEmpty()) {
                            add("ingenAvEgenskapene=${ingenAvEgenskapene.tilKommaseparert()}")
                        }
                        if (tildelt != null) {
                            add("erTildelt=$tildelt")
                        }
                        if (fane in setOf(Fane.MINE_OPPGAVER, Fane.PÅ_VENT)) {
                            add("tildeltTilOid=${saksbehandler.id.value}")
                        }
                        when (fane) {
                            Fane.TIL_GODKJENNING -> {}

                            Fane.MINE_OPPGAVER -> {
                                add("erPaaVent=false")
                            }

                            Fane.PÅ_VENT -> {
                                add("erPaaVent=true")
                            }
                        }
                        add("sidetall=1")
                        add("sidestoerrelse=1000")
                        if (sorteringsfelt != null) {
                            add("sorteringsfelt=$sorteringsfelt")
                        }
                    }.joinToString(prefix = "?", separator = "&"),
            )

        // Then:
        if (forventetDukketOpp) {
            assertAtLeast(1, response["totaltAntall"].asLong())
            assertAtLeast(1, response["totaltAntallSider"].asLong())
            assertEquals(1, response["sidetall"].asInt())
            assertEquals(1000, response["sidestoerrelse"].asInt())
            assertAtLeast(1, response["elementer"].size())
        }

        val aktørId = testContext.person.aktørId

        val oppgaverForPerson =
            response["elementer"]
                .filter { it["aktorId"].asText() == aktørId }
        assertEquals(if (forventetDukketOpp) 1 else 0, oppgaverForPerson.size) {
            "Fikk uventet antall oppgaver for personen (aktørId: $aktørId)"
        }

        return if (forventetDukketOpp) oppgaverForPerson.single() else null
    }

    private fun Collection<Egenskap>.tilKommaseparert(): String = joinToString(separator = ",") { it.name.replace('Ø', 'O') }

    enum class Fane { TIL_GODKJENNING, MINE_OPPGAVER, PÅ_VENT }

    @Test
    fun `minimal oppgave dukker opp i listen med forventet innhold`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        val oppgave = hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = true)!!

        // Then:
        assertMinimalOppgaveJson(oppgave)
    }

    @Test
    fun `maksimal oppgave dukker opp i listen med forventet innhold`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerOppgavePåVent(
                notatTekst = "Min notattekst",
                frist = LocalDate.now().plusDays(1337),
                arsaker = mapOf("arsak1" to "Min første årsak", "arsak2" to "Min andre årsak"),
            )
            saksbehandlerKommentererLagtPåVent(tekst = "Her er én kommentar")
            // TODO: Feilregistrerer korrekt i testen, men query'en gir alltid null tilbake uansett (!)
            saksbehandlerFeilregistrererFørsteKommentarPåHistorikkinnslag()
            saksbehandlerKommentererLagtPåVent(tekst = "Og her er en annen kommentar")
        }

        // When:
        val oppgave = hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = true)!!

        // Then:
        assertMaksimalOppgaveJson(oppgave)
    }

    @Test
    fun `oppgave tildelt noen andre dukker kun opp i Til godkjenning`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil(saksbehandler = lagSaksbehandler()) {
            saksbehandlerTildelerSegSaken()
        }

        // Then:
        hentOgAssertOppgaveIOppgaveliste(Fane.TIL_GODKJENNING, true)
        hentOgAssertOppgaveIOppgaveliste(Fane.MINE_OPPGAVER, false)
        hentOgAssertOppgaveIOppgaveliste(Fane.PÅ_VENT, false)
    }

    @Test
    fun `oppgave tildelt en selv men ikke på vent dukker kun opp i Til godkjenning og Mine saker`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
        }

        // Then:
        hentOgAssertOppgaveIOppgaveliste(Fane.TIL_GODKJENNING, true)
        hentOgAssertOppgaveIOppgaveliste(Fane.MINE_OPPGAVER, true)
        hentOgAssertOppgaveIOppgaveliste(Fane.PÅ_VENT, false)
    }

    @Test
    fun `oppgave tildelt en selv og lagt på vent dukker kun opp i Til godkjenning og På vent`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerOppgavePåVent()
        }

        // Then:
        hentOgAssertOppgaveIOppgaveliste(Fane.TIL_GODKJENNING, true)
        hentOgAssertOppgaveIOppgaveliste(Fane.MINE_OPPGAVER, false)
        hentOgAssertOppgaveIOppgaveliste(Fane.PÅ_VENT, true)
    }

    @Test
    fun `tildelt oppgave dukker opp i Til godkjenning riktig mtp filtrering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
        }

        // Then:
        hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = true)
        hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = false, tildelt = false)
        hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = true, tildelt = true)
    }

    @Test
    fun `ikke-tildelt oppgave dukker opp i Til godkjenning riktig mtp filtrering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = true)
        hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = true, tildelt = false)
        hentOgAssertOppgaveIOppgaveliste(fane = Fane.TIL_GODKJENNING, forventetDukketOpp = false, tildelt = true)
    }

    @Test
    fun `søknad med utbetaling til arbeidsgiver dukker opp gitt riktig filtreringer`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(Egenskap.UTBETALING_TIL_ARBEIDSGIVER),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(Egenskap.UTBETALING_TIL_ARBEIDSGIVER, Egenskap.UTBETALING_TIL_SYKMELDT),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            egenskaper = setOf(Egenskap.UTBETALING_TIL_SYKMELDT),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            ingenAvEgenskapene = setOf(Egenskap.UTBETALING_TIL_ARBEIDSGIVER),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(Egenskap.SØKNAD, Egenskap.UTBETALING_TIL_ARBEIDSGIVER),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper =
                setOf(
                    Egenskap.SØKNAD,
                    Egenskap.REVURDERING,
                    Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                    Egenskap.UTBETALING_TIL_SYKMELDT,
                ),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            egenskaper =
                setOf(
                    Egenskap.REVURDERING,
                    Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                    Egenskap.UTBETALING_TIL_SYKMELDT,
                ),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            egenskaper =
                setOf(
                    Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                    Egenskap.UTBETALING_TIL_SYKMELDT,
                ),
            ingenAvEgenskapene = setOf(Egenskap.SØKNAD),
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper =
                setOf(
                    Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                    Egenskap.UTBETALING_TIL_SYKMELDT,
                ),
            ingenAvEgenskapene = setOf(Egenskap.REVURDERING),
        )
    }

    @ParameterizedTest
    @EnumSource(value = ApiOppgaveSorteringsfelt::class)
    fun `oppgave sortert på sorteringsfelt dukker opp i Til godkjenning`(sorteringsfelt: ApiOppgaveSorteringsfelt) {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            sorteringsfelt = sorteringsfelt,
        )
    }
}
