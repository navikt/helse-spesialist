package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.api.GodkjenningService
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningServiceTest : AbstractE2ETest() {

    private val reserverpersonDaoMock = mockk<ReservasjonDao>(relaxed = true)
    private val totrinnsvurderingDaoMock = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val periodehistorikkDaoMock = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatMediatorMock = mockk<NotatMediator>(relaxed = true)
    private val godkjenningServiceWithMocks = GodkjenningService(
        dataSource = dataSource,
        oppgaveDao = oppgaveDao,
        hendelseDao = HendelseDao(dataSource),
        overstyringDao = mockk(relaxed = true),
        rapidsConnection = testRapid,
        oppgaveMediator = oppgaveMediator,
        reservasjonDao = reserverpersonDaoMock,
        periodehistorikkDao = periodehistorikkDaoMock,
        totrinnsvurderingMediator = TotrinnsvurderingMediator(
            totrinnsvurderingDaoMock,
            mockk<OppgaveDao>(),
            periodehistorikkDaoMock,
            notatMediatorMock,
        ),
    )

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        godkjenningServiceWithMocks.håndter(GodkjenningDto(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid, UUID.randomUUID())
        assertTrue(testRapid.inspektør.hendelser("saksbehandler_løsning").isNotEmpty())
        assertEquals("AvventerSystem", testRapid.inspektør.hendelser("oppgave_oppdatert").last()["status"].asText())
    }

    @Test
    fun `håndter godkjenning`() {
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        val behandlingId = UUID.randomUUID()
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        val hendelseId = oppgaveDao.finnHendelseId(oppgavereferanse)
        godkjenningServiceWithMocks.håndter(GodkjenningDto(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid, behandlingId)

        val melding = testRapid.inspektør.hendelser("saksbehandler_løsning").last()

        assertEquals(FØDSELSNUMMER, melding["fødselsnummer"].asText())
        assertEquals(oppgavereferanse, melding["oppgaveId"].asLong())
        assertEquals(hendelseId.toString(), melding["hendelseId"].asText())
        assertEquals(behandlingId.toString(), melding["behandlingId"].asText())
        assertEquals(true, melding["godkjent"].asBoolean())
        assertEquals(saksbehandlerIdent, melding["saksbehandlerident"].asText())
        assertEquals(oid.toString(), melding["saksbehandleroid"].asText())
        assertEquals(epost, melding["saksbehandlerepost"].asText())
        assertNotNull(melding["godkjenttidspunkt"]?.asText())
    }

    @Test
    fun `Ved godkjenning av beslutteroppgave reserveres personen til tidligereSaksbehandler`() {
        val oid = UUID.randomUUID()
        val tidligereSaksbehandlerOid = UUID.randomUUID()
        settOppBruker()

        every { totrinnsvurderingDaoMock.hentAktiv(any<UUID>()) } returns mockk<TotrinnsvurderingOld>().also { totrinnsvurdering ->
            every { totrinnsvurdering.erBeslutteroppgave() } returns true
            every { totrinnsvurdering.saksbehandler } returns tidligereSaksbehandlerOid
        }

        godkjenningServiceWithMocks.håndter(GodkjenningDto(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid, UUID.randomUUID())

        verify (exactly = 1) { reserverpersonDaoMock.reserverPerson(tidligereSaksbehandlerOid, any(), any()) }
    }

    @Test
    fun `For beslutteroppgave setter totrinnsvurderingens saksbehandlerOid som reserverPersonOid`() {
        val oid = UUID.randomUUID()
        val totrinnsvurderingSaksbehandlerOid = UUID.randomUUID()
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!

        every { totrinnsvurderingDaoMock.hentAktiv(Testdata.VEDTAKSPERIODE_ID) } returns TotrinnsvurderingOld(
            vedtaksperiodeId = Testdata.VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = totrinnsvurderingSaksbehandlerOid,
            beslutter = UUID.randomUUID(),
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        godkjenningServiceWithMocks.håndter(GodkjenningDto(oppgavereferanse, true, "saksbehandler", null, null, null), "epost@nav.no", oid, UUID.randomUUID())

        verify (exactly = 1) { reserverpersonDaoMock.reserverPerson(totrinnsvurderingSaksbehandlerOid, any(), any()) }
    }

    @Test
    fun `Dersom ikke beslutteroppgave settes saksbehandlerOid som reserverPersonOid`() {
        val oid = UUID.randomUUID()
        settOppBruker()

        every { totrinnsvurderingDaoMock.hentAktiv(Testdata.VEDTAKSPERIODE_ID) } returns null

        godkjenningServiceWithMocks.håndter(GodkjenningDto(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid, UUID.randomUUID())

        verify (exactly = 1) { reserverpersonDaoMock.reserverPerson(oid, any(), any()) }
    }

    @Test
    fun `Lagrer at beslutter har attestert i periodehistorikk`() {
        val oid = UUID.randomUUID()
        settOppBruker()

        every { totrinnsvurderingDaoMock.hentAktiv(Testdata.VEDTAKSPERIODE_ID) } returns TotrinnsvurderingOld(
            vedtaksperiodeId = Testdata.VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = Testdata.SAKSBEHANDLER_OID,
            beslutter = oid,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        godkjenningServiceWithMocks.håndter(GodkjenningDto(1L, true, "saksbehandler", null, null, null), "epost@nav.no", oid, UUID.randomUUID())

        verify (exactly = 1) { periodehistorikkDaoMock.lagre(PeriodehistorikkType.TOTRINNSVURDERING_ATTESTERT, oid, any(), null) }
    }

}
