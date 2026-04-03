package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
}
