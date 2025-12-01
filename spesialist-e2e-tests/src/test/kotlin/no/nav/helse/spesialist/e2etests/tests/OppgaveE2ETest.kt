package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Disabled
import kotlin.test.Test

class OppgaveE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `invaliderer oppgave når utbetalingen har blitt forkastet`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // When
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerKasterUtSaken(førsteVedtaksperiode().spleisBehandlingId!!, "En årsak", listOf("Begrunnelser"))
        }

        // Then:
        medPersonISpeil {
            assertPeriodeHarIkkeOppgave()
            assertGjeldendeOppgavestatus("Invalidert")
        }
    }

    @Test
    fun `oppretter ny oppgave når det finnes en invalidert oppgave for en vedtaksperiode`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // When
        spleisReberegnerAutomatisk(førsteVedtaksperiode())
        spleisSenderGodkjenningsbehov(førsteVedtaksperiode())

        // Then:
        assertOppgavestatuserKronoligisk("Invalidert", "AvventerSaksbehandler")
    }

    @Test
    fun `oppretter ikke ny oppgave når det finnes en aktiv oppgave`() {
        // Given
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // When
        spleisSenderGodkjenningsbehov(førsteVedtaksperiode())

        // Then:
        assertOppgavestatuserKronoligisk("AvventerSaksbehandler")
    }

    @Disabled("Hvordan kan vi teste eller støtte dette så snart vi sender melding om vedtak direkte? - scenario kopiert fra gammel testløype")
    @Test
    fun `oppretter ny oppgave når saksbehandler har godkjent, men spleis har reberegnet i mellomtiden`() {}

    @Disabled("Hvordan kan vi teste eller støtte dette så snart vi sender melding om vedtak direkte? - scenario kopiert fra gammel testløype")
    @Test
    fun `håndterer nytt godkjenningsbehov om vi har automatisk godkjent en periode men spleis har reberegnet i mellomtiden`() {}
}
