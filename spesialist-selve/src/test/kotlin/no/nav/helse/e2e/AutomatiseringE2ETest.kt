package no.nav.helse.e2e

import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import org.junit.jupiter.api.Test

internal class AutomatiseringE2ETest : AbstractE2ETest() {
    @Test
    fun `fatter automatisk vedtak`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistInnvilgerAutomatisk()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved warnings`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            regelverksvarsler = listOf("RV_IM_1"),
            kanGodkjennesAutomatisk = true,
        )

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `fatter ikke automatisk vedtak ved åpne oppgaver i gosys`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 1)
        håndterRisikovurderingløsning()

        assertGodkjenningsbehovIkkeBesvart()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
