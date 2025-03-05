package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.db.lagSaksbehandlerident
import no.nav.helse.spesialist.db.lagSaksbehandlernavn
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.random.Random.Default.nextLong
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgOppgaveRepositoryTest: AbstractDBIntegrationTest() {
    private val repository = PgOppgaveRepository(session)
    private val vedtaksperiodeId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val utbetalingId: UUID = UUID.randomUUID()
    private val saksbehandlernavn = lagSaksbehandlernavn()
    private val godkjenningsbehovId: UUID = UUID.randomUUID()

    private val legacySaksbehandler =
        LegacySaksbehandler(
            oid = UUID.randomUUID(),
            epostadresse = lagEpostadresseFraFulltNavn(saksbehandlernavn),
            navn = saksbehandlernavn,
            ident = lagSaksbehandlerident(),
            tilgangskontroll = { _, _ -> false },
        )

    @BeforeEach
    fun beforeEach() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId, spleisBehandlingId = behandlingId)
    }

    @Test
    fun `lagre og finn oppgave`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )

        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id) { _, _ -> false }
        assertNotNull(funnetOppgave)
        assertEquals(oppgave, funnetOppgave)
    }

    @Test
    fun `tildel oppgave`() {
        opprettSaksbehandler(legacySaksbehandler.oid, legacySaksbehandler.navn, legacySaksbehandler.epostadresse, legacySaksbehandler.ident())
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.forsøkTildeling(legacySaksbehandler)

        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id) { _, _ -> false }
        assertNotNull(funnetOppgave)
        assertEquals(legacySaksbehandler, funnetOppgave.tildeltTil)
    }

    @Test
    fun `endre egenskaper`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD, PÅ_VENT)
        )
        repository.lagre(oppgave)
        oppgave.leggTilEgenAnsatt()
        oppgave.fjernFraPåVent()
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id) { _, _ -> false }
        assertNotNull(funnetOppgave)
        assertEquals(setOf(Egenskap.EGEN_ANSATT, SØKNAD), funnetOppgave.egenskaper)
    }

    @Test
    fun `endre tilstand`() {
        opprettSaksbehandler(legacySaksbehandler.oid, legacySaksbehandler.navn, legacySaksbehandler.epostadresse, legacySaksbehandler.ident())
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        repository.lagre(oppgave)
        oppgave.avventerSystem(legacySaksbehandler.ident(), legacySaksbehandler.oid)
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id) { _, _ -> false }
        assertNotNull(funnetOppgave)
        assertEquals(Oppgave.AvventerSystem, funnetOppgave.tilstand)
    }

    @Test
    fun ferdigstill() {
        opprettSaksbehandler(legacySaksbehandler.oid, legacySaksbehandler.navn, legacySaksbehandler.epostadresse, legacySaksbehandler.ident())
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        repository.lagre(oppgave)
        oppgave.avventerSystem(legacySaksbehandler.ident(), legacySaksbehandler.oid)
        oppgave.ferdigstill()
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id) { _, _ -> false }
        assertNotNull(funnetOppgave)
        assertEquals(Oppgave.Ferdigstilt, funnetOppgave.tilstand)
        assertEquals(legacySaksbehandler.ident(), funnetOppgave.ferdigstiltAvIdent)
        assertEquals(legacySaksbehandler.oid, funnetOppgave.ferdigstiltAvOid)
    }

    @Test
    fun `exception om det finnes en annen aktiv oppgave for personen ved lagring`() {
        val oppgave1 = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        val oppgave2 = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        repository.lagre(oppgave1)
        assertThrows<IllegalStateException> {
            repository.lagre(oppgave2)
        }
    }
}
