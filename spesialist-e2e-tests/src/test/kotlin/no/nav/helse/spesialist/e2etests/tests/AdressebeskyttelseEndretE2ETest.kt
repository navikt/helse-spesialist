package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class AdressebeskyttelseEndretE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `oppdaterer adressebeskyttelse på en person vi kjenner til fra før`() {
        // Given:
        saksbehandlerHarTilgang(Tilgangstype.KODE7)
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Ugradert"
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            assertAdressebeskyttelse("Ugradert")
        }

        // When:
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Fortrolig"
        detPubliseresEnAdressebeskyttelseEndretMelding()

        // Then:
        medPersonISpeil {
            assertAdressebeskyttelse("Fortrolig")
        }
    }
}
