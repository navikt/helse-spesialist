package no.nav.helse.spesialist.e2etests.tests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class RESTFattVedtakE2ETest : AbstractE2EIntegrationTest() {

    @Disabled
    @Test
    fun `saksbehandler fatter vedtak etter hovedregel`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtakREST(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterHovedregel()
        assertSykepengegrunnlagfakta()
    }

    @Disabled
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
            saksbehandlerFatterVedtakREST(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterSkjønn()
    }

    @Test
    fun `alle varsler blir godkjent når saksbehandler fatter vedtak`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        leggTilVedtaksperiode()
        personSenderSøknad()

        // Første periode
        val førsteVedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(førsteVedtaksperiode) {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // Andre periode
        val andreVedtaksperiode = andreVedtaksperiode()
        spleisForberederBehandling(andreVedtaksperiode) {
            aktivitetsloggNyAktivitet(listOf("RV_IV_2"))
        }

        spleisSenderGodkjenningsbehov(førsteVedtaksperiode)

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtakREST(førsteVedtaksperiode.spleisBehandlingId!!)
        }

        // Then:
        medPersonISpeil {
            assertVarslerHarStatus("GODKJENT", førsteVedtaksperiode.spleisBehandlingId!!)
            assertVarslerHarStatus("GODKJENT", andreVedtaksperiode.spleisBehandlingId!!)
        }
    }
    @Test
    fun `opphever på vent-status når vedtaket fattes`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        personSenderSøknad()

        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode) {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerLeggerOppgavePåVent()
            assertHarOppgaveegenskap("PA_VENT")
            saksbehandlerFatterVedtakREST(vedtaksperiode.spleisBehandlingId!!)
        }

        // Then:
        medPersonISpeil {
            assertHarIkkeOppgaveegenskap("PA_VENT")
        }
    }

    @Test
    fun `begrunnelse blir lagret`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        personSenderSøknad()

        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode) {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtakREST(vedtaksperiode.spleisBehandlingId!!, "Her er min begrunnelse")
        }

        // Then:
        medPersonISpeil {
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
    }

    @Disabled
    @Test
    fun `saksbehandler fatter vedtak etter hovedregel, lagrer korrekt data og sender ut forventede meldinger`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtakREST(UUID.randomUUID(), "Her er min begrunnelse")

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
