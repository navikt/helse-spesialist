package no.nav.helse.spesialist.e2etests

import org.junit.jupiter.api.Test

class AutomatiseringE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `fatter vedtak automatisk ved åpen oppgave i Speil men ikke lenger åpen oppgave i Gosys`() {
        // Given:
        åpneOppgaverBehovLøser.antall = 1

        // When:
        simulerFremTilOgMedGodkjenningsbehov()
        åpneOppgaverBehovLøser.antall = 0
        simulerPublisertGosysOppgaveEndretMelding()

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }
}
