package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import kotlin.test.Test

class TildelingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler tildeler seg oppgaven`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // When
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
        }

        // Then
        medPersonISpeil {
            assertOppgaveTildeltSaksbehandlerEvent()
            assertTildelt()
        }
    }

    @Test
    fun `saksbehandler avmelder seg oppgaven`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
        }

        // When
        medPersonISpeil {
            saksbehandlerAvmelderSegSaken()
        }

        // Then
        medPersonISpeil {
            assertOppgaveIkkeTildeltEvent()
            assertIkkeTildelt()
        }
    }
}
