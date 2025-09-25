package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

class VedtakFattetE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler fatter vedtak etter hovedregel`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak()
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterHovedregel()
        assertSykepengegrunnlagfakta()
    }

    @Test
    fun `saksbehandler fatter vedtak med skjønnsfastsettelse`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerSkjønnsfastsetter830TredjeAvsnitt()
        }

        medPersonISpeil {
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerSenderTilGodkjenning()
        }

        beslutterMedPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" beslutter
            saksbehandlerFatterVedtak()
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterSkjønn()
    }

    @Test
    fun `Fjerner egenskap PÅ_VENT på oppgaven periode behandles`(){
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerLeggerOppgavePåVent()
        }

        fun hentAlleUnikeEgenskaper(person: JsonNode): List<String?> = person["arbeidsgivere"]
            .flatMap { arbeidsgiver ->
                arbeidsgiver["generasjoner"]
                    .first()["perioder"]
                    .flatMap { periode -> periode["egenskaper"].map { egenskap -> egenskap["egenskap"].asText() } }
            }.distinct()

        medPersonISpeil {
            val alleUnikeEgenskaper = hentAlleUnikeEgenskaper(person)
            assertTrue("PA_VENT" in alleUnikeEgenskaper)
            saksbehandlerFatterVedtak()
        }

        medPersonISpeil {
            val alleUnikeEgenskaper = hentAlleUnikeEgenskaper(person)
            assertFalse("PA_VENT" in alleUnikeEgenskaper)
        }
    }
}
