package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdressebeskyttelseEndretE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `oppdaterer adressebeskyttelse dersom personen er kjent, men forsøker ikke å forkaste periode`() {
        // Given:
        saksbehandlerHarRolle(Brukerrolle.Kode7)
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Ugradert"
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // When:
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Fortrolig"
        detPubliseresEnAdressebeskyttelseEndretMelding()

        // Then:
        assertTrue(meldinger().none { it["@event_name"].asString() == "vedtaksperiode_avvist" })
    }
}
