package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import org.junit.jupiter.api.Test
import java.util.UUID

internal class RevurderingE2ETest : AbstractE2ETest() {

    @Test
    fun `revurdering ved saksbehandlet oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler = listOf("RV_IM_1"))
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.Ferdigstilt)

        val utbetalingId2 = UUID.randomUUID()

        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertStoppknapp = true,
            harOppdatertMetadata = true,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = utbetalingId2),
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }

    @Test
    fun `revurdering av periode med negativt beløp medfører oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(kanGodkjennesAutomatisk = true)
        håndterVedtakFattet()

        val utbetalingId2 = UUID.randomUUID()

        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertStoppknapp = true,
            harOppdatertMetadata = true,
            kanGodkjennesAutomatisk = true,
            arbeidsgiverbeløp = -200,
            personbeløp = 0,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = utbetalingId2)
        )
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSaksbehandler)
    }
    @Test
    fun `revurdering av periode med positivt beløp og ingen varsler medfører ikke oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(kanGodkjennesAutomatisk = true)
        håndterVedtakFattet()

        val utbetalingId2 = UUID.randomUUID()

        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            harRisikovurdering = true,
            harOppdatertStoppknapp = true,
            harOppdatertMetadata = true,
            kanGodkjennesAutomatisk = true,
            arbeidsgiverbeløp = 200,
            personbeløp = 0,
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(utbetalingId = utbetalingId2)
        )
        assertSaksbehandleroppgaveBleIkkeOpprettet()
    }
}
