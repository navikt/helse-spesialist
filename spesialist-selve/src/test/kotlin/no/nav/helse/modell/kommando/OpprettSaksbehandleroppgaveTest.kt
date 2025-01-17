package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Testdata.godkjenningsbehovData
import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.PåVentRepository
import no.nav.helse.db.RisikovurderingRepository
import no.nav.helse.db.VergemålRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.HASTER
import no.nav.helse.modell.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.modell.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.modell.oppgave.Egenskap.REVURDERING
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SKJØNNSFASTSETTELSE
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.Egenskap.TILBAKEDATERT
import no.nav.helse.modell.oppgave.Egenskap.TILKOMMEN
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.modell.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.modell.oppgave.Egenskap.UTLAND
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.OVERGANG_FRA_IT
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettSaksbehandleroppgaveTest {
    private val FNR = lagFødselsnummer()
    private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    private val BEHANDLING_ID = UUID.randomUUID()
    private val UTBETALING_ID = UUID.randomUUID()
    private val HENDELSE_ID = UUID.randomUUID()
    private val contextId = UUID.randomUUID()
    private val context = CommandContext(contextId)

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val personRepository = mockk<PersonRepository>(relaxed = true)
    private val risikovurderingRepository = mockk<RisikovurderingRepository>(relaxed = true)
    private val egenAnsattRepository = mockk<EgenAnsattRepository>(relaxed = true)
    private val vergemålRepository = mockk<VergemålRepository>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)
    private val påVentRepository = mockk<PåVentRepository>(relaxed = true)

    private val command get() = opprettSaksbehandlerOppgaveCommand()
    private val utbetaling = mockk<Utbetaling>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        every { utbetaling.ingenUtbetaling() } returns true
        clearMocks(oppgaveService)
    }

    @Test
    fun `oppretter oppgave`() {
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter stikkprøve`() {
        every { automatisering.erStikkprøve(VEDTAKSPERIODE_ID, any()) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, STIKKPRØVE, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter risk QA`() {
        every { risikovurderingRepository.måTilManuell(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, RISK_QA, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter revurdering`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(utbetalingtype = Utbetalingtype.REVURDERING).execute(context))
        assertForventedeEgenskaper(REVURDERING, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for fortrolig adresse`() {
        every { personRepository.finnAdressebeskyttelse(FNR) } returns Adressebeskyttelse.Fortrolig
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, FORTROLIG_ADRESSE, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for strengt fortrolig adresse`() {
        every { personRepository.finnAdressebeskyttelse(FNR) } returns Adressebeskyttelse.StrengtFortrolig
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, STRENGT_FORTROLIG_ADRESSE, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for strengt fortrolig adresse utland`() {
        every { personRepository.finnAdressebeskyttelse(FNR) } returns Adressebeskyttelse.StrengtFortroligUtland
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, STRENGT_FORTROLIG_ADRESSE, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for utbetaling til sykmeldt`() {
        every { utbetaling.kunUtbetalingTilSykmeldt() } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, UTBETALING_TIL_SYKMELDT, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for delvis refusjon`() {
        every { utbetaling.delvisRefusjon() } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, DELVIS_REFUSJON, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egenskap utbetaling til arbeidsgiver`() {
        every { utbetaling.kunUtbetalingTilArbeidsgiver() } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, UTBETALING_TIL_ARBEIDSGIVER, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egenskap ingen utbetaling`() {
        every { utbetaling.ingenUtbetaling() } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter ikke oppgave med egenskap haster dersom det er utbetaling til arbeidsgiver`() {
        every { utbetaling.kunUtbetalingTilArbeidsgiver() } returns true
        every { sykefraværstilfelle.haster(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, UTBETALING_TIL_ARBEIDSGIVER, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egenskap haster dersom det er endring i utbetaling til sykmeldte`() {
        every { utbetaling.harEndringIUtbetalingTilSykmeldt() } returns true
        every { sykefraværstilfelle.haster(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, HASTER)
    }

    @Test
    fun `oppretter oppgave med egenskap skjønnsfastsettelse dersom det finnes varsel om avvik`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, SKJØNNSFASTSETTELSE)
    }

    @Test
    fun `oppretter oppgave med egenskap tilbakedatert dersom det finnes varsel om tilbakedatering`() {
        every { sykefraværstilfelle.erTilbakedatert(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, TILBAKEDATERT)
    }

    @Test
    fun `oppretter oppgave med egen ansatt`() {
        every { egenAnsattRepository.erEgenAnsatt(FNR) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, EGEN_ANSATT)
    }

    @Test
    fun `oppretter oppgave med egenskap UTLAND`() {
        every { personRepository.finnEnhetId(FNR) } returns "0393"
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, UTLAND)
    }

    @Test
    fun `oppretter oppgave med egenskap FLERE_ARBEIDSGIVERE`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE).execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, FLERE_ARBEIDSGIVERE, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `oppretter oppgave med egenskap FORLENGELSE`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = FORLENGELSE).execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, Egenskap.FORLENGELSE)
    }

    @Test
    fun `oppretter oppgave med egenskap INFOTRYGDFORLENGELSE`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = INFOTRYGDFORLENGELSE).execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, Egenskap.INFOTRYGDFORLENGELSE)
    }

    @Test
    fun `oppretter oppgave med egenskap OVERGANG_FRA_IT`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = OVERGANG_FRA_IT).execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, Egenskap.OVERGANG_FRA_IT)
    }

    @Test
    fun `oppretter oppgave med egenskap PÅ_VENT`() {
        every { påVentRepository.erPåVent(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, PÅ_VENT)
    }

    @Test
    fun `legger ikke til egenskap RISK_QA hvis oppgaven har egenskap REVURDERING`() {
        every { risikovurderingRepository.måTilManuell(VEDTAKSPERIODE_ID) } returns true
        assertTrue(opprettSaksbehandlerOppgaveCommand(utbetalingtype = Utbetalingtype.REVURDERING).execute(context))

        assertForventedeEgenskaper(REVURDERING, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING)
    }

    @Test
    fun `legger til oppgave med kanAvvises lik false`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(kanAvvises = false).execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, kanAvvises = false)
    }

    @Test
    fun `oppretter oppgave med tilkommen inntekt`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(tags = listOf("TilkommenInntekt")).execute(context))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, TILKOMMEN)
    }

    private fun assertForventedeEgenskaper(vararg egenskaper: Egenskap, kanAvvises: Boolean = true) {
        verify(exactly = 1) { oppgaveService.nyOppgave(
            FNR,
            VEDTAKSPERIODE_ID,
            BEHANDLING_ID,
            UTBETALING_ID,
            HENDELSE_ID,
            kanAvvises,
            egenskaper.toSet()
        ) }
    }

    private fun opprettSaksbehandlerOppgaveCommand(
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        kanAvvises: Boolean = true,
        tags: List<String> = emptyList()
    ) = OpprettSaksbehandleroppgave(
        behovData = godkjenningsbehovData(
            id = HENDELSE_ID,
            fødselsnummer = FNR,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            spleisBehandlingId = BEHANDLING_ID,
            utbetalingId = UTBETALING_ID,
            inntektskilde = inntektskilde,
            periodetype = periodetype,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            tags = tags
        ),
        oppgaveService = oppgaveService,
        automatisering = automatisering,
        personRepository = personRepository,
        risikovurderingRepository = risikovurderingRepository,
        egenAnsattRepository = egenAnsattRepository,
        utbetalingtype = utbetalingtype,
        sykefraværstilfelle = sykefraværstilfelle,
        utbetaling = utbetaling,
        vergemålRepository = vergemålRepository,
        påVentRepository = påVentRepository
    )
}
