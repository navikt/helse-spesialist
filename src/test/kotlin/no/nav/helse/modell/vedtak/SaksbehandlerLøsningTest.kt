package no.nav.helse.modell.vedtak

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class SaksbehandlerLøsningTest {
    private companion object {
        private val OPPGAVE_ID = nextLong()
        private const val GODKJENT = true
        private const val IDENT = "Z999999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val oppgave = Oppgave.avventerSaksbehandler("et navn", UUID.randomUUID())

    @Test
    fun `ferdigstiller oppgave`() {
        val godkjenningløsning = mockk<UtbetalingsgodkjenningMessage>(relaxed = true)
        val saksbehandlerLøsning = SaksbehandlerLøsning(GODKJENT, IDENT, SAKSBEHANDLER_OID, SAKSBEHANDLER_EPOST, GODKJENTTIDSPUNKT, null, null, null, OPPGAVE_ID)
        saksbehandlerLøsning.ferdigstillOppgave(oppgaveMediator, oppgave, godkjenningløsning)
        verify(exactly = 1) { oppgaveMediator.ferdigstill(oppgave, OPPGAVE_ID, IDENT, SAKSBEHANDLER_OID) }
        verify(exactly = 1) { godkjenningløsning.løs(true, IDENT, GODKJENTTIDSPUNKT, null, null, null) }
    }
}
