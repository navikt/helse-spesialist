package no.nav.helse.spesialist.e2etests.tests

import com.github.navikt.tbd_libs.jackson.asLocalDateTime
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.application.testing.assertMindreEnnNSekunderSiden
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FattVedtakE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler fatter vedtak etter hovedregel`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
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
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterSkjønn()
    }

    @Test
    fun `Velger nyeste skjønnsmessig fastsettelse når melding om vedtak sendes ut`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerSkjønnsfastsetter830TredjeAvsnitt("En begrunnelse")
        }

        medPersonISpeil {
            saksbehandlerSkjønnsfastsetter830TredjeAvsnitt("En annen begrunnelse")
        }

        medPersonISpeil {
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerSenderTilGodkjenning()
        }

        beslutterMedPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" beslutter
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterSkjønn("En annen begrunnelse")
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
            saksbehandlerFatterVedtak(førsteVedtaksperiode.spleisBehandlingId!!)
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
            saksbehandlerFatterVedtak(vedtaksperiode.spleisBehandlingId!!)
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
            saksbehandlerFatterVedtak(vedtaksperiode.spleisBehandlingId!!, "Her er min begrunnelse")
        }

        // Then:
        medPersonISpeil {
            // Sjekk at vedtaksbegrunnelse er lagret
            val vedtakBegrunnelse =
                person["arbeidsgivere"]
                    .flatMap { arbeidsgiver ->
                        arbeidsgiver["generasjoner"].flatMap { generasjon ->
                            generasjon["perioder"].flatMap { periode ->
                                periode["vedtakBegrunnelser"]?.toList().orEmpty()
                            }
                        }
                    }.single()

            assertEquals("INNVILGELSE", vedtakBegrunnelse["utfall"].asText())
            assertEquals("Her er min begrunnelse", vedtakBegrunnelse["begrunnelse"].asText())
            assertEquals(saksbehandler.ident.value, vedtakBegrunnelse["saksbehandlerIdent"].asText())
        }
    }

    @Test
    fun `beslutter godkjenner totrinnsvurdering`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        personSenderSøknad()

        val vedtaksperiode = førsteVedtaksperiode()
        spleisForberederBehandling(vedtaksperiode) {}

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerLeggerTilTilkommenInntekt(
                organisasjonsnummer = "999999999",
                periode = vedtaksperiode.fom tilOgMed vedtaksperiode.tom,
                periodebeløp = BigDecimal.valueOf(10000.0),
                ekskluderteUkedager = emptyList(),
                notatTilBeslutter = "Et notat",
            )
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerSenderTilGodkjenning("Her er min begrunnelse")
        }

        beslutterMedPersonISpeil {
            saksbehandlerFatterVedtak(vedtaksperiode.spleisBehandlingId!!, "Her er min begrunnelse")
        }

        // Then:
        medPersonISpeil {
            // Sjekk at vedtaksbegrunnelse er lagret
            val vedtakBegrunnelse =
                person["arbeidsgivere"]
                    .flatMap { arbeidsgiver ->
                        arbeidsgiver["generasjoner"].flatMap { generasjon ->
                            generasjon["perioder"].flatMap { periode ->
                                periode["vedtakBegrunnelser"]?.toList().orEmpty()
                            }
                        }
                    }.single()

            assertEquals("INNVILGELSE", vedtakBegrunnelse["utfall"].asText())
            assertEquals("Her er min begrunnelse", vedtakBegrunnelse["begrunnelse"].asText())
            assertEquals(saksbehandler.ident.value, vedtakBegrunnelse["saksbehandlerIdent"].asText())

            val historikkinnslag =
                person["arbeidsgivere"].flatMap { arbeidsgiver ->
                    arbeidsgiver["generasjoner"].flatMap { generasjon ->
                        generasjon["perioder"].flatMap { periode ->
                            periode["historikkinnslag"]?.toList().orEmpty()
                        }
                    }
                }
            assertContains(historikkinnslag.map { it["type"].asText() }, "TOTRINNSVURDERING_ATTESTERT")
        }

        assertGjeldendeOppgavestatus("Ferdigstilt")

        val meldinger = meldinger()
        val nestSisteOppgaveOppdatert = meldinger.filter { it["@event_name"].asText() == "oppgave_oppdatert" }.dropLast(1).last()
        val sisteOppgaveOppdatert = meldinger.last { it["@event_name"].asText() == "oppgave_oppdatert" }
        assertEquals("AvventerSystem", nestSisteOppgaveOppdatert["tilstand"].asText())
        assertEquals("Ferdigstilt", sisteOppgaveOppdatert["tilstand"].asText())

        val varselEndret = meldinger.single { it["@event_name"].asText() == "varsel_endret" }
        assertEquals("VURDERT", varselEndret["forrige_status"].asText())
        assertEquals("GODKJENT", varselEndret["gjeldende_status"].asText())

        // Sjekk at saksbehandlerløsning ikke blir publisert
        assertTrue(meldinger.none { it["@event_name"].asText() == "saksbehandler_løsning" })

        // Sjekk at løsning på godkjenningsbehov ble publisert
        val godkjenningsbehovLøsning = meldinger.single { it["@event_name"].asText() == "behov" && it["@behov"].first().asText() == "Godkjenning" && it["@løsning"] != null }
        assertJsonEquals(
            """
            {
              "Godkjenning": {
                "godkjent": true,
                "saksbehandlerIdent": "${saksbehandler.ident.value}",
                "saksbehandlerEpost": "${saksbehandler.epost}",
                "automatiskBehandling": false,
                "saksbehandleroverstyringer": []
              }
            }
            """.trimIndent(),
            godkjenningsbehovLøsning["@løsning"],
            "Godkjenning.godkjenttidspunkt",
        )
        assertMindreEnnNSekunderSiden(30, godkjenningsbehovLøsning["@løsning"]["Godkjenning"]["godkjenttidspunkt"].asLocalDateTime())

        // Sjekk at vedtaksperiode_godkjent ble publisert
        val vedtaksperiodeGodkjent = meldinger.single { it["@event_name"].asText() == "vedtaksperiode_godkjent" }
        assertJsonEquals(
            """
            {
              "@event_name": "vedtaksperiode_godkjent",
              "fødselsnummer": "${testContext.person.fødselsnummer}",
              "vedtaksperiodeId": "${førsteVedtaksperiode().vedtaksperiodeId}",
              "periodetype": "FØRSTEGANGSBEHANDLING",
              "saksbehandlerIdent": "${saksbehandler.ident.value}",
              "saksbehandlerEpost": "${saksbehandler.epost}",
              "automatiskBehandling": false,
              "saksbehandler": {
                "ident": "${saksbehandler.ident.value}",
                "epostadresse": "${saksbehandler.epost}"
              },
              "beslutter": {
                "ident": "${beslutter.ident.value}",
                "epostadresse": "${beslutter.epost}"
              },
              "behandlingId": "${førsteVedtaksperiode().spleisBehandlingId}",
              "yrkesaktivitetstype": "ARBEIDSTAKER"
            }
            """.trimIndent(),
            vedtaksperiodeGodkjent,
            "@id",
            "@opprettet",
            "system_read_count",
            "system_participating_services",
            "@forårsaket_av",
        )
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
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!, "Her er min begrunnelse")

            // Sjekk at varsler er godkjent
            person["arbeidsgivere"]
                .flatMap { arbeidsgiver ->
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
            val vedtakBegrunnelse =
                person["arbeidsgivere"]
                    .flatMap { arbeidsgiver ->
                        arbeidsgiver["generasjoner"].flatMap { generasjon ->
                            generasjon["perioder"].flatMap { periode ->
                                periode["vedtakBegrunnelser"]?.toList().orEmpty()
                            }
                        }
                    }.single()

            assertEquals("INNVILGELSE", vedtakBegrunnelse["utfall"].asText())
            assertEquals("Her er min begrunnelse", vedtakBegrunnelse["begrunnelse"].asText())
            assertEquals(saksbehandler.ident.value, vedtakBegrunnelse["saksbehandlerIdent"].asText())
        }

        // Then:
        assertGjeldendeOppgavestatus("Ferdigstilt")

        val meldinger = meldinger()
        val nestSisteOppgaveOppdatert = meldinger.filter { it["@event_name"].asText() == "oppgave_oppdatert" }.dropLast(1).last()
        val sisteOppgaveOppdatert = meldinger.last { it["@event_name"].asText() == "oppgave_oppdatert" }
        assertEquals("AvventerSystem", nestSisteOppgaveOppdatert["tilstand"].asText())
        assertEquals("Ferdigstilt", sisteOppgaveOppdatert["tilstand"].asText())

        val varselEndret = meldinger.single { it["@event_name"].asText() == "varsel_endret" }
        assertEquals("VURDERT", varselEndret["forrige_status"].asText())
        assertEquals("GODKJENT", varselEndret["gjeldende_status"].asText())

        // Sjekk at saksbehandlerløsning ikke blir publisert
        assertTrue(meldinger.none { it["@event_name"].asText() == "saksbehandler_løsning" })

        // Sjekk at løsning på godkjenningsbehov ble publisert
        val godkjenningsbehovLøsning = meldinger.single { it["@event_name"].asText() == "behov" && it["@behov"].first().asText() == "Godkjenning" && it["@løsning"] != null }
        assertJsonEquals(
            """
            {
              "Godkjenning": {
                "godkjent": true,
                "saksbehandlerIdent": "${saksbehandler.ident.value}",
                "saksbehandlerEpost": "${saksbehandler.epost}",
                "automatiskBehandling": false,
                "saksbehandleroverstyringer": []
              }
            }
            """.trimIndent(),
            godkjenningsbehovLøsning["@løsning"],
            "Godkjenning.godkjenttidspunkt",
        )
        assertMindreEnnNSekunderSiden(30, godkjenningsbehovLøsning["@løsning"]["Godkjenning"]["godkjenttidspunkt"].asLocalDateTime())

        // Sjekk at vedtaksperiode_godkjent ble publisert
        val vedtaksperiodeGodkjent = meldinger.single { it["@event_name"].asText() == "vedtaksperiode_godkjent" }
        assertJsonEquals(
            """
            {
              "@event_name": "vedtaksperiode_godkjent",
              "fødselsnummer": "${testContext.person.fødselsnummer}",
              "vedtaksperiodeId": "${førsteVedtaksperiode().vedtaksperiodeId}",
              "periodetype": "FØRSTEGANGSBEHANDLING",
              "saksbehandlerIdent": "${saksbehandler.ident.value}",
              "saksbehandlerEpost": "${saksbehandler.epost}",
              "automatiskBehandling": false,
              "saksbehandler": {
                "ident": "${saksbehandler.ident.value}",
                "epostadresse": "${saksbehandler.epost}"
              },
              "behandlingId": "${førsteVedtaksperiode().spleisBehandlingId}",
              "yrkesaktivitetstype": "ARBEIDSTAKER"
            }
            """.trimIndent(),
            vedtaksperiodeGodkjent,
            "@id",
            "@opprettet",
            "system_read_count",
            "system_participating_services",
            "@forårsaket_av",
        )
    }

    @ParameterizedTest
    @CsvSource("Innvilget,Innvilgelse", "DelvisInnvilget,DelvisInnvilgelse", "Avslag,Avslag")
    fun `fatter vedtak med utfall innvilgelse basert på tags fra Spleis`(
        tag: String,
        utfall: VedtakFattetMelding.BegrunnelseType,
    ) {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn(tags = listOf(tag))

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterHovedregel(utfall)
        assertSykepengegrunnlagfakta()
    }
}
