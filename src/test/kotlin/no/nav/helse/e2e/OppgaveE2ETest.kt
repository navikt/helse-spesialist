package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.modell.oppgave.Oppgavestatus.*
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class OppgaveE2ETest: AbstractE2ETest() {

    private companion object {
        private val ORGANISASJONSNUMMER = "123456789"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val FAGSYSTEM_ID = "AAAAABBBBBCCCCCEEEEE"
    }

    @Test
    fun `invaliderer oppgave når utbetalingen har blitt forkastet`() {
        vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID)
        assertOppgave(0, AvventerSaksbehandler, Invalidert)
    }

    @Test
    fun `oppretter ny oppgave når det finnes en invalidert oppgave for en vedtaksperiode`() {
        vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID)
        vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        assertOppgave(0, AvventerSaksbehandler, Invalidert)
        assertOppgave(1, AvventerSaksbehandler)
    }

    @Test
    fun `oppretter ikke ny oppgave når det finnes en aktiv oppgave`() {
        vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        val behov2 = sendGodkjenningsbehov(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, UUID.randomUUID())
        assertOppgave(0, AvventerSaksbehandler)
        assertOppgaver(1)
        assertIkkeHendelse(behov2)
    }

    @Test
    fun `oppretter ny oppgave når saksbehandler har godkjent, men spleis har reberegnet i mellomtiden`() {
        val behov1 = vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        sendSaksbehandlerløsning(testRapid.inspektør.oppgaveId(behov1).toLong(), "ident", "epost", UUID.randomUUID(), true)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID)
        vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        assertOppgave(0, AvventerSaksbehandler, AvventerSystem, Invalidert)
        assertOppgave(1, AvventerSaksbehandler)
    }

    @Disabled("Må støtte å kunne invalidere et automatisert vedtak")
    @Test
    fun `håndterer nytt godkjenningsbehov om vi har automatisk godkjent en periode men spleis har reberegnet i mellomtiden`() {
        vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID,true)
        sendUtbetalingEndret("UTBETALING", FORKASTET, ORGANISASJONSNUMMER, FAGSYSTEM_ID)
        val behov2 = vedtaksperiode(ORGANISASJONSNUMMER, VEDTAKSPERIODE_ID, false)
        assertHendelse(behov2)
        assertOppgave(0, AvventerSaksbehandler)
        assertOppgaver(1)
    }
}
