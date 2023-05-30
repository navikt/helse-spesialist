package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test

internal class RevurderingE2ETest : AbstractE2ETestV2() {

    @Test
    fun `revurdering ved saksbehandlet oppgave`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave(regelverksvarsler = listOf("RV_IM_1"))
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.Ferdigstilt)

        val utbetalingId2 = UUID.randomUUID()

        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, utbetalingId = utbetalingId2, harRisikovurdering = true)
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `revurdering av periode med negativt beløp medfører oppgave`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave(kanGodkjennesAutomatisk = true)
        håndterVedtakFattet()

        val utbetalingId2 = UUID.randomUUID()

        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            kanGodkjennesAutomatisk = true,
            utbetalingId = utbetalingId2,
            arbeidsgiverbeløp = -200,
            personbeløp = 0
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }
    @Test
    fun `revurdering av periode med positivt beløp og ingen varsler medfører ikke oppgave`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave(kanGodkjennesAutomatisk = true)
        håndterVedtakFattet()

        val utbetalingId2 = UUID.randomUUID()

        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            kanGodkjennesAutomatisk = true,
            utbetalingId = utbetalingId2,
            arbeidsgiverbeløp = 200,
            personbeløp = 0
        )
        assertIkkeSaksbehandleroppgave()
    }
}
