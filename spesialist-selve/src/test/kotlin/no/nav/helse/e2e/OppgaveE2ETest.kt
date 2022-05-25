package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.oppgave.Oppgave
import no.nav.helse.oppgave.Oppgavestatus
import no.nav.helse.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.oppgave.Oppgavestatus.Invalidert
import no.nav.helse.oppgave.Oppgavetype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppgaveE2ETest: AbstractE2ETest() {

    private companion object {
        private val ORGANISASJONSNUMMER = "123456789"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val FAGSYSTEM_ID = "AAAAABBBBBCCCCCEEEEE"
    }

    @Test
    fun `invaliderer oppgave når utbetalingen har blitt forkastet`() {
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID, utbetalingId = UTBETALING_ID)
        assertOppgavestatuser(0, AvventerSaksbehandler, Invalidert)
    }

    @Test
    fun `invaliderer oppgave når utbetaling som har status IKKE_UTBETALT blir forkastet`() {
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(), "ident", "ident@nav.no", UUID.randomUUID(), true)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID, utbetalingId = UTBETALING_ID, forrigeStatus = IKKE_UTBETALT)
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem, Invalidert)
    }

    @Test
    fun `ferdigstiller oppgaven først når utbetalingen er utbetalt`() {
        val oid = UUID.randomUUID()
        val godkjenningsbehov = vedtaksperiode(
            FØDSELSNUMMER,
            ORGANISASJONSNUMMER,
            VEDTAKSPERIODE_ID,
            false,
            utbetalingId = UTBETALING_ID
        )
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(godkjenningsbehov).toLong(), "ident", "epost", oid, true)
        sendUtbetalingEndret("UTBETALING", UTBETALT, ORGANISASJONSNUMMER, FAGSYSTEM_ID, utbetalingId = UTBETALING_ID)
        val oppgave = oppgaveDao.finn(UTBETALING_ID)
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem, Ferdigstilt)
        assertOppgavedetaljer(oppgave, Ferdigstilt, Oppgavetype.SØKNAD, "ident", oid, UTBETALING_ID, godkjenningsbehov, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter ny oppgave når det finnes en invalidert oppgave for en vedtaksperiode`() {
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID, utbetalingId = UTBETALING_ID)
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        assertOppgavestatuser(0, AvventerSaksbehandler, Invalidert)
        assertOppgavestatuser(1, AvventerSaksbehandler)
    }

    @Test
    fun `oppretter ikke ny oppgave når det finnes en aktiv oppgave`() {
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        val behov2 = sendGodkjenningsbehov(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, UTBETALING_ID)
        assertOppgavestatuser(0, AvventerSaksbehandler)
        assertOppgaver(1)
        assertIkkeHendelse(behov2)
    }

    @Test
    fun `oppretter ny oppgave når saksbehandler har godkjent, men spleis har reberegnet i mellomtiden`() {
        val behov1 = vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(behov1).toLong(), "ident", "epost", UUID.randomUUID(), true)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID, utbetalingId = UTBETALING_ID)
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false, utbetalingId = UTBETALING_ID)
        assertOppgavestatuser(0, AvventerSaksbehandler, AvventerSystem, Invalidert)
        assertOppgavestatuser(1, AvventerSaksbehandler)
    }

    @Test
    fun `håndterer nytt godkjenningsbehov om vi har automatisk godkjent en periode men spleis har reberegnet i mellomtiden`() {
        vedtaksperiode(FØDSELSNUMMER, ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, true, utbetalingId = UTBETALING_ID)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID, utbetalingId = UTBETALING_ID)
        val behov = sendGodkjenningsbehov(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, UTBETALING_ID2)
        assertHendelse(behov)
    }

    private fun assertOppgavedetaljer(oppgave: Oppgave?, status: Oppgavestatus, type: Oppgavetype, ferdigstiltAv: String, ferdigstiltAvOid: UUID, utbetalingId: UUID, hendelseId: UUID, vedtaksperiodeId: UUID) {
        assertEquals(Oppgave(testRapid.inspektør.oppgaveId(hendelseId).toLong(), type, status, vedtaksperiodeId, ferdigstiltAv, ferdigstiltAvOid, utbetalingId), oppgave)
    }
}
