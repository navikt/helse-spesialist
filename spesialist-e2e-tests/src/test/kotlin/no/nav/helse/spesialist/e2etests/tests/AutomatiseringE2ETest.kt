package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class AutomatiseringE2ETest : AbstractE2EIntegrationTest() {
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
