package no.nav.helse.spesialist.db

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.mediator.oppgave.Oppgavelagrer
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextLong

class OppgavelagrerTest : DatabaseIntegrationTest() {
    private val OPPGAVETYPE = SØKNAD
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    private val BEHANDLING_ID = UUID.randomUUID()
    override val UTBETALING_ID: UUID = UUID.randomUUID()
    private val BESLUTTER_OID = UUID.randomUUID()
    override var OPPGAVE_ID = nextLong()
    private val saksbehandler =
        Saksbehandler(
            oid = SAKSBEHANDLER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = { _, _ -> false },
        )
    private val beslutter =
        Saksbehandler(
            oid = BESLUTTER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = { _, _ -> false },
        )

    private val TOTRINNSVURDERING_OPPRETTET = LocalDateTime.now()
    private val TOTRINNSVURDERING_OPPDATERT = LocalDateTime.now()

    override val HENDELSE_ID: UUID = UUID.randomUUID()

    private val tildelingRepository = mockk<TildelingDao>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        clearMocks(tildelingRepository, oppgaveService)
    }

    @Test
    fun `lagre oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                godkjenningsbehovId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 1) { tildelingRepository.avmeld(OPPGAVE_ID) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.oppdater(oppgaveService, oppgave.toDto())
        verify(exactly = 1) { oppgaveService.oppdater(OPPGAVE_ID, "AvventerSaksbehandler", null, null, listOf(
            EgenskapForDatabase.SØKNAD
        )) }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 1) { tildelingRepository.avmeld(OPPGAVE_ID) }
    }

    @Test
    fun `lagre oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                godkjenningsbehovId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
    }

    @Test
    fun `lagre oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                godkjenningsbehovId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 1) { tildelingRepository.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.oppdater(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(
                EgenskapForDatabase.SØKNAD
            ))
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
    }

    @Test
    fun `oppdatere oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.oppdater(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(
                EgenskapForDatabase.SØKNAD
            ))
        }
    }

    private fun nyOppgave(medTotrinnsvurdering: Boolean = false) =
        Oppgave.nyOppgave(
            id = OPPGAVE_ID,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            behandlingId = BEHANDLING_ID,
            utbetalingId = UTBETALING_ID,
            hendelseId = HENDELSE_ID,
            egenskaper = setOf(OPPGAVETYPE),
            totrinnsvurdering = if (medTotrinnsvurdering) nyTotrinnsvurdering() else null,
            kanAvvises = true,
        )

    private fun nyTotrinnsvurdering() =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(nextLong()),
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            utbetalingId = UTBETALING_ID,
            opprettet = TOTRINNSVURDERING_OPPRETTET,
            oppdatert = TOTRINNSVURDERING_OPPDATERT,
            overstyringer = emptyList(),
            ferdigstilt = false,
        )
}
