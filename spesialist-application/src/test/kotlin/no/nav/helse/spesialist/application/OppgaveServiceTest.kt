package no.nav.helse.spesialist.application

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.Daos
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class OppgaveServiceTest {
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    private val VEDTAKSPERIODE_ID_2 = UUID.randomUUID()
    private val BEHANDLING_ID = UUID.randomUUID()
    private val BEHANDLING_ID_2 = UUID.randomUUID()
    private val UTBETALING_ID = UUID.randomUUID()
    private val UTBETALING_ID_2 = UUID.randomUUID()
    private val HENDELSE_ID = UUID.randomUUID()
    private val OPPGAVE_ID = nextLong()
    private val SAKSBEHANDLERIDENT = lagSaksbehandlerident()
    private val SAKSBEHANDLEROID = UUID.randomUUID()
    private val SAKSBEHANDLERNAVN = lagSaksbehandlernavn()
    private val SAKSBEHANDLEREPOST = lagEpostadresseFraFulltNavn(SAKSBEHANDLERNAVN)
    private val EGENSKAP_SØKNAD = EgenskapForDatabase.SØKNAD

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val daos = mockk<Daos>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)

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
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = { _, _ -> false },
            tilgangsgrupper = tilgangsgrupper,
            daos = daos
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
    }

    @Test
    fun `lagrer oppgave og tildeler til saksbehandler som har reservert personen`() {
        every { oppgaveDao.reserverNesteId() } returns 0L
        val fødselsnummer = lagFødselsnummer()
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns Reservasjon(saksbehandlerFraDatabase)
        every { oppgaveDao.finnFødselsnummer(0L) } returns fødselsnummer
        lagSøknadsoppgave(fødselsnummer)
        verify(exactly = 1) { tildelingDao.tildel(0L, SAKSBEHANDLEROID) }
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
    }

    @Test
    fun `kaller bare hentGrupper når personen er reservert`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgaveId = 0L
        every { reservasjonDao.hentReservasjonFor(fødselsnummer) } returns null
        every { oppgaveDao.reserverNesteId() } returns oppgaveId
        every { oppgaveDao.finnFødselsnummer(oppgaveId) } returns fødselsnummer
        lagStikkprøveoppgave(fødselsnummer)
    }

    @Test
    fun `oppdaterer oppgave`() {
        every { oppgaveDao.finnOppgave(OPPGAVE_ID) } returns oppgaveFraDatabase()
        every { oppgaveDao.finnHendelseId(any()) } returns HENDELSE_ID
        mediator.oppgave(OPPGAVE_ID) {
            avventerSystem(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
            ferdigstill()
        }
        assertEquals(2, meldingPubliserer.antallMeldinger)
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
    }

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
