package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveSorteringsfelt
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import no.nav.helse.spesialist.kafka.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

abstract class AbstractOppgavelisteE2ETest : AbstractE2EIntegrationTest() {
    abstract fun assertMinimalOppgaveJson(oppgave: JsonNode)

    abstract fun assertMaksimalOppgaveJson(oppgave: JsonNode)

    abstract fun hentOgAssertOppgaveIOppgaveliste(
        fane: Fane,
        forventetDukketOpp: Boolean,
        tildelt: Boolean? = null,
        egenskaper: Set<Egenskap> = emptySet(),
        ingenAvEgenskapene: Set<Egenskap> = emptySet(),
        sorteringsfelt: ApiOppgaveSorteringsfelt? = null,
    ): JsonNode?

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
                arsaker = mapOf("arsak1" to "Min første årsak", "arsak2" to "Min andre årsak")
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
            forventetDukketOpp = true
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(Egenskap.UTBETALING_TIL_ARBEIDSGIVER)
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(Egenskap.UTBETALING_TIL_ARBEIDSGIVER, Egenskap.UTBETALING_TIL_SYKMELDT)
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            egenskaper = setOf(Egenskap.UTBETALING_TIL_SYKMELDT)
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            ingenAvEgenskapene = setOf(Egenskap.UTBETALING_TIL_ARBEIDSGIVER)
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(Egenskap.SØKNAD, Egenskap.UTBETALING_TIL_ARBEIDSGIVER)
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(
                Egenskap.SØKNAD,
                Egenskap.REVURDERING,
                Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                Egenskap.UTBETALING_TIL_SYKMELDT
            )
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            egenskaper = setOf(
                Egenskap.REVURDERING,
                Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                Egenskap.UTBETALING_TIL_SYKMELDT
            )
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = false,
            egenskaper = setOf(
                Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                Egenskap.UTBETALING_TIL_SYKMELDT
            ),
            ingenAvEgenskapene = setOf(Egenskap.SØKNAD)
        )
        hentOgAssertOppgaveIOppgaveliste(
            fane = Fane.TIL_GODKJENNING,
            forventetDukketOpp = true,
            egenskaper = setOf(
                Egenskap.UTBETALING_TIL_ARBEIDSGIVER,
                Egenskap.UTBETALING_TIL_SYKMELDT
            ),
            ingenAvEgenskapene = setOf(Egenskap.REVURDERING)
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
            sorteringsfelt = sorteringsfelt
        )
    }

    protected fun assertIsNumber(actual: JsonNode?) {
        assertTrue(actual?.takeUnless { it.isNull }?.isNumber == true)
    }

    protected fun assertIsTextual(actual: JsonNode?) {
        assertTrue(actual?.takeUnless { it.isNull }?.isTextual == true)
    }

    protected fun assertAfter(expectedAfter: Instant, actual: Instant) {
        assertTrue(actual.isAfter(expectedAfter)) { "Forventet tidspunkt etter $expectedAfter, men var $actual" }
    }

    protected fun assertAfter(expectedAfter: LocalDateTime, actual: LocalDateTime) {
        assertTrue(actual.isAfter(expectedAfter)) { "Forventet tidspunkt etter $expectedAfter, men var $actual" }
    }

    protected fun assertAtLeast(expectedMinimum: Long, actual: Long) {
        assertTrue(actual >= expectedMinimum) { "Forventet minst $expectedMinimum, men var $actual" }
    }

    protected fun assertAtLeast(expectedMinimum: Int, actual: Int) {
        assertTrue(actual >= expectedMinimum) { "Forventet minst $expectedMinimum, men var $actual" }
    }

    protected fun assertJsonEquals(expectedJson: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJson)),
            writer.writeValueAsString(actualJsonNode)
        )
    }
}
