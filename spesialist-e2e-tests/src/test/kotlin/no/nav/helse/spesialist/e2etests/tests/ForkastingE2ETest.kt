package no.nav.helse.spesialist.e2etests.tests

import com.github.navikt.tbd_libs.jackson.asLocalDateTime
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.application.testing.assertMindreEnnNSekunderSiden
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForkastingE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `saksbehandler fatter vedtak etter hovedregel, lagrer korrekt data og sender ut forventede meldinger`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerKasterUtSaken(førsteVedtaksperiode().spleisBehandlingId!!, "Her er en årsak", begrunnelser = listOf("Mangler støtte"), kommentar = "ingen kommentar")
        }

        // Then:
        assertOppgavestatus("Invalidert")

        val meldinger = meldinger()
        val nestSisteOppgaveOppdatert = meldinger.filter { it["@event_name"].asText() == "oppgave_oppdatert" }.dropLast(1).last()
        val sisteOppgaveOppdatert = meldinger.last { it["@event_name"].asText() == "oppgave_oppdatert" }
        assertEquals("AvventerSystem", nestSisteOppgaveOppdatert["tilstand"].asText())
        assertEquals("Invalidert", sisteOppgaveOppdatert["tilstand"].asText())

        val varselEndret = meldinger.single { it["@event_name"].asText() == "varsel_endret" }
        assertEquals("VURDERT", varselEndret["forrige_status"].asText())
        assertEquals("AVVIST", varselEndret["gjeldende_status"].asText())

        // Sjekk at saksbehandlerløsning ikke blir publisert
        assertTrue(meldinger.none { it["@event_name"].asText() == "saksbehandler_løsning" })

        // Sjekk at løsning på godkjenningsbehov ble publisert
        val godkjenningsbehovLøsning = meldinger.single { it["@event_name"].asText() == "behov" && it["@behov"].first().asText() == "Godkjenning" && it["@løsning"] != null }
        assertJsonEquals(
            """
                {
                  "Godkjenning": {
                    "godkjent": false,
                    "saksbehandlerIdent": "${saksbehandler.ident}",
                    "saksbehandlerEpost": "${saksbehandler.epost}",
                    "automatiskBehandling": false,
                    "årsak" : "Her er en årsak",
                    "begrunnelser" : ["Mangler støtte"],
                    "kommentar" : "ingen kommentar",
                    "saksbehandleroverstyringer": []
                  }
                }
            """.trimIndent(),
            godkjenningsbehovLøsning["@løsning"],
            "Godkjenning.godkjenttidspunkt"
        )
        assertMindreEnnNSekunderSiden(30, godkjenningsbehovLøsning["@løsning"]["Godkjenning"]["godkjenttidspunkt"].asLocalDateTime())

        // Sjekk at vedtaksperiode_avvist ble publisert
        val vedtaksperiodeAvvist = meldinger.single { it["@event_name"].asText() == "vedtaksperiode_avvist" }
        assertJsonEquals(
            """
                {
                  "@event_name": "vedtaksperiode_avvist",
                  "fødselsnummer": "${testContext.person.fødselsnummer}",
                  "vedtaksperiodeId": "${førsteVedtaksperiode().vedtaksperiodeId}",
                  "saksbehandlerIdent": "${saksbehandler.ident}",
                  "saksbehandlerEpost": "${saksbehandler.epost}",
                  "saksbehandler": {
                    "ident": "${saksbehandler.ident}",
                    "epostadresse": "${saksbehandler.epost}"
                  },
                  "automatiskBehandling": false,
                  "årsak" : "Her er en årsak",
                  "begrunnelser" : ["Mangler støtte"],
                  "kommentar" : "ingen kommentar",
                  "periodetype": "FØRSTEGANGSBEHANDLING",
                  "behandlingId": "${førsteVedtaksperiode().spleisBehandlingId}",
                  "yrkesaktivitetstype": "ARBEIDSTAKER"
                }
            """.trimIndent(),
            vedtaksperiodeAvvist,
            "@id",
            "@opprettet",
            "system_read_count",
            "system_participating_services",
            "@forårsaket_av"
        )
    }

    @Test
    fun `Forkasting endrer ikke status på oppgave dersom den allerede er ferdigstilt`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!)
        }

        // When:
        spleisKasterUtSaken(førsteVedtaksperiode())

        // Then:
        assertOppgavestatus("Ferdigstilt")
    }

    @Test
    fun `Hvis spleis forkaster perioden, blir den forkastet i Spesialist også`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        // When:
        spleisKasterUtSaken(førsteVedtaksperiode())

        // Then:
        assertOppgavestatus("Invalidert")
        assertPeriodeForkastet(true)
    }
}
