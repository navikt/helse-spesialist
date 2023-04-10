package no.nav.helse.mediator

import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.Testdata
import no.nav.helse.modell.oppgave.OppgaveDao
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HendelseMediatorAttesteringTest {

    private val oppgaveDaoMock = mockk<OppgaveDao>(relaxed = true)
    private val mediatorWithMock = HendelseMediator(
        dataSource = AbstractDatabaseTest.dataSource,
        rapidsConnection = mockk(relaxed = true),
        oppgaveDao = oppgaveDaoMock,
        oppgaveMediator = mockk(),
        hendelsefabrikk = mockk(),
    )

    @Test
    fun `sjekker at saksbehandler ikke har lov å attestere beslutteroppgaven hvis saksbehandler sendte saken til godkjenning`() {
        val oppgaveId = 1L

        every { oppgaveDaoMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveDaoMock.finnTidligereSaksbehandler(oppgaveId) } returns Testdata.SAKSBEHANDLER_OID

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(
                oppgaveId,
                Testdata.SAKSBEHANDLER_OID
            )

        assertTrue(kanIkkeAttestere)
    }

    @Test
    fun `sjekker at saksbehandler har lov å attestere beslutteroppgaven hvis saksbehandler ikke sendte saken til godkjenning`() {
        val oppgaveId = 1L

        every { oppgaveDaoMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveDaoMock.finnTidligereSaksbehandler(oppgaveId) } returns UUID.randomUUID()

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(
                oppgaveId,
                Testdata.SAKSBEHANDLER_OID
            )

        assertFalse(kanIkkeAttestere)
    }

    @Test
    fun `sjekker at saksbehandler har lov å attestere beslutteroppgaven hvis tidligere saksbehandler ikke finnes`() {
        val oppgaveId = 1L

        every { oppgaveDaoMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveDaoMock.finnTidligereSaksbehandler(oppgaveId) } returns null

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(
                oppgaveId,
                Testdata.SAKSBEHANDLER_OID
            )

        assertFalse(kanIkkeAttestere)
    }

}
