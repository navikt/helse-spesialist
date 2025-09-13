package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class EgenAnsattStatusEndretE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `oppgave får egen ansatt egenskap når person får status egen ansatt`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        saksbehandlerHarTilgang(Tilgangsgruppe.SKJERMEDE)
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            assertHarIkkeOppgaveegenskap("EGEN_ANSATT")
        }

        // When:
        skjermetInfoEndres(skjermet = true)

        // Then:
        medPersonISpeil {
            assertHarOppgaveegenskap("EGEN_ANSATT")
        }
    }

    @Test
    fun `oppgave har ikke lenger egen ansatt egenskap når person ikke lenger er egen ansatt`() {
        // Given:
        saksbehandlerHarTilgang(Tilgangsgruppe.SKJERMEDE)
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            assertHarIkkeOppgaveegenskap("EGEN_ANSATT")
        }

        // When:
        skjermetInfoEndres(skjermet = true)
        medPersonISpeil {
            assertHarOppgaveegenskap("EGEN_ANSATT")
        }
        skjermetInfoEndres(skjermet = false)

        // Then:
        medPersonISpeil {
            assertHarIkkeOppgaveegenskap("EGEN_ANSATT")
        }
    }
}
