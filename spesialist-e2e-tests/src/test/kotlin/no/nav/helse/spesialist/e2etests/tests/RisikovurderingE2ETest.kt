package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import no.nav.helse.spesialist.e2etests.SpeilPersonReceiver
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import tools.jackson.databind.JsonNode

class RisikovurderingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `oppretter oppgave av type RISK_QA`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.funn = listOf(mapOf("kategori" to emptyList<Any>(), "beskrivelse" to "et faresignal"))

        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        medPersonISpeil {
            assertHarOppgaveegenskap("RISK_QA")
        }
    }

    @Test
    fun `oppretter oppgave av type SØKNAD`() {
        // Given:
        // risikovurderingBehovLøser.kanGodkjenneAutomatisk = true (standard, ingen RISK_QA)

        // When:
        søknadOgGodkjenningbehovKommerInn {
            aktivitetsloggNyAktivitet(listOf("RV_IV_1"))
        }

        // Then:
        medPersonISpeil {
            assertHarOppgaveegenskap("SOKNAD")
            assertHarIkkeOppgaveegenskap("RISK_QA")
        }
    }

    @Test
    fun `sender med kunRefusjon`() {
        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        val risikovurderingBehov = finnRisikovurderingBehov()
        assertTrue(risikovurderingBehov["Risikovurdering"]["kunRefusjon"].asBoolean())
    }

    @Test
    fun `sender med inntekt`() {
        // When:
        søknadOgGodkjenningbehovKommerInn()

        // Then:
        val risikovurderingBehov = finnRisikovurderingBehov()
        assertEquals("Arbeidsgiver", risikovurderingBehov["Risikovurdering"]["inntekt"]["inntektskilde"].asString())
        assertEquals(123456.7, risikovurderingBehov["Risikovurdering"]["inntekt"]["omregnetÅrsinntekt"].asDouble())
    }

    @Test
    fun `risikovurdering av annen vedtaksperiode i samme løsning gir varsel på den andre perioden`() {
        // Given:
        leggTilVedtaksperiode()
        val førsteVedtaksperiode = førsteVedtaksperiode()
        val andreVedtaksperiode = andreVedtaksperiode()
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = true
        risikovurderingBehovLøser.andreVedtaksperioder =
            listOf(
                mapOf(
                    "vedtaksperiodeId" to andreVedtaksperiode.vedtaksperiodeId,
                    "kanGodkjennesAutomatisk" to false,
                    "funn" to listOf(mapOf("kategori" to emptyList<Any>(), "beskrivelse" to "et faresignal på annen periode")),
                    "kontrollertOk" to emptyList<Any>(),
                ),
            )

        // When:
        personSenderSøknad()
        spleisForberederBehandling(førsteVedtaksperiode) {}
        spleisForberederBehandling(andreVedtaksperiode) {}
        spleisSenderGodkjenningsbehov(
            førsteVedtaksperiode,
            perioderMedSammeSkjæringstidspunkt = listOf(førsteVedtaksperiode, andreVedtaksperiode),
        )

        // Then:
        assertGodkjenningsbehovIkkeBesvart()
        medPersonISpeil {
            assertVarselkoder(emptyList(), førsteVedtaksperiode)
            assertVarselkoder(listOf("SB_RV_1"), andreVedtaksperiode)
            assertEquals(
                "et faresignal på annen periode",
                periode(andreVedtaksperiode)["risikovurdering"]["funn"][0]["beskrivelse"].asString(),
            )

            saksbehandlerTildelerSegSaken()
            // Varselet på den andre perioden må vurderes før vedtak for den første perioden kan fattes
            val feil = assertThrows<AssertionFailedError> { saksbehandlerFatterVedtak(førsteVedtaksperiode.spleisBehandlingId!!) }
            assertTrue(feil.message!!.contains("status 400"))
        }
        assertGodkjenningsbehovIkkeBesvart()

        medPersonISpeil {
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode.spleisBehandlingId!!)
        }
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = false)
    }

    @Test
    fun `forhåndslagret risikovurdering brukes når den andre vedtaksperioden kommer til godkjenning`() {
        // Given:
        leggTilVedtaksperiode()
        val førsteVedtaksperiode = førsteVedtaksperiode()
        val andreVedtaksperiode = andreVedtaksperiode()
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        risikovurderingBehovLøser.andreVedtaksperioder =
            listOf(
                mapOf(
                    "vedtaksperiodeId" to andreVedtaksperiode.vedtaksperiodeId,
                    "kanGodkjennesAutomatisk" to false,
                    "funn" to emptyList<Any>(),
                    "kontrollertOk" to emptyList<Any>(),
                ),
            )
        personSenderSøknad()
        spleisForberederBehandling(førsteVedtaksperiode) {}
        spleisForberederBehandling(andreVedtaksperiode) {}
        spleisSenderGodkjenningsbehov(
            førsteVedtaksperiode,
            perioderMedSammeSkjæringstidspunkt = listOf(førsteVedtaksperiode, andreVedtaksperiode),
        )
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode.spleisBehandlingId!!)
        }

        // When:
        spleisSenderGodkjenningsbehov(
            andreVedtaksperiode,
            perioderMedSammeSkjæringstidspunkt = listOf(førsteVedtaksperiode, andreVedtaksperiode),
        )

        // Then:
        assertEquals(1, alleRisikovurderingBehov().size)
        medPersonISpeil {
            assertVarselkoder(listOf("SB_RV_1"), andreVedtaksperiode)
            assertVarslerHarStatus("GODKJENT", andreVedtaksperiode.spleisBehandlingId!!)
        }
    }

    private fun SpeilPersonReceiver.periode(vedtaksperiode: Vedtaksperiode): JsonNode =
        person["arbeidsgivere"]
            .flatMap { arbeidsgiver -> arbeidsgiver["behandlinger"].flatMap { it["perioder"] } }
            .first { it["vedtaksperiodeId"].asString() == vedtaksperiode.vedtaksperiodeId.toString() }

    private fun alleRisikovurderingBehov(): List<JsonNode> =
        meldinger().filter { melding ->
            melding["@event_name"].asString() == "behov" &&
                melding["@behov"].any { it.asString() == "Risikovurdering" } &&
                melding["@løsning"] == null
        }

    private fun finnRisikovurderingBehov(): JsonNode =
        meldinger()
            .first { melding ->
                melding["@event_name"].asString() == "behov" &&
                    melding["@behov"].any { it.asString() == "Risikovurdering" }
            }
}
