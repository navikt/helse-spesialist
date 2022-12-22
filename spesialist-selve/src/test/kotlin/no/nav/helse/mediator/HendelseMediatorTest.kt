package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.modell.oppgave.OppgaveMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HendelseMediatorTest : AbstractE2ETest() {

    private val mediator = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        opptegnelseDao = opptegnelseDao,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk
    )

    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val mediatorWithMock = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        opptegnelseDao = opptegnelseDao,
        oppgaveMediator = oppgaveMediatorMock,
        hendelsefabrikk = hendelsefabrikk
    )

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        mediator.håndter(GodkjenningDTO(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid)
        assertTrue(testRapid.inspektør.hendelser("saksbehandler_løsning").isNotEmpty())
        assertEquals("AvventerSystem", testRapid.inspektør.hendelser("oppgave_oppdatert").last()["status"].asText())
    }

    @Test
    fun `oppgave_oppdater inneholder påVent-flagg i noen tilfeller`() {
        fun påVentNode() = testRapid.inspektør.hendelser("oppgave_oppdatert").last()["påVent"]

        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!

        mediator.sendMeldingOppgaveOppdatert(oppgavereferanse)
        assertNull(påVentNode())

        mediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = true)
        assertEquals("true", påVentNode().asText())

        mediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = false)
        assertEquals("false", påVentNode().asText())
    }

    @Test
    fun `sjekker at saksbehandler ikke har lov å attestere beslutteroppgaven hvis saksbehandler sendte saken til godkjenning`() {
        val oppgaveId = 1L

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgaveId) } returns SAKSBEHANDLER_OID

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(oppgaveId, SAKSBEHANDLER_OID)

        assertTrue(kanIkkeAttestere)
    }

    @Test
    fun `sjekker at saksbehandler har lov å attestere beslutteroppgaven hvis saksbehandler ikke sendte saken til godkjenning`() {
        val oppgaveId = 1L

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgaveId) } returns UUID.randomUUID()

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(oppgaveId, SAKSBEHANDLER_OID)

        assertFalse(kanIkkeAttestere)
    }

    @Test
    fun `sjekker at saksbehandler har lov å attestere beslutteroppgaven hvis tidligere saksbehandler ikke finnes`() {
        val oppgaveId = 1L

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgaveId) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgaveId) } returns null

        val kanIkkeAttestere =
            mediatorWithMock.erBeslutteroppgave(oppgaveId) && mediatorWithMock.erTidligereSaksbehandler(oppgaveId, SAKSBEHANDLER_OID)

        assertFalse(kanIkkeAttestere)
    }
}
