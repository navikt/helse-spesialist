package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import no.nav.helse.spesialist.e2etests.E2ETestApplikasjon
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Isolated("behovLøserStub.svarerPåBehov = false ødelegger for andre tester")
class GosysOppgaveEndretE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ber om informasjon om åpne oppgaver ved aktiv oppgave i Speil`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        detPubliseresEnGosysOppgaveEndretMelding()

        assertEquals("ÅpneOppgaver", sisteSendteBehovnavn())
    }

    @Test
    fun `ber ikke om informasjon dersom det ikke finnes aktiv oppgave i Speil`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = true
        søknadOgGodkjenningbehovKommerInn()

        detPubliseresEnGosysOppgaveEndretMelding()

        assertNotEquals("ÅpneOppgaver", sisteSendteBehovnavn())
    }

    @Test
    fun `avslutter kommandokjeden hvis oppgaven forsvant mens kommandokjeden var suspendert`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        E2ETestApplikasjon.behovLøserStub.svarerPåBehov = false
        detPubliseresEnGosysOppgaveEndretMelding()

        val commandContextId = meldinger().filter { it["command"]?.asText() == "GosysOppgaveEndretCommand" }.last{ it["@event_name"].asText() == "kommandokjede_suspendert" }["commandContextId"].asText()

        medPersonISpeil {
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(førsteVedtaksperiode().spleisBehandlingId!!, "En begrunnelse")
        }
        E2ETestApplikasjon.behovLøserStub.svarerPåBehov = true
        besvarBehovIgjen("ÅpneOppgaver")

        assertTrue(meldinger().filter { it["commandContextId"]?.asText() == commandContextId }.any { it["@event_name"].asText() == "kommandokjede_ferdigstilt" })
    }
}
