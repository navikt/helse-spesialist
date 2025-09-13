package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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
    private val godkjenningsbehovId: UUID = UUID.randomUUID()

    private val saksbehandler = nyLegacySaksbehandler()

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
        val funnetOppgave = repository.finn(oppgave.id)
        assertNotNull(funnetOppgave)
        assertEquals(oppgave, funnetOppgave)
    }

    @Test
    fun `tildel oppgave`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.forsøkTildeling(saksbehandler, emptySet())

        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id)
        assertNotNull(funnetOppgave)
        assertEquals(saksbehandler.saksbehandlerOid, funnetOppgave.tildeltTil)
    }

    @Test
    fun `avmeld oppgave`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.forsøkTildeling(saksbehandler, emptySet())
        repository.lagre(oppgave)

        oppgave.forsøkAvmelding(saksbehandler)
        repository.lagre(oppgave)

        val funnetOppgave = repository.finn(oppgave.id)
        assertNotNull(funnetOppgave)
        assertEquals(null, funnetOppgave.tildeltTil)
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
        val funnetOppgave = repository.finn(oppgave.id)
        assertNotNull(funnetOppgave)
        assertEquals(setOf(Egenskap.EGEN_ANSATT, SØKNAD), funnetOppgave.egenskaper)
    }

    @Test
    fun `endre tilstand`() {
        opprettSaksbehandler(saksbehandler.oid, saksbehandler.navn, saksbehandler.epostadresse, saksbehandler.ident())
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
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id)
        assertNotNull(funnetOppgave)
        assertEquals(Oppgave.AvventerSystem, funnetOppgave.tilstand)
    }

    @Test
    fun ferdigstill() {
        opprettSaksbehandler(saksbehandler.oid, saksbehandler.navn, saksbehandler.epostadresse, saksbehandler.ident())
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
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave.ferdigstill()
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id)
        assertNotNull(funnetOppgave)
        assertEquals(Oppgave.Ferdigstilt, funnetOppgave.tilstand)
        assertEquals(saksbehandler.ident(), funnetOppgave.ferdigstiltAvIdent)
        assertEquals(saksbehandler.oid, funnetOppgave.ferdigstiltAvOid)
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

    @Test
    fun `skal lagre ny oppgave dersom eksisterende oppgave på samme person ikke avventer saksbehandler`() {
        val oppgave1 = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave1.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave1.ferdigstill()
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
        assertDoesNotThrow {
            repository.lagre(oppgave2)
        }
    }

    @Test
    fun `finner tilstand på oppgave som avventer saksbehandler`() {
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

        val oppgaveTilstand = repository.finnSisteOppgaveForUtbetaling(utbetalingId)?.tilstand
        assertEquals(Oppgave.AvventerSaksbehandler::class, oppgaveTilstand?.let { it::class })
    }

    @Test
    fun `finner tilstand på oppgave som avventer system`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        repository.lagre(oppgave)

        val oppgaveTilstand = repository.finnSisteOppgaveForUtbetaling(utbetalingId)?.tilstand
        assertEquals(Oppgave.AvventerSystem::class, oppgaveTilstand?.let { it::class })
    }

    @Test
    fun `finner tilstand på ferdigstilt oppgave`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave.ferdigstill()
        repository.lagre(oppgave)

        val oppgaveTilstand = repository.finnSisteOppgaveForUtbetaling(utbetalingId)?.tilstand
        assertEquals(Oppgave.Ferdigstilt::class, oppgaveTilstand?.let { it::class })
    }

    @Test
    fun `finner tilstand på invalidert oppgave`() {
        val oppgave = Oppgave.ny(
            id = nextLong(),
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave.avbryt()
        repository.lagre(oppgave)

        val oppgaveTilstand = repository.finnSisteOppgaveForUtbetaling(utbetalingId)?.tilstand
        assertEquals(Oppgave.Invalidert::class, oppgaveTilstand?.let { it::class })
    }

    @Test
    fun `finner tilstand på ferdigstilt oppgave når det finnes en tidligere invalidert oppgave`() {
        val oppgaveId = nextLong()
        val gammelOppgave = Oppgave.ny(
            id = oppgaveId,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        gammelOppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        gammelOppgave.avbryt()
        repository.lagre(gammelOppgave)

        val oppgave = Oppgave.ny(
            id = oppgaveId+1,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(SØKNAD)
        )
        oppgave.avventerSystem(saksbehandler.ident(), saksbehandler.oid)
        oppgave.ferdigstill()
        repository.lagre(oppgave)

        val oppgaveTilstand = repository.finnSisteOppgaveForUtbetaling(utbetalingId)?.tilstand
        assertEquals(Oppgave.Ferdigstilt::class, oppgaveTilstand?.let { it::class })
    }

    @Test
    fun `gir null som tilstand når det ikke finnes noen oppgave for utbetalingen`() {
        val oppgaveTilstand = repository.finnSisteOppgaveForUtbetaling(utbetalingId)
        assertEquals(null, oppgaveTilstand?.let { it::class })
    }
}
