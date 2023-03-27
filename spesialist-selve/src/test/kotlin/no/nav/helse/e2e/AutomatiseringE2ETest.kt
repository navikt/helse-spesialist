package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import org.junit.jupiter.api.Test

internal class AutomatiseringE2ETest : AbstractE2ETestV2() {
    @Test
    fun `fatter automatisk vedtak`() {
        automatiskGodkjent()
        assertAutomatiskGodkjent()
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        fremTilSaksbehandleroppgave(regelverksvarsler = listOf("Brukeren har flere inntekter de siste tre måneder"), kanGodkjennesAutomatisk = true)

        assertIkkeGodkjent()
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
        assertIkkeGodkjent()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()

        assertIkkeGodkjent()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
