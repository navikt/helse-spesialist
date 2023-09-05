package no.nav.helse.mediator.oppgave

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OppgavelagrerTest {
    private companion object {
        private val OPPGAVETYPE = Oppgavetype.SØKNAD
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "Z999999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Hen Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val BESLUTTER_OID = UUID.randomUUID()
        private val OPPGAVE_ID = Random.nextLong()
        private val saksbehandler = Saksbehandler(
            oid = SAKSBEHANDLER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT
        )
        private val beslutter = Saksbehandler(
            oid = BESLUTTER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT
        )

        private val TOTRINNSVURDERING_OPPRETTET = LocalDateTime.now()
        private val TOTRINNSVURDERING_OPPDATERT = LocalDateTime.now()

        private val CONTEXT_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    @Test
    fun `lagre oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, HENDELSE_ID, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, Oppgavetype.SØKNAD, HENDELSE_ID) }
        verify(exactly = 0) { oppgaveMediator.tildel(any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.avmeld(any()) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling medfører forsøk på å slette eksisterende tildeling`() {
        val oppgave = nyOppgave()
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, Oppgavestatus.AvventerSaksbehandler, null, null) }
        verify(exactly = 0) { oppgaveMediator.tildel(any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.avmeld(any()) }
    }

    @Test
    fun `lagre oppgave uten tildeling eller totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, HENDELSE_ID, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, Oppgavetype.SØKNAD, HENDELSE_ID) }
        verify(exactly = 0) { oppgaveMediator.tildel(any(), any(), any()) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `lagre oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, HENDELSE_ID, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, Oppgavetype.SØKNAD, HENDELSE_ID) }
        verify(exactly = 0) { oppgaveMediator.tildel(any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, 1L, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    @Test
    fun `lagre oppgave uten totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.forsøkTildeling(saksbehandler, harTilgangTil = { _, _ -> true })
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, HENDELSE_ID, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, Oppgavetype.SØKNAD, HENDELSE_ID) }
        verify(exactly = 1) { oppgaveMediator.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID, false) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `lagre oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildeling(saksbehandler, harTilgangTil = { _, _ -> true })
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.lagre(oppgaveMediator, HENDELSE_ID, CONTEXT_ID)
        verify(exactly = 1) { oppgaveMediator.opprett(OPPGAVE_ID, CONTEXT_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, Oppgavetype.SØKNAD, HENDELSE_ID) }
        verify(exactly = 1) { oppgaveMediator.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID, false) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, 1L, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling eller totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, Oppgavestatus.Ferdigstilt, SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID) }
        verify(exactly = 0) { oppgaveMediator.tildel(any(), any(), any()) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `oppdatere oppgave uten tildeling`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, Oppgavestatus.Ferdigstilt, SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID) }
        verify(exactly = 0) { oppgaveMediator.tildel(any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, 1L, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    @Test
    fun `oppdatere oppgave uten totrinnsvurdering`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = false)
        oppgave.forsøkTildeling(saksbehandler, harTilgangTil = { _, _ -> true })
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, Oppgavestatus.Ferdigstilt, SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID) }
        verify(exactly = 1) { oppgaveMediator.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID, false) }
        verify(exactly = 0) { oppgaveMediator.lagreTotrinnsvurdering(any()) }
    }

    @Test
    fun `oppdatere oppgave`() {
        val oppgave = nyOppgave(medTotrinnsvurdering = true)
        oppgave.forsøkTildeling(saksbehandler, harTilgangTil = { _, _ -> true })
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        oppgave.ferdigstill()
        val oppgavelagrer = Oppgavelagrer()
        oppgave.accept(oppgavelagrer)

        oppgavelagrer.oppdater(oppgaveMediator)
        verify(exactly = 1) { oppgaveMediator.oppdater(OPPGAVE_ID, Oppgavestatus.Ferdigstilt, SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID) }
        verify(exactly = 1) { oppgaveMediator.tildel(OPPGAVE_ID, SAKSBEHANDLER_OID, false) }
        verify(exactly = 1) { oppgaveMediator.lagreTotrinnsvurdering(TotrinnsvurderingFraDatabase(VEDTAKSPERIODE_ID, false, SAKSBEHANDLER_OID, BESLUTTER_OID, 1L, TOTRINNSVURDERING_OPPRETTET, TOTRINNSVURDERING_OPPDATERT)) }
    }

    private fun nyOppgave(medTotrinnsvurdering: Boolean = false) = Oppgave.oppgaveMedEgenskaper(
        id = OPPGAVE_ID,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        utbetalingId = UTBETALING_ID,
        egenskaper = listOf(OPPGAVETYPE),
        totrinnsvurdering = if (medTotrinnsvurdering) nyTotrinnsvurdering() else null
    )

    private fun nyTotrinnsvurdering() = Totrinnsvurdering(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        erRetur = false,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        utbetalingIdRef = 1L,
        opprettet = TOTRINNSVURDERING_OPPRETTET,
        oppdatert = TOTRINNSVURDERING_OPPDATERT
    )
}