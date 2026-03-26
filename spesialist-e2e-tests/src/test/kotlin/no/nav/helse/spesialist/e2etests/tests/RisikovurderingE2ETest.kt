package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RisikovurderingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.funn = listOf(mapOf("kategori" to emptyList<Any>(), "beskrivelse" to "et faresignal"))

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertHarOppgaveegenskap("RISK_QA")
        }
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        // Given:
        // risikovurderingBehovLøser.kanGodkjenneAutomatisk = true (standard, ingen RISK_QA)

        // When:
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // Then:
        medPersonISpeil {
            assertHarOppgaveegenskap("SOKNAD")
            assertHarIkkeOppgaveegenskap("RISK_QA")
        }
    }

    @Test
    fun `sender med kunRefusjon`() {
        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        val risikovurderingBehov = finnRisikovurderingBehov()
        assertTrue(risikovurderingBehov["Risikovurdering"]["kunRefusjon"].asBoolean())
    }

    @Test
    fun `sender med inntekt`() {
        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        val risikovurderingBehov = finnRisikovurderingBehov()
        assertEquals("Arbeidsgiver", risikovurderingBehov["Risikovurdering"]["inntekt"]["inntektskilde"].asText())
        assertEquals(123456.7, risikovurderingBehov["Risikovurdering"]["inntekt"]["omregnetÅrsinntekt"].asDouble())
    }

    private fun finnRisikovurderingBehov(): JsonNode =
        meldinger()
            .first { melding ->
                melding["@event_name"].asText() == "behov" &&
                    melding["@behov"].any { it.asText() == "Risikovurdering" }
            }
}
