package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test

internal class GosysOppgaveEndretE2ETest : AbstractE2ETest() {
    @Test
    fun `ber om informasjon om åpne oppgaver ved aktiv oppgave i Speil`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterGosysOppgaveEndret()
        assertSisteEtterspurteBehov("ÅpneOppgaver")
    }

    @Test
    fun `Legg til varsel ved åpne oppgaver i Gosys`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterGosysOppgaveEndret()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 1)
        assertVarsel(VEDTAKSPERIODE_ID, "SB_EX_1")
    }

    @Test
    fun `ber ikke om informasjon dersom det ikke finnes aktiv oppgave i Speil`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistInnvilgerAutomatisk()
        håndterGosysOppgaveEndret()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `fatter vedtak automatisk ved åpen oppgave i Speil men ikke lenger åpen oppgave i Gosys`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(
            antallÅpneOppgaverIGosys = 1,
        )
        håndterRisikovurderingløsning()
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
        håndterGosysOppgaveEndret()

        håndterÅpneOppgaverløsning(
            antallÅpneOppgaverIGosys = 0,
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.Invalidert)
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = true)
    }
}
