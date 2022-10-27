package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.spesialist.api.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import no.nav.helse.spesialist.api.graphql.enums.Utbetalingtype as GraphQLUtbetalingtype

internal class OpprettSaksbehandleroppgaveCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val FNR = "12345678910"
        private val hendelseId = UUID.randomUUID()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val snapshotMediator = mockk<SnapshotMediator>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
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
        snapshotMediator = snapshotMediator,
    )

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(oppgaveMediator)
    }

    @Test
    fun `oppretter oppgave`() {
        every { reservasjonDao.hentReservertTil(FNR) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.søknad(VEDTAKSPERIODE_ID, UTBETALING_ID)) }
    }

    @Test
    fun `oppretter stikkprøve`() {
        every { reservasjonDao.hentReservertTil(FNR) } returns null
        every { automatisering.erStikkprøve(VEDTAKSPERIODE_ID, any()) } returns true
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.stikkprøve(VEDTAKSPERIODE_ID, UTBETALING_ID)) }
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for fortrlig adressebeskyttelse`() {
        every { reservasjonDao.hentReservertTil(FNR) } returns null
        every { personDao.findAdressebeskyttelse(FNR) } returns Adressebeskyttelse.Fortrolig
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            oppgaveMediator.opprett(
                Oppgave.fortroligAdressebeskyttelse(
                    VEDTAKSPERIODE_ID,
                    UTBETALING_ID
                )
            )
        }
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for utbetaling til sykmeldt`() {
        every { reservasjonDao.hentReservertTil(FNR) } returns null
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(personbeløp = 500)
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.utbetalingTilSykmeldt(VEDTAKSPERIODE_ID, UTBETALING_ID)) }
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for delvis refusjon`() {
        every { reservasjonDao.hentReservertTil(FNR) } returns null
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(
            personbeløp = 500,
            arbeidsgiverbeløp = 500
        )
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.opprett(Oppgave.delvisRefusjon(VEDTAKSPERIODE_ID, UTBETALING_ID)) }
    }

    private fun enUtbetaling(personbeløp: Int = 0, arbeidsgiverbeløp: Int = 0): GraphQLUtbetaling =
        GraphQLUtbetaling(
            id = UTBETALING_ID.toString(),
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
            arbeidsgiverNettoBelop = arbeidsgiverbeløp,
            personFagsystemId = "EN_FAGSYSTEMID",
            personNettoBelop = personbeløp,
            statusEnum = GraphQLUtbetalingstatus.GODKJENT,
            typeEnum = GraphQLUtbetalingtype.UTBETALING,
            vurdering = null,
            personoppdrag = null,
            arbeidsgiveroppdrag = null,
        )
}
