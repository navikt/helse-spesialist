package no.nav.helse.modell.kommando

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveInspector.Companion.oppgaveinspektør
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.OVERGANG_FRA_IT
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import no.nav.helse.spleis.graphql.enums.Utbetalingtype as GraphQLUtbetalingtype

internal class OpprettSaksbehandleroppgaveCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val FNR = "12345678910"
        private val hendelseId = UUID.randomUUID()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val snapshotMediator = mockk<SnapshotMediator>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val vergemålDao = mockk<VergemålDao>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)
    private lateinit var context: CommandContext
    private lateinit var contextId: UUID
    private lateinit var utbetalingstype: Utbetalingtype
    private val command get() = opprettSaksbehandlerOppgaveCommand()

    @BeforeEach
    fun setup() {
        contextId = UUID.randomUUID()
        context = CommandContext(contextId)
        utbetalingstype = Utbetalingtype.UTBETALING
        clearMocks(oppgaveMediator)
    }

    @Test
    fun `oppretter oppgave`() {
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }
        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter stikkprøve`() {
        every { automatisering.erStikkprøve(VEDTAKSPERIODE_ID, any()) } returns true
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, STIKKPRØVE, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter risk QA`() {
        every { risikovurderingDao.kreverSupersaksbehandler(VEDTAKSPERIODE_ID) } returns true
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, RISK_QA, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter revurdering`() {
        utbetalingstype = Utbetalingtype.REVURDERING
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(REVURDERING, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for fortrlig adressebeskyttelse`() {
        every { personDao.findAdressebeskyttelse(FNR) } returns Adressebeskyttelse.Fortrolig
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, FORTROLIG_ADRESSE, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for utbetaling til sykmeldt`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(personbeløp = 500)
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, UTBETALING_TIL_SYKMELDT, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for delvis refusjon`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(
            personbeløp = 500,
            arbeidsgiverbeløp = 500
        )
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, DELVIS_REFUSJON, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `oppretter oppgave med egenskap utbetaling til arbeidsgiver`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(arbeidsgiverbeløp = 500)
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(UTBETALING_TIL_ARBEIDSGIVER))
        }
    }

    @Test
    fun `oppretter oppgave med egenskap ingen utbetaling`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling(personbeløp = 0, arbeidsgiverbeløp = 0)
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(INGEN_UTBETALING))
        }
    }

    @Test
    fun `oppretter oppgave med egenskap haster`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        every { sykefraværstilfelle.haster(VEDTAKSPERIODE_ID) } returns true
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(HASTER))
        }
    }

    @Test
    fun `oppretter oppgave med egen ansatt`() {
        Toggle.EgenAnsatt.enable()
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        every { egenAnsattDao.erEgenAnsatt(FNR) } returns true
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(EGEN_ANSATT))
        }
        Toggle.EgenAnsatt.disable()
    }

    @Test
    fun `oppretter oppgave med egenskap UTLAND`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        every { personDao.finnEnhetId(FNR) } returns "0393"
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(UTLAND))
        }
    }

    @Test
    fun `oppretter oppgave med egenskap FLERE_ARBEIDSGIVERE`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(opprettSaksbehandlerOppgaveCommand(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE).execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(FLERE_ARBEIDSGIVERE))
        }
    }

    @Test
    fun `oppretter oppgave med egenskap FORLENGELSE`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = FORLENGELSE).execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(Egenskap.FORLENGELSE))
        }
    }

    @Test
    fun `oppretter oppgave med egenskap INFOTRYGDFORLENGELSE`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = INFOTRYGDFORLENGELSE).execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(Egenskap.INFOTRYGDFORLENGELSE))
        }
    }

    @Test
    fun `oppretter oppgave med egenskap OVERGANG_FRA_IT`() {
        every { snapshotMediator.finnUtbetaling(FNR, UTBETALING_ID) } returns enUtbetaling()
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = OVERGANG_FRA_IT).execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        oppgaveinspektør(oppgave) {
            assertTrue(egenskaper.contains(Egenskap.OVERGANG_FRA_IT))
        }
    }

    @Test
    fun `legger ikke til egenskap RISK_QA hvis oppgaven har egenskap REVURDERING`() {
        every { risikovurderingDao.kreverSupersaksbehandler(VEDTAKSPERIODE_ID) } returns true
        utbetalingstype = Utbetalingtype.REVURDERING
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }

        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(REVURDERING, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING), oppgave)
    }

    @Test
    fun `legger til oppgave med kanAvvises lik false`() {
        val slot = slot<((Long) -> Oppgave)>()
        assertTrue(opprettSaksbehandlerOppgaveCommand(kanAvvises = false).execute(context))
        verify(exactly = 1) { oppgaveMediator.nyOppgave(FNR, contextId, capture(slot)) }
        val oppgave = slot.captured.invoke(1L)
        assertEquals(enOppgave(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, kanAvvises = false), oppgave)
    }

    private fun enOppgave(vararg egenskaper: Egenskap, kanAvvises: Boolean = true) =
        Oppgave.nyOppgave(1L, VEDTAKSPERIODE_ID, UTBETALING_ID, UUID.randomUUID(), kanAvvises, egenskaper.toList())


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

    private fun opprettSaksbehandlerOppgaveCommand(inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER, periodetype: Periodetype = FØRSTEGANGSBEHANDLING, kanAvvises: Boolean = true) =
        OpprettSaksbehandleroppgaveCommand(
            fødselsnummer = FNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            hendelseId = hendelseId,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao,
            egenAnsattDao = egenAnsattDao,
            utbetalingId = UTBETALING_ID,
            utbetalingtype = utbetalingstype,
            sykefraværstilfelle = sykefraværstilfelle,
            snapshotMediator = snapshotMediator,
            vergemålDao = vergemålDao,
            inntektskilde = inntektskilde,
            periodetype = periodetype,
            kanAvvises = kanAvvises,
        )
}
