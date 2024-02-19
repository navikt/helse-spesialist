package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Invalidert
import org.junit.jupiter.api.Test

internal class OppgaveE2ETest: AbstractE2ETest() {

    @Test
    fun `invaliderer oppgave når utbetalingen har blitt forkastet`() {
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
    }

    @Test
    fun `invaliderer oppgave når utbetaling som har status IKKE_UTBETALT blir forkastet`() {
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet(forrigeStatus = IKKE_UTBETALT)
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
    }

    @Test
    fun `ferdigstiller oppgaven først når utbetalingen er utbetalt`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtbetalingUtbetalt()
        assertSaksbehandleroppgave(oppgavestatus = Ferdigstilt)
    }

    @Test
    fun `oppretter ny oppgave når det finnes en invalidert oppgave for en vedtaksperiode`() {
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `oppretter ikke ny oppgave når det finnes en aktiv oppgave`() {
        fremTilSaksbehandleroppgave()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
        håndterGodkjenningsbehovUtenValidering()
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `oppretter ny oppgave når saksbehandler har godkjent, men spleis har reberegnet i mellomtiden`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }

    @Test
    fun `håndterer nytt godkjenningsbehov om vi har automatisk godkjent en periode men spleis har reberegnet i mellomtiden`() {
        fremTilSaksbehandleroppgave()
        håndterUtbetalingForkastet()
        assertSaksbehandleroppgave(oppgavestatus = Invalidert)
        fremTilSaksbehandleroppgave(
            harOppdatertMetadata = true,
            harRisikovurdering = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = UUID.randomUUID()),
        )
        assertSaksbehandleroppgave(oppgavestatus = AvventerSaksbehandler)
    }
}
