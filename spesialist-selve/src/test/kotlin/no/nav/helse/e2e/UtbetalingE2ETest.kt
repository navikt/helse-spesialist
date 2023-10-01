package no.nav.helse.e2e

import AbstractE2ETest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Testdata.UTBETALING_ID
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
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT)
        håndterUtbetalingEndret(utbetalingtype = "ETTERUTBETALING", gjeldendeStatus = OVERFØRT)
        håndterUtbetalingEndret(utbetalingtype = "ANNULLERING", gjeldendeStatus = ANNULLERT)
        assertUtbetalinger(UTBETALING_ID, 4)
    }

    @Test
    fun `utbetaling endret lagres ikke dobbelt`() {
        val opprettet = LocalDateTime.now()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = opprettet)
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = opprettet)
        assertUtbetalinger(UTBETALING_ID, 2)
    }

    @Test
    fun `tillater samme status ved et senere tidspunkt`() {
        val opprettet = LocalDateTime.now()
        val senereTidspunkt = opprettet.plusSeconds(10)
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = opprettet)
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = GODKJENT, opprettet = senereTidspunkt)
        assertUtbetalinger(UTBETALING_ID, 3)
    }

    @Test
    fun `utbetaling endret uten at vi kjenner arbeidsgiver`() {
        val ET_ANNET_ORGNR = "2"

        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(organisasjonsnummer = ET_ANNET_ORGNR)
        assertUtbetalinger(UTBETALING_ID, 0)
        assertFeilendeMeldinger(1)
    }

    @Test
    fun `lagrer utbetaling etter utbetaling_endret når utbetalingen har vært til godkjenning og vi kjenner arbeidsgiver`() {
        val utbetalingId = UUID.randomUUID()
        fremTilSaksbehandleroppgave(utbetalingId = utbetalingId)
        håndterSaksbehandlerløsning()
        håndterUtbetalingEndret(forrigeStatus = IKKE_UTBETALT, gjeldendeStatus = GODKJENT)
        assertUtbetalinger(utbetalingId, 2)
        assertFeilendeMeldinger(0)
    }

    @Test
    fun `utbetaling forkastet`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(utbetalingtype = "UTBETALING")
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = FORKASTET, forrigeStatus = IKKE_GODKJENT)
        assertUtbetalinger(UTBETALING_ID, 2)
        håndterUtbetalingEndret(utbetalingtype = "UTBETALING", gjeldendeStatus = FORKASTET, forrigeStatus = GODKJENT)
        assertUtbetalinger(UTBETALING_ID, 3)
    }

    @Test
    fun `feriepengeutbetalinger tas vare på`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(utbetalingtype = "FERIEPENGER")
        håndterUtbetalingEndret(utbetalingtype = "FERIEPENGER")
        assertUtbetalinger(UTBETALING_ID, 2)
    }

    @Test
    fun `forstår utbetaling til bruker`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterUtbetalingOpprettet(utbetalingtype = "FERIEPENGER")
        håndterUtbetalingEndret(utbetalingtype = "FERIEPENGER", personbeløp = 1000)
        assertUtbetalinger(UTBETALING_ID, 2)
    }
}
