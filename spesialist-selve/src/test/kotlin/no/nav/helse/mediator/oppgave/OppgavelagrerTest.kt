package no.nav.helse.mediator.oppgave

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.TildelingRepository
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgave.Companion.toDto
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Saksbehandler.Companion.toDto
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDto
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class OppgavelagrerTest : DatabaseIntegrationTest() {
    private val OPPGAVETYPE = SØKNAD
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    private val BEHANDLING_ID = UUID.randomUUID()
    override val UTBETALING_ID = UUID.randomUUID()
    override val SAKSBEHANDLER_IDENT = "Z999999"
    override val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
    override val SAKSBEHANDLER_NAVN = "Hen Saksbehandler"
    override val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val BESLUTTER_OID = UUID.randomUUID()
    override var OPPGAVE_ID = Random.nextLong()
    private val saksbehandler =
        Saksbehandler(
            oid = SAKSBEHANDLER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        )
    private val beslutter =
        Saksbehandler(
            oid = BESLUTTER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        )

    private val TOTRINNSVURDERING_OPPRETTET = LocalDateTime.now()
    private val TOTRINNSVURDERING_OPPDATERT = LocalDateTime.now()

    private val CONTEXT_ID = UUID.randomUUID()
    override val HENDELSE_ID = UUID.randomUUID()

    private val tildelingRepository = mockk<TildelingRepository>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        clearMocks(tildelingRepository, oppgaveService)
    }

    @Test
    fun `lagre oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto(), CONTEXT_ID)
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                contextId = CONTEXT_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                hendelseId = HENDELSE_ID,
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
        verify(exactly = 1) { oppgaveService.oppdater(OPPGAVE_ID, "AvventerSaksbehandler", null, null, listOf(EgenskapForDatabase.SØKNAD)) }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 1) { tildelingRepository.avmeld(OPPGAVE_ID) }
    }

    @Test
    fun `lagre oppgave uten tildeling eller totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto(), CONTEXT_ID)
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                contextId = CONTEXT_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                hendelseId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 0) { oppgaveService.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `lagre oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto(), CONTEXT_ID)
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                contextId = CONTEXT_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                hendelseId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 1) {
            oppgaveService.lagreTotrinnsvurdering(
                TotrinnsvurderingDto(
                    VEDTAKSPERIODE_ID,
                    false,
                    saksbehandler.toDto(),
                    beslutter.toDto(),
                    UTBETALING_ID,
                    TOTRINNSVURDERING_OPPRETTET,
                    TOTRINNSVURDERING_OPPDATERT,
                ),
            )
        }
    }

    @Test
    fun `lagre oppgave uten totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto(), CONTEXT_ID)
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                contextId = CONTEXT_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                hendelseId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 1) { tildelingRepository.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID) }
        verify(exactly = 0) { oppgaveService.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `lagre oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.lagre(oppgaveService, oppgave.toDto(), CONTEXT_ID)
        verify(exactly = 1) {
            oppgaveService.opprett(
                id = OPPGAVE_ID,
                contextId = CONTEXT_ID,
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                behandlingId = BEHANDLING_ID,
                utbetalingId = UTBETALING_ID,
                egenskaper = listOf(EgenskapForDatabase.SØKNAD),
                hendelseId = HENDELSE_ID,
                kanAvvises = true,
            )
        }
        verify(exactly = 1) { tildelingRepository.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID) }
        verify(exactly = 1) {
            oppgaveService.lagreTotrinnsvurdering(
                TotrinnsvurderingDto(
                    VEDTAKSPERIODE_ID,
                    false,
                    saksbehandler.toDto(),
                    beslutter.toDto(),
                    UTBETALING_ID,
                    TOTRINNSVURDERING_OPPRETTET,
                    TOTRINNSVURDERING_OPPDATERT,
                ),
            )
        }
    }

    @Test
    fun `oppdatere oppgave uten tildeling eller totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.oppdater(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD))
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 0) { oppgaveService.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.oppdater(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD))
        }
        verify(exactly = 0) { tildelingRepository.tildel(any(), any()) }
        verify(exactly = 1) {
            oppgaveService.lagreTotrinnsvurdering(
                TotrinnsvurderingDto(
                    VEDTAKSPERIODE_ID,
                    false,
                    saksbehandler.toDto(),
                    beslutter.toDto(),
                    UTBETALING_ID,
                    TOTRINNSVURDERING_OPPRETTET,
                    TOTRINNSVURDERING_OPPDATERT,
                ),
            )
        }
    }

    @Test
    fun `oppdatere oppgave uten totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(tildelingRepository)

        oppgavelagrer.oppdater(oppgaveService, oppgave.toDto())
        verify(exactly = 1) {
            oppgaveService.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD))
        }
        verify(exactly = 0) { oppgaveService.lagreTotrinnsvurdering(any()) }
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
            oppgaveService.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD))
        }
        verify(exactly = 1) {
            oppgaveService.lagreTotrinnsvurdering(
                TotrinnsvurderingDto(
                    VEDTAKSPERIODE_ID,
                    false,
                    saksbehandler.toDto(),
                    beslutter.toDto(),
                    UTBETALING_ID,
                    TOTRINNSVURDERING_OPPRETTET,
                    TOTRINNSVURDERING_OPPDATERT,
                ),
            )
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
        Totrinnsvurdering(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            utbetalingId = UTBETALING_ID,
            opprettet = TOTRINNSVURDERING_OPPRETTET,
            oppdatert = TOTRINNSVURDERING_OPPDATERT,
        )
}
