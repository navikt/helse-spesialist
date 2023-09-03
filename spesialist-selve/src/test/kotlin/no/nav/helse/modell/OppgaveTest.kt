package no.nav.helse.modell

import io.mockk.clearMocks
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveVisitor
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.STIKKPRØVE
import no.nav.helse.spesialist.api.oppgave.Oppgavetype.SØKNAD
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextLong

internal class OppgaveTest {
    private companion object {
        private val OPPGAVETYPE = SØKNAD
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val SAKSBEHANDLER_IDENT = "Z999999"
        private const val SAKSBEHANDLER_EPOST = "saksbehandler@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Hen Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val OPPGAVE_ID = nextLong()
        private val saksbehandler = Saksbehandler(
            oid = SAKSBEHANDLER_OID,
            epostadresse = SAKSBEHANDLER_EPOST,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT
        )
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>()
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao, tildelingDao)
    }

    @Test
    fun `gjenoppretter oppgave`() {
        val oppgave1 = Oppgave(
            OPPGAVE_ID,
            OPPGAVETYPE,
            Oppgavestatus.AvventerSaksbehandler,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        val oppgave2 = Oppgave(
            OPPGAVE_ID,
            OPPGAVETYPE,
            Oppgavestatus.AvventerSaksbehandler,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        val oppgave3 = Oppgave(
            OPPGAVE_ID,
            OPPGAVETYPE,
            Oppgavestatus.AvventerSystem,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1, oppgave3)
    }

    @Test
    fun `Forsøker tildeling ved reservasjon`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildeling(saksbehandler, false, harTilgangTil = { _, _ -> true })

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(false, påVent)
            assertEquals(saksbehandler, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon med påVent`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.forsøkTildeling(saksbehandler, påVent = true, harTilgangTil = { _, _ -> true })

        inspektør(oppgave) {
            assertEquals(true, tildelt)
            assertEquals(true, påVent)
            assertEquals(saksbehandler, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon ved stikkprøve`() {
        val oppgave = nyOppgave(STIKKPRØVE)
        oppgave.forsøkTildeling(saksbehandler, påVent = true, harTilgangTil = { _, _ -> true })

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Forsøker tildeling ved reservasjon ved manglende tilgang`() {
        val oppgave = nyOppgave(STIKKPRØVE)
        oppgave.forsøkTildeling(saksbehandler, påVent = true, harTilgangTil = { _, _ -> false })

        inspektør(oppgave) {
            assertEquals(false, tildelt)
            assertEquals(false, påVent)
            assertEquals(null, tildeltTil)
        }
    }

    @Test
    fun `Setter oppgavestatus til AvventerSystem når oppgaven avventer system`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avventerSystem(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)

        inspektør(oppgave) {
            assertEquals(Oppgavestatus.AvventerSystem, status)
        }
    }

    @Test
    fun `Setter oppgavestatus til Invalidert når oppgaven avbrytes`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.avbryt()

        inspektør(oppgave) {
            assertEquals(Oppgavestatus.Invalidert, status)
        }
    }

    @Test
    fun `Setter oppgavestatus til Ferdigstilt når oppgaven ferdigstilles`() {
        val oppgave = nyOppgave(SØKNAD)
        oppgave.ferdigstill()

        inspektør(oppgave) {
            assertEquals(Oppgavestatus.Ferdigstilt, status)
        }
    }

    @Test
    fun equals() {
        val gjenopptattOppgave = Oppgave(
            1L,
            OPPGAVETYPE,
            Oppgavestatus.AvventerSaksbehandler,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        val oppgave1 = Oppgave.oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, listOf(SØKNAD))
        val oppgave2 = Oppgave.oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, listOf(SØKNAD))
        val oppgave3 = Oppgave.oppgaveMedEgenskaper(OPPGAVE_ID, UUID.randomUUID(), UTBETALING_ID, listOf(SØKNAD))
        val oppgave4 = Oppgave.oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, listOf(STIKKPRØVE))
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
        assertNotEquals(oppgave1, oppgave3)
        assertNotEquals(oppgave1.hashCode(), oppgave3.hashCode())
        assertNotEquals(oppgave1, oppgave4)
        assertNotEquals(oppgave1.hashCode(), oppgave4.hashCode())

        gjenopptattOppgave.ferdigstill(SAKSBEHANDLER_IDENT, SAKSBEHANDLER_OID)
        assertNotEquals(gjenopptattOppgave.hashCode(), oppgave2.hashCode())
        assertNotEquals(gjenopptattOppgave, oppgave2)
        assertNotEquals(gjenopptattOppgave, oppgave3)
        assertNotEquals(gjenopptattOppgave, oppgave4)

        oppgave2.ferdigstill("ANNEN_SAKSBEHANDLER", UUID.randomUUID())
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
    }

    private fun nyOppgave(vararg egenskaper: Oppgavetype) =
        Oppgave.oppgaveMedEgenskaper(OPPGAVE_ID, VEDTAKSPERIODE_ID, UTBETALING_ID, egenskaper.toList())

    private fun inspektør(oppgave: Oppgave, block: OppgaveInspektør.() -> Unit) {
        val inspektør = OppgaveInspektør()
        oppgave.accept(inspektør)
        block(inspektør)
    }

    private class OppgaveInspektør: OppgaveVisitor {
        lateinit var status: Oppgavestatus
        var tildelt: Boolean = false
        var påVent: Boolean = false
        var tildeltTil: Saksbehandler? = null
        override fun visitOppgave(
            id: Long,
            type: Oppgavetype,
            status: Oppgavestatus,
            vedtaksperiodeId: UUID,
            utbetalingId: UUID,
            ferdigstiltAvOid: UUID?,
            ferdigstiltAvIdent: String?,
            egenskaper: List<Oppgavetype>,
            tildelt: Saksbehandler?,
            påVent: Boolean
        ) {
            this.status = status
            this.tildelt = tildelt != null
            this.tildeltTil = tildelt
            this.påVent = påVent
        }
    }
}
