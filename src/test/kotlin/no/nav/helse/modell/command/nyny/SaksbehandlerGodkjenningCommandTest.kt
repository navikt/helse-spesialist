package no.nav.helse.modell.command.nyny

import io.mockk.*
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.tildeling.ReservasjonDao
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class SaksbehandlerGodkjenningCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FNR = "12345678910"
        private const val JSON = "{}"
        private const val SAKSBEHANDLER = "Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private const val EPOST = "saksbehandler@nav.no"
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private val OPPGAVE_ID = nextLong()
        private val hendelseId = UUID.randomUUID()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private lateinit var context: CommandContext
    private val command = SaksbehandlerGodkjenningCommand(
        FNR,
        VEDTAKSPERIODE_ID,
        JSON,
        reservasjonDao,
        oppgaveMediator,
        automatisering,
        hendelseId
    )
    private lateinit var forventetOppgave: Oppgave

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        forventetOppgave =
            Oppgave.avventerSaksbehandler(SaksbehandlerGodkjenningCommand::class.java.simpleName, VEDTAKSPERIODE_ID)
        clearMocks(oppgaveMediator)
    }

    @Test
    fun `oppretter oppgave`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        assertFalse(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(forventetOppgave) }
    }

    @Test
    fun `oppretter oppgave med reservasjon`() {
        val reservasjon = Pair(UUID.randomUUID(), LocalDateTime.now())
        every { reservasjonDao.hentReservasjonFor(FNR) } returns reservasjon
        assertFalse(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.tildel(forventetOppgave, reservasjon.first, reservasjon.second) }
    }

    @Test
    fun `ferdigstiller oppgave`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        context.add(
            SaksbehandlerLøsning(
                true,
                SAKSBEHANDLER,
                SAKSBEHANDLER_OID,
                EPOST,
                GODKJENTTIDSPUNKT,
                null,
                null,
                null,
                OPPGAVE_ID
            )
        )
        assertTrue(command.execute(context))
        assertEquals(1, context.meldinger().size)

        verify(ordering = Ordering.SEQUENCE) {
            oppgaveMediator.nyOppgave(forventetOppgave)
            oppgaveMediator.ferdigstill(forventetOppgave, OPPGAVE_ID, SAKSBEHANDLER, SAKSBEHANDLER_OID)
        }
    }

    @Test
    fun `ferdigstilling av oppgave når det finnes en reservasjon`() {
        val reservasjon = Pair(UUID.randomUUID(), LocalDateTime.now())
        every { reservasjonDao.hentReservasjonFor(FNR) } returns reservasjon

        context.add(SaksbehandlerLøsning(true, SAKSBEHANDLER, SAKSBEHANDLER_OID, EPOST, GODKJENTTIDSPUNKT, null, null, null, OPPGAVE_ID))
        assertTrue(command.execute(context))

        verify(ordering = Ordering.SEQUENCE) {
            oppgaveMediator.tildel(forventetOppgave, reservasjon.first, reservasjon.second)
            oppgaveMediator.ferdigstill(forventetOppgave, OPPGAVE_ID, SAKSBEHANDLER, SAKSBEHANDLER_OID)
        }
    }

    @Test
    fun `ferdigstiller oppgave ved svar fra saksbehandler`() {
        context.add(
            SaksbehandlerLøsning(
                true,
                SAKSBEHANDLER,
                SAKSBEHANDLER_OID,
                EPOST,
                GODKJENTTIDSPUNKT,
                null,
                null,
                null,
                OPPGAVE_ID
            )
        )
        assertTrue(command.resume(context))
        assertEquals(1, context.meldinger().size)
        verify(exactly = 1) { oppgaveMediator.ferdigstill(forventetOppgave, OPPGAVE_ID, SAKSBEHANDLER, SAKSBEHANDLER_OID) }
    }
}
