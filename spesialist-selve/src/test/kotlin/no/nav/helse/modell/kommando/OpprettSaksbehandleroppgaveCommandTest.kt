package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.oppgave.Oppgave
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.reservasjon.ReservasjonDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettSaksbehandleroppgaveCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val FNR = "12345678910"
        private val hendelseId = UUID.randomUUID()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private lateinit var context: CommandContext
    private val command = OpprettSaksbehandleroppgaveCommand(
        fødselsnummer = FNR,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        oppgaveMediator = oppgaveMediator,
        automatisering = automatisering,
        hendelseId = hendelseId,
        personDao = personDao,
        risikovurderingDao = risikovurderingDao,
        utbetalingId = UTBETALING_ID,
        utbetalingtype = Utbetalingtype.UTBETALING,
        periodeFom = 1.januar,
        periodeTom = 31.januar,
        snapshotDao = snapshotDao,
    )

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(oppgaveMediator)
    }

    @Test
    fun `oppretter oppgave`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.søknad(VEDTAKSPERIODE_ID, UTBETALING_ID)) }
    }

    @Test
    fun `oppretter stikkprøve`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        every { automatisering.erStikkprøve(VEDTAKSPERIODE_ID, any()) } returns true
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.stikkprøve(VEDTAKSPERIODE_ID, UTBETALING_ID)) }
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for fortrlig adressebeskyttelse`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        every { personDao.findAdressebeskyttelse(FNR) } returns Adressebeskyttelse.Fortrolig
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.fortroligAdressebeskyttelse(VEDTAKSPERIODE_ID, UTBETALING_ID))}
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for utbetaling til sykmeldt`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        every { snapshotDao.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(personbeløp = 500)
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.utbetalingTilSykmeldt(VEDTAKSPERIODE_ID, UTBETALING_ID))}
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for delvis refusjon`() {
        every { reservasjonDao.hentReservasjonFor(FNR) } returns null
        every { snapshotDao.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(personbeløp = 500, arbeidsgiverbeløp = 500)
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.delvisRefusjon(VEDTAKSPERIODE_ID, UTBETALING_ID))}
    }

    private fun enUtbetaling(personbeløp: Int = 0, arbeidsgiverbeløp: Int = 0): GraphQLUtbetaling =
        GraphQLUtbetaling(
            id = UTBETALING_ID.toString(),
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
            arbeidsgiverNettoBelop = arbeidsgiverbeløp,
            personFagsystemId = "EN_FAGSYSTEMID",
            personNettoBelop = personbeløp,
            statusEnum = GraphQLUtbetalingstatus.GODKJENT,
            typeEnum = no.nav.helse.mediator.graphql.enums.Utbetalingtype.UTBETALING,
            vurdering = null,
            personoppdrag = null,
            arbeidsgiveroppdrag = null,
        )
}
