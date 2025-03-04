package no.nav.helse.spesialist.db.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.db.dao.PgOppgaveDao
import no.nav.helse.spesialist.db.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.db.lagSaksbehandlerident
import no.nav.helse.spesialist.db.lagSaksbehandlernavn
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random.Default.nextLong

class PgOppgaveRepositoryTest {
    private val oppgavetype = SØKNAD
    private val vedtaksperiodeId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val utbetalingId: UUID = UUID.randomUUID()
    private val oppgaveId = nextLong()
    private val saksbehandlernavn = lagSaksbehandlernavn()
    private val legacySaksbehandler =
        LegacySaksbehandler(
            oid = UUID.randomUUID(),
            epostadresse = lagEpostadresseFraFulltNavn(saksbehandlernavn),
            navn = saksbehandlernavn,
            ident = lagSaksbehandlerident(),
            tilgangskontroll = { _, _ -> false },
        )

    private val godkjenningsbehovId: UUID = UUID.randomUUID()

    private val oppgaveDaoMock = mockk<PgOppgaveDao>(relaxed = true)
    private val tildelingDaoMock = mockk<TildelingDao>(relaxed = true)

    @Test
    fun `finn oppgave`() {
        every { oppgaveDaoMock.finnOppgave(any()) } returns OppgaveFraDatabase(
            id = oppgaveId,
            egenskaper = listOf(EgenskapForDatabase.SØKNAD),
            status = "AvventerSaksbehandler",
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            godkjenningsbehovId = godkjenningsbehovId,
            kanAvvises = false,
            ferdigstiltAvIdent = legacySaksbehandler.ident(),
            ferdigstiltAvOid = legacySaksbehandler.oid,
            tildelt = null,
        )
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.oppgave(oppgaveId) {_, _ -> false }
        verify(exactly = 1) { oppgaveDaoMock.finnOppgave(oppgaveId) }
    }

    @Test
    fun `lagre oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

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
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

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
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

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
        oppgave.forsøkTildelingVedReservasjon(legacySaksbehandler)
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

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
        verify(exactly = 1) { tildelingDaoMock.tildel(oppgaveId, legacySaksbehandler.oid) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling`() {
        val oppgave = nyOppgave()
        oppgave.avventerSystem(legacySaksbehandler.ident(), legacySaksbehandler.oid)
        oppgave.ferdigstill()
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.oppdater(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.updateOppgave(
                oppgaveId, "Ferdigstilt", legacySaksbehandler.ident(), legacySaksbehandler.oid, listOf(
                    EgenskapForDatabase.SØKNAD
                )
            )
        }
        verify(exactly = 0) { tildelingDaoMock.tildel(any(), any()) }
    }

    @Test
    fun `oppdatere oppgave`() {
        val oppgave = nyOppgave()
        oppgave.forsøkTildelingVedReservasjon(legacySaksbehandler)
        oppgave.avventerSystem(legacySaksbehandler.ident(), legacySaksbehandler.oid)
        oppgave.ferdigstill()
        val oppgavelagrer = PgOppgaveRepository(oppgaveDaoMock, tildelingDaoMock)

        oppgavelagrer.oppdater(oppgave)
        verify(exactly = 1) {
            oppgaveDaoMock.updateOppgave(
                oppgaveId, "Ferdigstilt", legacySaksbehandler.ident(), legacySaksbehandler.oid, listOf(
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
