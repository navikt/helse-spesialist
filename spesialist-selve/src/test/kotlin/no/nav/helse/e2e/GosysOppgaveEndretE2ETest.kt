package no.nav.helse.e2e

import AbstractE2ETest
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
    fun `ber ikke om informasjon dersom det ikke finnes aktiv oppgave i Speil`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistInnvilgerAutomatisk()
        håndterGosysOppgaveEndret()
        assertIngenEtterspurteBehov()
    }
}
