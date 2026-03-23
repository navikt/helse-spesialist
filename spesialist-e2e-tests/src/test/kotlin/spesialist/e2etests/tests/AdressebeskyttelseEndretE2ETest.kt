package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class AdressebeskyttelseEndretE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `oppdaterer adressebeskyttelse på en person vi kjenner til fra før`() {
        // Given:
        saksbehandlerHarRolle(Brukerrolle.Kode7)
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

    @Test
    fun `oppdaterer ikke adressebeskyttelse dersom personen er ukjent`() {
        // Given:
        saksbehandlerHarRolle(Brukerrolle.Kode7)
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

    @Test
    fun `oppdaterer adressebeskyttelse dersom personen er kjent, men forsøker ikke å forkaste periode`() {
        // Given:
        saksbehandlerHarRolle(Brukerrolle.Kode7)
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Ugradert"
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            assertAdressebeskyttelse("Ugradert")
            saksbehandlerTildelerSegSaken()
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // When:
        hentPersoninfoV2BehovLøser.adressebeskyttelse = "Fortrolig"
        detPubliseresEnAdressebeskyttelseEndretMelding()

        // Then:
        medPersonISpeil {
            assertAdressebeskyttelse("Fortrolig")
        }
        val vedtaksperiodeAvvistMelding = meldinger().find { it["@event_name"].asText() == "vedtaksperiode_avvist" }
        assertNull(vedtaksperiodeAvvistMelding)
    }
}
