package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import org.junit.jupiter.api.Test

internal class AutomatiseringE2ETest : AbstractE2ETestV2() {
    @Test
    fun `fatter automatisk vedtak`() {
        automatiskGodkjent()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        fremTilSaksbehandleroppgave(
            regelverksvarsler = listOf("RV_IM_1"),
            kanGodkjennesAutomatisk = true
        )

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved 8-4 ikke oppfylt`() {
        fremTilSaksbehandleroppgave(
            risikofunn = listOf(
                Risikofunn(kategori = listOf("8-4"), beskrivelse = "8-4 ikke ok", kreverSupersaksbehandler = false),
                Risikofunn(kategori = emptyList(), beskrivelse = "faresignaler ikke ok", kreverSupersaksbehandler = false)
            ),
            kanGodkjennesAutomatisk = false
        )
        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
