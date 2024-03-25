package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import org.junit.jupiter.api.Test

internal class VedtaksperiodeForkastetE2ETest : AbstractE2ETest() {

    @Test
    fun `VedtaksperiodeForkastet oppdaterer ikke oppgave-tabellen dersom status er inaktiv`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()

        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
        håndterVedtaksperiodeForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
    }
    @Test
    fun `Markerer perioden som forkastet hvis vi mottar VedtaksperiodeForkastet fra spleis`() {
        fremTilSaksbehandleroppgave()

        håndterVedtaksperiodeForkastet()
        assertVedtaksperiodeForkastet(VEDTAKSPERIODE_ID)
    }
}
