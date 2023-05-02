package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Invalidert
import org.junit.jupiter.api.Test

internal class OppgaveE2ETest: AbstractE2ETestV2() {

    @Test
    fun `invaliderer oppgave når utbetalingen har blitt forkastet`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
    }

    @Test
    fun `invaliderer oppgave når utbetaling som har status IKKE_UTBETALT blir forkastet`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet(forrigeStatus = IKKE_UTBETALT)
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
    }

    @Test
    fun `ferdigstiller oppgaven først når utbetalingen er utbetalt`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtbetalingUtbetalt()
        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
    }

    @Test
    fun `oppretter ny oppgave når det finnes en invalidert oppgave for en vedtaksperiode`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `oppretter ikke ny oppgave når det finnes en aktiv oppgave`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
        håndterGodkjenningsbehovUtenValidering()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `oppretter ny oppgave når saksbehandler har godkjent, men spleis har reberegnet i mellomtiden`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `håndterer nytt godkjenningsbehov om vi har automatisk godkjent en periode men spleis har reberegnet i mellomtiden`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        fremTilSaksbehandleroppgave(harOppdatertMetadata = true, harRisikovurdering = true)
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
