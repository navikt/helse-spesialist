package no.nav.helse.e2e

import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetE2ETest : AbstractE2ETest() {

    @Test
    fun `VedtaksperiodeForkastet oppdaterer ikke oppgave-tabellen dersom status er inaktiv`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterAvsluttetMedVedtak()

        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
        håndterVedtaksperiodeForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
    }
    @Test
    fun `Markerer perioden som forkastet hvis vi mottar VedtaksperiodeForkastet fra spleis`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        håndterVedtaksperiodeForkastet()
        assertVedtaksperiodeForkastet(VEDTAKSPERIODE_ID)
    }
}
