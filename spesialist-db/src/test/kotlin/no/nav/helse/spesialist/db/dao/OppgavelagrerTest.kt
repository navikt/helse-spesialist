package no.nav.helse.spesialist.db.dao

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.mediator.oppgave.Oppgavelagrer
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.db.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.db.lagSaksbehandlerident
import no.nav.helse.spesialist.db.lagSaksbehandlernavn
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random.Default.nextLong

class OppgavelagrerTest {
    private val oppgavetype = SØKNAD
    private val vedtaksperiodeId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val utbetalingId: UUID = UUID.randomUUID()
    private val oppgaveId = nextLong()
    private val saksbehandlernavn = lagSaksbehandlernavn()
    private val saksbehandler =
        Saksbehandler(
            oid = UUID.randomUUID(),
            epostadresse = lagEpostadresseFraFulltNavn(saksbehandlernavn),
            navn = saksbehandlernavn,
            ident = lagSaksbehandlerident(),
            tilgangskontroll = { _, _ -> false },
        )

    private val godkjenningsbehovId: UUID = UUID.randomUUID()

    private val oppgaveDaoMock = mockk<OppgaveDao>(relaxed = true)
    private val tildelingDaoMock = mockk<TildelingDao>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        clearMocks(this.tildelingDaoMock, oppgaveService)
    }

    @Test
    fun `lagre oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.lagre(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.opprettOppgave(
                id = oppgaveId,
                godkjenningsbehovId = godkjenningsbehovId,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                kanAvvises = true,
            )
        }
        verify(exactly = 0) { tildelingDaoMock.tildel(any(), any()) }
        verify(exactly = 1) { tildelingDaoMock.avmeld(oppgaveId) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.oppdater(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.updateOppgave(
                oppgaveId, "AvventerSaksbehandler", null, null, listOf(
                    EgenskapForDatabase.SØKNAD
                )
            )
        }
        verify(exactly = 0) { tildelingDaoMock.tildel(any(), any()) }
        verify(exactly = 1) { tildelingDaoMock.avmeld(oppgaveId) }
    }

    @Test
    fun `lagre oppgave uten tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.lagre(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.opprettOppgave(
                id = oppgaveId,
                godkjenningsbehovId = godkjenningsbehovId,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                kanAvvises = true,
            )
        }
        verify(exactly = 0) { tildelingDaoMock.tildel(any(), any()) }
    }

    @Test
    fun `lagre oppgave`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        val oppgavelagrer = Oppgavelagrer(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.lagre(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.opprettOppgave(
                id = oppgaveId,
                godkjenningsbehovId = godkjenningsbehovId,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                utbetalingId = utbetalingId,
                kanAvvises = true,
            )
        }
        verify(exactly = 1) { tildelingDaoMock.tildel(oppgaveId, saksbehandler.oid) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling`() {
        val oppgave = nyOppgave()
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.oppdater(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.updateOppgave(
                oppgaveId, "Ferdigstilt", saksbehandler.ident(), saksbehandler.oid, listOf(
                    EgenskapForDatabase.SØKNAD
                )
            )
        }
        verify(exactly = 0) { tildelingDaoMock.tildel(any(), any()) }
    }

    @Test
    fun `oppdatere oppgave`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.oppdater(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.updateOppgave(
                oppgaveId, "Ferdigstilt", saksbehandler.ident(), saksbehandler.oid, listOf(
                    EgenskapForDatabase.SØKNAD
                )
            )
        }
    }

    private fun nyOppgave() =
        Oppgave.ny(
            id = oppgaveId,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(oppgavetype),
        )
}
