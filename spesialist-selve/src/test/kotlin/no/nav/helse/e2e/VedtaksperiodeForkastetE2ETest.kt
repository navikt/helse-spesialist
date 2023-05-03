package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetE2ETest : AbstractE2ETestV2() {

    @Test
    fun `VedtaksperiodeForkastet oppdaterer ikke oppgave-tabellen dersom status er inaktiv`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
        håndterVedtaksperiodeForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
    }
}
