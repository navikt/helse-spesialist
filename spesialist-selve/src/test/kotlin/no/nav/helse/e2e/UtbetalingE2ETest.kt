package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDateTime
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.OVERFØRT
import org.junit.jupiter.api.Test

internal class UtbetalingE2ETest : AbstractE2ETest() {

    @Test
    fun `utbetaling endret`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT)
        håndterUtbetalingEndret(utbetalingtype = "ETTERUTBETALING", gjeldendeStatus = OVERFØRT)
        håndterUtbetalingEndret(utbetalingtype = "ANNULLERING", gjeldendeStatus = ANNULLERT)
        assertUtbetalinger(UTBETALING_ID, 4)
    }

    @Test
    fun `utbetaling endret lagres ikke dobbelt`() {
        val opprettet = LocalDateTime.now()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = opprettet)
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = opprettet)
        assertUtbetalinger(UTBETALING_ID, 2)
    }

    @Test
    fun `tillater samme status ved et senere tidspunkt`() {
        val opprettet = LocalDateTime.now()
        val senereTidspunkt = opprettet.plusSeconds(10)
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = opprettet)
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = senereTidspunkt)
        assertUtbetalinger(UTBETALING_ID, 3)
    }

    @Test
    fun `lagrer utbetaling etter utbetaling_endret når utbetalingen har vært til godkjenning og vi kjenner arbeidsgiver`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        val utbetalingId = godkjenningsbehovTestdata.utbetalingId
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterUtbetalingEndret(forrigeStatus = IKKE_UTBETALT, gjeldendeStatus = GODKJENT)
        val utbetalingMeldingId = sisteMeldingId
        assertUtbetalinger(utbetalingId, 2)
        assertFeilendeMeldinger(0, utbetalingMeldingId)
    }

    @Test
    fun `utbetaling forkastet`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = FORKASTET, forrigeStatus = IKKE_GODKJENT)
        assertUtbetalinger(UTBETALING_ID, 2)
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = FORKASTET, forrigeStatus = GODKJENT)
        assertUtbetalinger(UTBETALING_ID, 3)
    }

    // Når spinnvill utfører avviksvurdering kan spleis rekke å forkaste utbetalingen og sende ut nytt godkjenningsbehov
    // før spesialist har mottatt det første godkjenningsbehovet
    @Test
    fun `ignorerer godkjenningsbehov for forkastet utbetaling`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet()
        håndterUtbetalingEndret(gjeldendeStatus = FORKASTET, forrigeStatus = IKKE_GODKJENT)

        håndterGodkjenningsbehovUtenValidering()
        assertIngenEtterspurteBehov()
    }

    @Test
    fun `feriepengeutbetalinger tas vare på`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet(utbetalingtype = "FERIEPENGER")
        håndterUtbetalingEndret(utbetalingtype = "FERIEPENGER")
        assertUtbetalinger(UTBETALING_ID, 2)
    }

    @Test
    fun `forstår utbetaling til bruker`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterUtbetalingOpprettet(utbetalingtype = "FERIEPENGER")
        håndterUtbetalingEndret(utbetalingtype = "FERIEPENGER", personbeløp = 1000)
        assertUtbetalinger(UTBETALING_ID, 2)
    }
}
