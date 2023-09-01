package no.nav.helse.modell

import io.mockk.clearMocks
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveVisitor
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
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val OPPGAVE_ID = nextLong()
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
    fun `Setter oppgavestatus til INVALIDERT når oppgaven avbrytes`() {
        val oppgave = Oppgave(
            OPPGAVE_ID,
            OPPGAVETYPE,
            Oppgavestatus.AvventerSaksbehandler,
            VEDTAKSPERIODE_ID,
            utbetalingId = UTBETALING_ID
        )
        oppgave.avbryt()
        val visitor = object : OppgaveVisitor {
            lateinit var status: Oppgavestatus
            override fun visitOppgave(
                id: Long,
                type: Oppgavetype,
                status: Oppgavestatus,
                vedtaksperiodeId: UUID,
                utbetalingId: UUID,
                ferdigstiltAvOid: UUID?,
                ferdigstiltAvIdent: String?,
                egenskaper: List<Oppgavetype>
            ) {
                this.status = status
            }
        }
        oppgave.accept(visitor)
        assertEquals(Oppgavestatus.Invalidert, visitor.status)
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

        gjenopptattOppgave.ferdigstill(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        assertNotEquals(gjenopptattOppgave.hashCode(), oppgave2.hashCode())
        assertNotEquals(gjenopptattOppgave, oppgave2)
        assertNotEquals(gjenopptattOppgave, oppgave3)
        assertNotEquals(gjenopptattOppgave, oppgave4)

        oppgave2.ferdigstill("ANNEN_SAKSBEHANDLER", UUID.randomUUID())
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
    }
}
