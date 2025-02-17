package no.nav.helse.spesialist.application

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.Repositories
import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.spesialist.application.kommando.TestMelding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class OppgaveServiceTest {
    private val FNR = lagFødselsnummer()
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
    private val BEHANDLING_ID = UUID.randomUUID()
    private val BEHANDLING_ID_2 = UUID.randomUUID()
    private val UTBETALING_ID = UUID.randomUUID()
    private val UTBETALING_ID_2 = UUID.randomUUID()
    private val HENDELSE_ID = UUID.randomUUID()
    private val TESTHENDELSE = TestMelding(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
    private val OPPGAVE_ID = nextLong()
    private val SAKSBEHANDLERIDENT = lagSaksbehandlerident()
    private val SAKSBEHANDLEROID = UUID.randomUUID()
    private val SAKSBEHANDLERNAVN = lagSaksbehandlernavn()
    private val SAKSBEHANDLEREPOST = lagEpostadresseFraFulltNavn(SAKSBEHANDLERNAVN)
    private val EGENSKAP_SØKNAD = EgenskapForDatabase.SØKNAD

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val repositories = mockk<Repositories>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val saksbehandlerDao = mockk<SaksbehandlerDao>()

    private val meldingPubliserer = object : MeldingPubliserer {
        var antallMeldinger: Int = 0
            private set

        override fun publiser(fødselsnummer: String, hendelse: UtgåendeHendelse, årsak: String) {
            antallMeldinger++
        }

        override fun publiser(fødselsnummer: String, subsumsjonEvent: SubsumsjonEvent, versjonAvKode: String) =
            error("Not implemented for test")

        override fun publiser(
            hendelseId: UUID,
            commandContextId: UUID,
            fødselsnummer: String,
            behov: List<Behov>
        ) = error("Not implemented for test")

        override fun publiser(event: KommandokjedeEndretEvent, hendelseNavn: String) = error("Not implemented for test")
    }

    private val mediator =
        OppgaveService(
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDao,
            reservasjonDao = reservasjonDao,
            opptegnelseDao = opptegnelseDao,
            totrinnsvurderingDao = totrinnsvurderingDao,
            saksbehandlerDao = saksbehandlerDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = { _, _ -> false },
            tilgangsgrupper = tilgangsgrupper,
            repositories = repositories
        )
    private val saksbehandlerFraDatabase =
        SaksbehandlerFraDatabase(SAKSBEHANDLEREPOST, SAKSBEHANDLEROID, SAKSBEHANDLERNAVN, SAKSBEHANDLERIDENT)

    private fun lagSøknadsoppgave(
        fødselsnummer: String,
    ) {
        mediator.nyOppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            behandlingId = BEHANDLING_ID,
            utbetalingId = UTBETALING_ID,
            hendelseId = HENDELSE_ID,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD),
        )
    }

    private fun lagStikkprøveoppgave(
        fødselsnummer: String,
    ) {
        mediator.nyOppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = VEDTAKSPERIODE_ID_2,
            behandlingId = BEHANDLING_ID_2,
            utbetalingId = UTBETALING_ID_2,
            hendelseId = HENDELSE_ID,
            kanAvvises = true,
            egenskaper = setOf(STIKKPRØVE),
        )
    }

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, tildelingDao, opptegnelseDao)
    }

    @Test
    fun `lagrer oppgaver`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns null
        lagSøknadsoppgave(fødselsnummer)
        verify(exactly = 1) {
            oppgaveDao.opprettOppgave(
                id = oppgaveId,
                godkjenningsbehovId = HENDELSE_ID,
                egenskaper = listOf(EGENSKAP_SØKNAD),
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                kanAvvises = true,
            )
        }
        assertEquals(1, meldingPubliserer.antallMeldinger)
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `lagrer oppgave og tildeler til saksbehandler som har reservert personen`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        val fødselsnummer = lagFødselsnummer()
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns Reservasjon(saksbehandlerFraDatabase)
        every { oppgaveDao.finnFødselsnummer(0L) } returns fødselsnummer
        lagSøknadsoppgave(fødselsnummer)
        verify(exactly = 1) { tildelingDao.tildel(0L, SAKSBEHANDLEROID) }
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `tildeler ikke reservert personen når oppgave er stikkprøve`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns Reservasjon(saksbehandlerFraDatabase)
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        lagStikkprøveoppgave(fødselsnummer)
        verify(exactly = 0) { tildelingDao.tildel(any(), any()) }
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `kaller bare hentGrupper når personen er reservert`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns null
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        lagStikkprøveoppgave(fødselsnummer)
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    @Test
    fun `oppdaterer oppgave`() {
        every { totrinnsvurderingDao.hentAktivTotrinnsvurdering(OPPGAVE_ID) } returns (nextLong() to mockk<TotrinnsvurderingFraDatabase>(
            relaxed = true
        ))
        every { oppgaveDao.finnOppgave(OPPGAVE_ID) } returns oppgaveFraDatabase()
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        every { saksbehandlerDao.finnSaksbehandlerFraDatabase(any()) } returns saksbehandlerFraDatabase
        mediator.oppgave(OPPGAVE_ID) {
            avventerSystem(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
            ferdigstill()
        }
        assertEquals(2, meldingPubliserer.antallMeldinger)
        assertOpptegnelseIkkeOpprettet(TESTHENDELSE.fødselsnummer())
    }

    @Test
    fun `lagrer ikke dobbelt`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns null

        lagSøknadsoppgave(fødselsnummer)

        assertEquals(1, meldingPubliserer.antallMeldinger)
        assertAntallOpptegnelser(1, fødselsnummer)
    }

    private fun assertAntallOpptegnelser(
        antallOpptegnelser: Int,
        fødselsnummer: String,
    ) = verify(exactly = antallOpptegnelser) {
        opptegnelseDao.opprettOpptegnelse(
            eq(fødselsnummer),
            any(),
            eq(OpptegnelseDao.Opptegnelse.Type.NY_SAKSBEHANDLEROPPGAVE),
        )
    }

    private fun assertOpptegnelseIkkeOpprettet(fødselsnummer: String) = assertAntallOpptegnelser(0, fødselsnummer)

    private fun oppgaveFraDatabase(
        oppgaveId: Long = OPPGAVE_ID,
        tildelt: Boolean = false,
    ) = OppgaveFraDatabase(
        id = oppgaveId,
        egenskaper = listOf(EgenskapForDatabase.SØKNAD),
        status = "AvventerSaksbehandler",
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        behandlingId = BEHANDLING_ID,
        utbetalingId = UTBETALING_ID,
        godkjenningsbehovId = HENDELSE_ID,
        kanAvvises = true,
        ferdigstiltAvIdent = null,
        ferdigstiltAvOid = null,
        tildelt =
            if (tildelt) {
                SaksbehandlerFraDatabase(
                    SAKSBEHANDLEREPOST,
                    SAKSBEHANDLEROID,
                    SAKSBEHANDLERNAVN,
                    SAKSBEHANDLERIDENT,
                )
            } else {
                null
            },
    )
}
