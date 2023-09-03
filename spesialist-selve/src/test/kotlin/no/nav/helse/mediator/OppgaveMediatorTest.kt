package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangskontroll
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.RISK_QA
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.STIKKPRØVE
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.SØKNAD
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
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLERNAVN = "Hen Saksbehandler"
        private val OPPGAVETYPE_SØKNAD = SØKNAD
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
        harTilgangTil = gruppehenterTestoppsett.hentGrupper,
    )

    private val saksbehandler = Saksbehandler(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT)
    private fun søknadsoppgave(id: Long): Oppgave = Oppgave.oppgaveMedEgenskaper(id, VEDTAKSPERIODE_ID, UTBETALING_ID, listOf(SØKNAD))
    private fun stikkprøveoppgave(id: Long): Oppgave = Oppgave.oppgaveMedEgenskaper(id, VEDTAKSPERIODE_ID_2, UTBETALING_ID_2, listOf(STIKKPRØVE))
    private fun riskoppgave(id: Long): Oppgave = Oppgave.oppgaveMedEgenskaper(id, VEDTAKSPERIODE_ID, UTBETALING_ID, listOf(RISK_QA))

    private val testRapid = TestRapid()

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao, tildelingDao, opptegnelseDao)
        testRapid.reset()
    }

    @Test
    fun `lagrer oppgaver`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { oppgaveDao.finn(0L) } returns søknadsoppgave(0L)
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.nyOppgave {
            søknadsoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 1) {
            oppgaveDao.opprettOppgave(
                0L,
                COMMAND_CONTEXT_ID,
                OPPGAVETYPE_SØKNAD,
                VEDTAKSPERIODE_ID,
                UTBETALING_ID
            )
        }
        assertEquals(1, testRapid.inspektør.size)
        assertOppgaveevent(0, "oppgave_opprettet")
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `lagrer oppgave og tildeler til saksbehandler som har reservert personen`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjonsinfo(saksbehandler, false)
        every { oppgaveDao.finn(0L) } returns søknadsoppgave(0L)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.nyOppgave {
            søknadsoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertFalse(gruppehenterTestoppsett.erKalt)
        verify(exactly = 1) { tildelingDao.opprettTildeling(any(), SAKSBEHANDLEROID, any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `tildeler ikke risk-oppgave til saksbehandler som har reservert personen hvis hen ikke har risk-tilgang`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjonsinfo(saksbehandler, false)
        every { oppgaveDao.finn(0L) } returns riskoppgave(0L)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.nyOppgave {
            riskoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertTrue(gruppehenterTestoppsett.erKalt)
        verify(exactly = 0) { tildelingDao.opprettTildeling(any(), SAKSBEHANDLEROID, any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `tildeler ikke reservert personen når oppgave er stikkprøve`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns Reservasjonsinfo(saksbehandler, false)
        every { oppgaveDao.finn(0L) } returns stikkprøveoppgave(0L)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.nyOppgave {
            stikkprøveoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 0) { tildelingDao.opprettTildeling(any(), any(), any()) }
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `kaller bare hentGrupper når personen er reservert`() {
        every { reservasjonDao.hentReservasjonFor(TESTHENDELSE.fødselsnummer()) } returns null
        every { oppgaveDao.reserverNesteId() } returns 0L

        every { oppgaveDao.finn(0L) } returns søknadsoppgave(0L)
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()
        mediator.nyOppgave {
            stikkprøveoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertFalse(gruppehenterTestoppsett.erKalt)
        assertAntallOpptegnelser(1)
    }

    @Test
    fun `oppdaterer oppgave`() {
        val oppgave = Oppgave(
            OPPGAVE_ID,
            OPPGAVETYPE_SØKNAD,
            Oppgavestatus.AvventerSaksbehandler,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        every { oppgaveDao.finn(any<Long>()) } returns oppgave
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
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
        every { oppgaveDao.harGyldigOppgave(UTBETALING_ID) } returnsMany listOf(false, true)
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { oppgaveDao.finn(0L) } returns søknadsoppgave(0L)
        mediator.nyOppgave {
            søknadsoppgave(it)
        }
        mediator.nyOppgave {
            søknadsoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 1) { oppgaveDao.opprettOppgave(any(), COMMAND_CONTEXT_ID, OPPGAVETYPE_SØKNAD, any(), UTBETALING_ID) }
        assertOpptegnelseIkkeOpprettet()

    }

    @Test
    fun `lagrer ikke dobbelt`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        every { oppgaveDao.finn(0L) } returns søknadsoppgave(0L)
        every { oppgaveDao.opprettOppgave(any(), any(), OPPGAVETYPE_SØKNAD, any(), any()) } returns 0L
        every { oppgaveDao.finnFødselsnummer(any()) } returns TESTHENDELSE.fødselsnummer()

        mediator.nyOppgave {
            søknadsoppgave(it)
        }
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertEquals(1, testRapid.inspektør.size)
        assertAntallOpptegnelser(1)
        testRapid.reset()
        clearMocks(opptegnelseDao)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        assertEquals(0, testRapid.inspektør.size)
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `avbryter oppgaver`() {
        val oppgave1 = Oppgave(
            1L,
            OPPGAVETYPE_SØKNAD,
            Oppgavestatus.AvventerSaksbehandler,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        every { oppgaveDao.finnAktiv(VEDTAKSPERIODE_ID) } returns oppgave1
        every { oppgaveDao.finn(1L) } returns oppgave1
        mediator.avbrytOppgaver(VEDTAKSPERIODE_ID)
        mediator.lagreOgTildelOppgaver(TESTHENDELSE.id, TESTHENDELSE.fødselsnummer(), COMMAND_CONTEXT_ID, testRapid)
        verify(exactly = 1) { oppgaveDao.finnAktiv(VEDTAKSPERIODE_ID) }
        verify(exactly = 1) { oppgaveDao.updateOppgave(any(), Oppgavestatus.Invalidert, null, null) }
        assertOpptegnelseIkkeOpprettet()
    }

    private fun assertAntallOpptegnelser(antallOpptegnelser: Int) = verify(exactly = antallOpptegnelser) {
        opptegnelseDao.opprettOpptegnelse(
            eq(TESTHENDELSE.fødselsnummer()),
            any(),
            eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)
        )
    }

    private fun assertOpptegnelseIkkeOpprettet() = assertAntallOpptegnelser(0)

    private fun assertOppgaveevent(
        indeks: Int,
        navn: String,
        status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
        assertBlock: (JsonNode) -> Unit = {},
    ) {
        testRapid.inspektør.message(indeks).also {
            assertEquals(navn, it.path("@event_name").asText())
            assertEquals(HENDELSE_ID, UUID.fromString(it.path("hendelseId").asText()))
            assertEquals(status, enumValueOf<Oppgavestatus>(it.path("status").asText()))
            assertTrue(it.hasNonNull("oppgaveId"))
            assertBlock(it)
        }
    }

    class GruppehenterTestoppsett {
        var erKalt = false

        val hentGrupper: Tilgangskontroll = { _: UUID, _: Gruppe ->
            erKalt = true
            true
        }
    }
}
