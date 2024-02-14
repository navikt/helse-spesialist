package no.nav.helse.mediator.oppgave

import DatabaseIntegrationTest
import TilgangskontrollForTestHarIkkeTilgang
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OppgavelagrerTest: DatabaseIntegrationTest() {
    private val OPPGAVETYPE = SØKNAD
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    override val UTBETALING_ID = UUID.randomUUID()
    override val SAKSBEHANDLER_IDENT = "Z999999"
    override val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
    override val SAKSBEHANDLER_NAVN = "Hen Saksbehandler"
    override val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val BESLUTTER_OID = UUID.randomUUID()
    override var OPPGAVE_ID = Random.nextLong()
    private val saksbehandler = Saksbehandler(
        oid = SAKSBEHANDLER_OID,
        epostadresse = SAKSBEHANDLER_EPOST,
        navn = SAKSBEHANDLER_NAVN,
        ident = SAKSBEHANDLER_IDENT,
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
    )
    private val beslutter = Saksbehandler(
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

    private val nyTildelingDao = mockk<TildelingDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        clearMocks(nyTildelingDao, oppgaveMediator)
    }

    @Test
    fun `lagre oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, "SØKNAD", listOf(EgenskapForDatabase.SØKNAD), HENDELSE_ID, true) }
        verify(exactly = 0) { nyTildelingDao.tildel(any(), any(), any()) }
        verify(exactly = 1) { nyTildelingDao.avmeld(OPPGAVE_ID) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, "AvventerSaksbehandler", null, null, listOf(EgenskapForDatabase.SØKNAD)) }
        verify(exactly = 0) { nyTildelingDao.tildel(any(), any(), any()) }
        verify(exactly = 1) { nyTildelingDao.avmeld(OPPGAVE_ID) }
    }

    @Test
    fun `lagre oppgave uten tildeling eller totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, "SØKNAD", listOf(EgenskapForDatabase.SØKNAD), HENDELSE_ID, true) }
        verify(exactly = 0) { nyTildelingDao.tildel(any(), any(), any()) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `lagre oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, "SØKNAD", listOf(EgenskapForDatabase.SØKNAD), HENDELSE_ID, true) }
        verify(exactly = 0) { nyTildelingDao.tildel(any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, UTBETALING_ID, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    @Test
    fun `lagre oppgave uten totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, "SØKNAD", listOf(EgenskapForDatabase.SØKNAD), HENDELSE_ID, true) }
        verify(exactly = 1) { nyTildelingDao.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID, false) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `lagre oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, "SØKNAD", listOf(EgenskapForDatabase.SØKNAD), HENDELSE_ID, true) }
        verify(exactly = 1) { nyTildelingDao.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID, false) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, UTBETALING_ID, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling eller totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD)) }
        verify(exactly = 0) { nyTildelingDao.tildel(any(), any(), any()) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD)) }
        verify(exactly = 0) { nyTildelingDao.tildel(any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, UTBETALING_ID, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    @Test
    fun `oppdatere oppgave uten totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD)) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `oppdatere oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildelingVedReservasjon(saksbehandler)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer(nyTildelingDao)
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, "Ferdigstilt", SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID, listOf(EgenskapForDatabase.SØKNAD)) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, UTBETALING_ID, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    private fun nyOppgave(medTotrinnsvurdering: Boolean = false) = Oppgave.nyOppgave(
        id = OPPGAVE_ID,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        utbetalingId = UTBETALING_ID,
        hendelseId = HENDELSE_ID,
        egenskaper = listOf(OPPGAVETYPE),
        totrinnsvurdering = if (medTotrinnsvurdering) nyTotrinnsvurdering() else null,
        kanAvvises = true,
    )

    private fun nyTotrinnsvurdering() = Totrinnsvurdering(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        erRetur = false,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        utbetalingId = UTBETALING_ID,
        opprettet = TOTRINNSVURDERING_OPPRETTET,
        oppdatert = TOTRINNSVURDERING_OPPDATERT
    )
}