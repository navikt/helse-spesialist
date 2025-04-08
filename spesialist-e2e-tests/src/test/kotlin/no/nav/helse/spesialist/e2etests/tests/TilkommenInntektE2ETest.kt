package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class TilkommenInntektE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler legger til tilkommen inntekt`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerLeggerTilTilkommenInntekt()
        }

        // Then:


    }
}
