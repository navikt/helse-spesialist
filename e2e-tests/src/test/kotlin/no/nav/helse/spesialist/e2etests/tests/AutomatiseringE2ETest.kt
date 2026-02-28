package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class AutomatiseringE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `fatter automatisk vedtak`() {
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        // Given:
        søknadOgGodkjenningbehovKommerInn {
            // When:
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertGjeldendeOppgavestatus("AvventerSaksbehandler")
        }
        assertGodkjenningsbehovIkkeBesvart()
    }

    @Test
    fun `fatter vedtak automatisk ved åpen oppgave i Speil men ikke lenger åpen oppgave i Gosys`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        søknadOgGodkjenningbehovKommerInn()
        åpneOppgaverBehovLøser.antall = 0
        detPubliseresEnGosysOppgaveEndretMelding()

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }
}
