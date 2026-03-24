package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import kotlin.test.Test

class PåVentE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler legger oppgaven på vent`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // When
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerOppgavePåVent(frist = 1.jan(2026))
        }

        // Then
        medPersonISpeil {
            assertHarOppgaveegenskap("PA_VENT")
            assertPåVent(frist = 1.jan(2026))
        }
    }

    @Test
    fun `saksbehandler endrer på vent`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerOppgavePåVent(frist = 1.jan(2026))
        }

        // When
        medPersonISpeil {
            saksbehandlerLeggerOppgavePåVent(frist = 2.jan(2026))
        }

        // Then
        medPersonISpeil {
            assertHarOppgaveegenskap("PA_VENT")
            assertPåVent(frist = 2.jan(2026))
        }
    }

    @Test
    fun `saksbehandler fjerner oppgaven fra på vent`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerLeggerOppgavePåVent()
        }

        // When
        medPersonISpeil {
            saksbehandlerFjernerOppgaveFraPåVent()
        }

        // Then
        medPersonISpeil {
            assertHarIkkeOppgaveegenskap("PA_VENT")
            assertIkkePåVent()
        }
    }
}
