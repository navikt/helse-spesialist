package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HendelseMediatorTest : AbstractE2ETest() {

    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val reserverpersonDaoMock = mockk<ReservasjonDao>(relaxed = true)
    private val totrinnsvurderingDaoMock = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val periodehistorikkDaoMock = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatMediatorMock = mockk<NotatMediator>(relaxed = true)
    private val mediatorWithMock = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = mockk(relaxed = true),
        opptegnelseDao = mockk(),
        oppgaveMediator = oppgaveMediatorMock,
        hendelsefabrikk = mockk(),
        reservasjonDao = reserverpersonDaoMock,
        periodehistorikkDao = periodehistorikkDaoMock,
        totrinnsvurderingMediator = TotrinnsvurderingMediator(totrinnsvurderingDaoMock, oppgaveMediatorMock, notatMediatorMock)
    )

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        hendelseMediator.håndter(GodkjenningDTO(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid)
        assertTrue(testRapid.inspektør.hendelser("saksbehandler_løsning").isNotEmpty())
        assertEquals("AvventerSystem", testRapid.inspektør.hendelser("oppgave_oppdatert").last()["status"].asText())
    }

    @Test
    fun `oppgave_oppdater inneholder påVent-flagg i noen tilfeller`() {
        fun påVentNode() = testRapid.inspektør.hendelser("oppgave_oppdatert").last()["påVent"]

        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        hendelseMediator.sendMeldingOppgaveOppdatert(oppgavereferanse)
        assertNull(påVentNode())

        hendelseMediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = true)
        assertEquals("true", påVentNode().asText())

        hendelseMediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = false)
        assertEquals("false", påVentNode().asText())
    }

    @Test
    fun `For beslutteroppgave settes oppgavens tidligere_saksbehandler som reserverPersonOid`() {
        val oid = UUID.randomUUID()
        val tidligereSaksbehandlerOid = UUID.randomUUID()
        settOppBruker()

        every { oppgaveMediatorMock.erBeslutteroppgave(1L) } returns true
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(1L) } returns tidligereSaksbehandlerOid

        mediatorWithMock.håndter(GodkjenningDTO(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid)

        verify (exactly = 1) { reserverpersonDaoMock.reserverPerson(tidligereSaksbehandlerOid, any(), any()) }
    }

    @Test
    fun `For beslutteroppgave setter totrinnsvurderingens saksbehandlerOid som reserverPersonOid`() {
        val oid = UUID.randomUUID()
        val totrinnsvurderingSaksbehandlerOid = UUID.randomUUID()
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!

        every { oppgaveMediatorMock.erBeslutteroppgave(oppgavereferanse) } returns false
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(oppgavereferanse) } returns null
        every { totrinnsvurderingDaoMock.hentAktiv(VEDTAKSPERIODE_ID) } returns Totrinnsvurdering(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = totrinnsvurderingSaksbehandlerOid,
            beslutter = UUID.randomUUID(),
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        mediatorWithMock.håndter(GodkjenningDTO(oppgavereferanse, true, "saksbehandler", null, null, null), "epost@nav.no", oid)

        verify (exactly = 1) { reserverpersonDaoMock.reserverPerson(totrinnsvurderingSaksbehandlerOid, any(), any()) }
    }

    @Test
    fun `Dersom ikke beslutteroppgave settes saksbehandlerOid som reserverPersonOid`() {
        val oid = UUID.randomUUID()
        settOppBruker()

        every { oppgaveMediatorMock.erBeslutteroppgave(1L) } returns false
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(1L) } returns null
        every { totrinnsvurderingDaoMock.hentAktiv(VEDTAKSPERIODE_ID) } returns null

        mediatorWithMock.håndter(GodkjenningDTO(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid)

        verify (exactly = 1) { reserverpersonDaoMock.reserverPerson(oid, any(), any()) }
    }

    @Test
    fun `Ferdigstiller totrinnsvurdering`() {
        val oid = UUID.randomUUID()
        settOppBruker()

        every { oppgaveMediatorMock.erBeslutteroppgave(1L) } returns false
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(1L) } returns null
        every { totrinnsvurderingDaoMock.hentAktiv(VEDTAKSPERIODE_ID) } returns Totrinnsvurdering(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = SAKSBEHANDLER_OID,
            beslutter = UUID.randomUUID(),
            utbetalingIdRef = 1,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        mediatorWithMock.håndter(GodkjenningDTO(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid)

        verify (exactly = 1) { totrinnsvurderingDaoMock.ferdigstill(VEDTAKSPERIODE_ID) }
    }

    @Test
    fun `Lagrer periodehistorikk for beslutteroppgave ved utbetaling`() {
        val oid = UUID.randomUUID()
        settOppBruker()

        every { oppgaveMediatorMock.erBeslutteroppgave(1L) } returns false
        every { oppgaveMediatorMock.finnTidligereSaksbehandler(1L) } returns null
        every { totrinnsvurderingDaoMock.hentAktiv(VEDTAKSPERIODE_ID) } returns Totrinnsvurdering(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = SAKSBEHANDLER_OID,
            beslutter = oid,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        mediatorWithMock.håndter(GodkjenningDTO(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid)

        verify (exactly = 1) { periodehistorikkDaoMock.lagre(PeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT, oid, any(), null) }
    }

}
