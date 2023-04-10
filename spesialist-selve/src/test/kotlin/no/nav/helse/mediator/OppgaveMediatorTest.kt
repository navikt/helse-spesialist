package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.oppgave.Saksbehandlergrupper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.reservasjon.Reservasjonsinfo
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextLong

internal class OppgaveMediatorTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private val UTBETALING_ID_2 = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private val COMMAND_CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestHendelse(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
        private val OPPGAVE_ID = nextLong()
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val OPPGAVETYPE_SØKNAD = Oppgavetype.SØKNAD
        private val OPPGAVETYPE_STIKKPRØVE = Oppgavetype.STIKKPRØVE
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val gruppehenterTestoppsett = GruppehenterTestoppsett()
    private val mediator = OppgaveMediator(
        oppgaveDao,
        tildelingDao,
        reservasjonDao,
        opptegnelseDao,
        gruppehenter = gruppehenterTestoppsett.hentGrupper,
    )
    private val søknadsoppgave: Oppgave = Oppgave.søknad(VEDTAKSPERIODE_ID, UTBETALING_ID)
    private val stikkprøveoppgave: Oppgave = Oppgave.stikkprøve(VEDTAKSPERIODE_ID_2, UTBETALING_ID_2)

    private val testRapid = TestRapid()

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao, tildelingDao, opptegnelseDao)
        testRapid.reset()
    }

    @Test
    fun `lagrer oppgaver`() {
        every { reservasjonDao.hentReservertTil(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.finn(0L) } returns søknadsoppgave
        every { oppgaveDao.finn(1L) } returns stikkprøveoppgave
        every { oppgaveDao.opprettOppgave(any(), OPPGAVETYPE_SØKNAD, any(), any()) } returns 0L
        every { oppgaveDao.opprettOppgave(any(), OPPGAVETYPE_STIKKPRØVE, any(), any()) } returns 1L
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { oppgaveDao.finnContextId(any()) } returns COMMAND_CONTEXT_ID
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.opprett(søknadsoppgave)
        mediator.opprett(stikkprøveoppgave)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 1) { oppgaveDao.opprettOppgave(COMMAND_CONTEXT_ID, OPPGAVETYPE_SØKNAD, VEDTAKSPERIODE_ID, UTBETALING_ID) }
        verify(exactly = 1) { oppgaveDao.opprettOppgave(COMMAND_CONTEXT_ID, OPPGAVETYPE_STIKKPRØVE, VEDTAKSPERIODE_ID_2, UTBETALING_ID_2) }
        assertEquals(2, testRapid.inspektør.size)
        assertOppgaveevent(0, "oppgave_opprettet")
        assertOppgaveevent(1, "oppgave_opprettet")
        assertAntallOpptegnelser(2)
    }

    @Test
    fun `lagrer oppgave og tildeler til saksbehandler som har reservert personen`() {
        val oid = UUID.randomUUID()
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjonsinfo(oid, false)
        every { oppgaveDao.finn(0L) } returns søknadsoppgave
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.opprett(søknadsoppgave)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertTrue(gruppehenterTestoppsett.erKalt)
        verify(exactly = 1) { tildelingDao.opprettTildeling(any(), oid, any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `tildeler ikke reservert personen når oppgave er stikkprøve`() {
        val oid = UUID.randomUUID()
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjonsinfo(oid, false)
        every { oppgaveDao.finn(0L) } returns stikkprøveoppgave
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.opprett(stikkprøveoppgave)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 0) { tildelingDao.opprettTildeling(any(), any(), any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `kaller bare hentGrupper når personen er reservert`() {
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.finn(0L) } returns søknadsoppgave
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.opprett(stikkprøveoppgave)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertFalse(gruppehenterTestoppsett.erKalt)
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `oppdaterer oppgave`() {
        every { reservasjonDao.hentReservertTil(TESTHENDELSE.fødselsnummer()) } returns null
        val oppgave = Oppgave(OPPGAVE_ID, OPPGAVETYPE_SØKNAD, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID)
        every { oppgaveDao.finn(any<Long>()) } returns oppgave
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { oppgaveDao.finnContextId(any()) } returns COMMAND_CONTEXT_ID
        mediator.ferdigstill(oppgave, SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertEquals(1, testRapid.inspektør.size)
        assertOppgaveevent(0, "oppgave_oppdatert", Oppgavestatus.Ferdigstilt) {
            assertEquals(OPPGAVE_ID, it.path("oppgaveId").longValue())
            assertEquals(SAKSBEHANDLERIDENT, it.path("ferdigstiltAvIdent").asText())
            assertEquals(SAKSBEHANDLEROID, UUID.fromString(it.path("ferdigstiltAvOid").asText()))
        }
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `oppretter ikke flere oppgaver på samme vedtaksperiodeId`() {
        every { oppgaveDao.harGyldigOppgave( UTBETALING_ID) } returnsMany listOf(false, true)
        every { reservasjonDao.hentReservertTil(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.finn(0L) } returns søknadsoppgave
        mediator.opprett(søknadsoppgave)
        mediator.opprett(søknadsoppgave)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 1) { oppgaveDao.opprettOppgave(COMMAND_CONTEXT_ID, OPPGAVETYPE_SØKNAD, any(), UTBETALING_ID) }
        assertOpptegnelseIkkeOpprettet()

    }
    @Test
    fun `lagrer ikke dobbelt`() {
        every { reservasjonDao.hentReservertTil(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.finn(0L) } returns søknadsoppgave
        every { oppgaveDao.finn(1L) } returns stikkprøveoppgave
        every { oppgaveDao.opprettOppgave(any(), OPPGAVETYPE_SØKNAD, any(), any()) } returns 0L
        every { oppgaveDao.opprettOppgave(any(), OPPGAVETYPE_STIKKPRØVE, any(), any()) } returns 1L
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()

        mediator.opprett(søknadsoppgave)
        mediator.opprett(stikkprøveoppgave)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertEquals(2, testRapid.inspektør.size)
        assertAntallOpptegnelser(2)
        testRapid.reset()
        clearMocks(opptegnelseDao)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertEquals(0, testRapid.inspektør.size)
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `avbryter oppgaver`() {
        val oppgave1 = Oppgave(1L, OPPGAVETYPE_SØKNAD, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID)
        val oppgave2 = Oppgave(2L, OPPGAVETYPE_STIKKPRØVE, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID, utbetalingId = UTBETALING_ID)
        every { oppgaveDao.finnAktive(VEDTAKSPERIODE_ID) } returns listOf(oppgave1, oppgave2)
        every { reservasjonDao.hentReservertTil(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.finn(1L) } returns oppgave1
        every { oppgaveDao.finn(2L) } returns oppgave2
        mediator.avbrytOppgaver(VEDTAKSPERIODE_ID)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 1) { oppgaveDao.finnAktive(VEDTAKSPERIODE_ID) }
        verify(exactly = 2) { oppgaveDao.updateOppgave(any(), Oppgavestatus.Invalidert, null, null) }
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `setter beslutter på totrinnsvurdering`() {

        mediator.setBeslutteroppgave(
            oppgaveId = 1L,
            tidligereSaksbehandlerOid = UUID.randomUUID()
        )

        verify(exactly = 1) { oppgaveDao.setBeslutteroppgave(any(), any()) }
    }

    private fun assertAntallOpptegnelser(antallOpptegnelser: Int) = verify(exactly = antallOpptegnelser) { opptegnelseDao.opprettOpptegnelse(eq(TESTHENDELSE.fødselsnummer()), any(), eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)) }

    private fun assertOpptegnelseIkkeOpprettet()= assertAntallOpptegnelser(0)

    private fun assertOppgaveevent(indeks: Int, navn: String, status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler, assertBlock: (JsonNode) -> Unit = {}) {
        testRapid.inspektør.message(indeks).also {
            assertEquals(navn, it.path("@event_name").asText())
            assertEquals(HENDELSE_ID, UUID.fromString(it.path("hendelseId").asText()))
            assertEquals(COMMAND_CONTEXT_ID, UUID.fromString(it.path("contextId").asText()))
            assertEquals(status, enumValueOf<Oppgavestatus>(it.path("status").asText()))
            assertTrue(it.hasNonNull("oppgaveId"))
            assertBlock(it)
        }
    }

    class GruppehenterTestoppsett {
        var erKalt = false

        val hentGrupper: Saksbehandlergrupper = { _: UUID ->
            erKalt = true
        }
    }
}
