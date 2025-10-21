package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
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

    @Test
    fun `saksbehandler fatter vedtak etter hovedregel, lagrer korrekt data og sender ut forventede meldinger`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak("Her er min begrunnelse")

            // Sjekk at varsler er godkjent
            person["arbeidsgivere"].flatMap { arbeidsgiver ->
                arbeidsgiver["generasjoner"].flatMap { generasjon ->
                    generasjon["perioder"].flatMap { periode ->
                        periode["varsler"].map {
                            it["vurdering"]?.get("status")?.asText()
                        }
                    }
                }
            }.forEach { varselStatus ->
                assertEquals("GODKJENT", varselStatus)
            }

            // Sjekk at vedtaksbegrunnelse er lagret
            val vedtakBegrunnelse = person["arbeidsgivere"].flatMap { arbeidsgiver ->
                arbeidsgiver["generasjoner"].flatMap { generasjon ->
                    generasjon["perioder"].flatMap { periode ->
                        periode["vedtakBegrunnelser"]?.toList().orEmpty()
                    }
                }
            }.single()

            assertEquals("INNVILGELSE", vedtakBegrunnelse["utfall"].asText())
            assertEquals("Her er min begrunnelse", vedtakBegrunnelse["begrunnelse"].asText())
            assertEquals(saksbehandler.ident, vedtakBegrunnelse["saksbehandlerIdent"].asText())
        }

        // Then:
        // Sjekk at oppgaven har status "avventer system"
        assertOppgavestatus("AvventerSystem")

        val meldinger = meldinger()
        val sisteOppgaveOppdatert = meldinger.last { it["@event_name"].asText() == "oppgave_oppdatert" }
        assertEquals("AvventerSystem", sisteOppgaveOppdatert["tilstand"].asText())

        val varselEndret = meldinger.single { it["@event_name"].asText() == "varsel_endret" }
        assertEquals("VURDERT", varselEndret["forrige_status"].asText())
        assertEquals("GODKJENT", varselEndret["gjeldende_status"].asText())

        // Sjekk at saksbehandlerløsning blir publisert
        val saksbehandlerLøsning = meldinger.single { it["@event_name"].asText() == "saksbehandler_løsning" }
        assertEquals("true", saksbehandlerLøsning["godkjent"].asText())
        assertEquals(saksbehandler.ident, saksbehandlerLøsning["saksbehandlerident"].asText())
        assertEquals(saksbehandler.id().value.toString(), saksbehandlerLøsning["saksbehandleroid"].asText())
        assertEquals(saksbehandler.epost, saksbehandlerLøsning["saksbehandlerepost"].asText())
        assertEquals(emptyList<JsonNode>(), saksbehandlerLøsning["saksbehandleroverstyringer"].toList())
        assertEquals(saksbehandler.ident, saksbehandlerLøsning["saksbehandler"]["ident"].asText())
        assertEquals(saksbehandler.epost, saksbehandlerLøsning["saksbehandler"]["epostadresse"].asText())
        assertEquals(null, saksbehandlerLøsning["årsak"])
        assertEquals(null, saksbehandlerLøsning["begrunnelser"])
        assertEquals(null, saksbehandlerLøsning["kommentar"])
        assertEquals(null, saksbehandlerLøsning["beslutter"])
    }
}
