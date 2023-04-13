package no.nav.helse.modell.automatisering

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spesialist.api.graphql.enums.Utbetalingtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class AutomatiseringTest {

    private val vedtakDaoMock = mockk<VedtakDao>()
    private val warningDaoMock = mockk<WarningDao>()
    private val risikovurderingDaoMock = mockk<RisikovurderingDao> {
        every { hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
    }
    private val snapshotMediator = mockk<SnapshotMediator>(relaxed = true)
    private val åpneGosysOppgaverDaoMock = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val personDaoMock = mockk<PersonDao>(relaxed = true)
    private val automatiseringDaoMock = mockk<AutomatiseringDao>(relaxed = true)
    private val vergemålDaoMock = mockk<VergemålDao>(relaxed = true)
    private val overstyringDaoMock = mockk<OverstyringDao>(relaxed = true)
    private var stikkprøveFullRefusjon = false
    private var stikkprøveUTS = false
    private val stikkprøver = object : Stikkprøver {
        override fun fullRefusjon() = stikkprøveFullRefusjon
        override fun uts() = stikkprøveUTS
    }

    private val automatisering =
        Automatisering(
            warningDao = warningDaoMock,
            risikovurderingDao = risikovurderingDaoMock,
            automatiseringDao = automatiseringDaoMock,
            åpneGosysOppgaverDao = åpneGosysOppgaverDaoMock,
            egenAnsattDao = egenAnsattDao,
            vergemålDao = vergemålDaoMock,
            personDao = personDaoMock,
            vedtakDao = vedtakDaoMock,
            overstyringDao = overstyringDaoMock,
            snapshotMediator = snapshotMediator,
            stikkprøver = stikkprøver
        )

    companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private val periodetype = Periodetype.FORLENGELSE
    }

    @BeforeEach
    fun setupDefaultTilHappyCase() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
        every { warningDaoMock.finnAktiveWarnings(vedtaksperiodeId) } returns emptyList()
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns periodetype
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 0
        every { egenAnsattDao.erEgenAnsatt(any()) } returns false
        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns false
        stikkprøveFullRefusjon = false
        stikkprøveUTS = false
    }

    @Test
    fun `vedtaksperiode som oppfyller krav blir automatisk godkjent og lagret`() {
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        automatisering.utfør(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = UUID.randomUUID(),
            utbetalingId = UUID.randomUUID(),
            periodetype = periodetype,
            sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()),
            periodeTom = 1.januar,
            onAutomatiserbar = onSuccessCallback
        )

        verify { automatiseringDaoMock.automatisert(any(), any(), any()) }
        verify { onSuccessCallback() }
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        val gjeldendeGenerasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        gjeldendeGenerasjon.håndter(Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId))
        val vedtaksperiode = Vedtaksperiode(vedtaksperiodeId, gjeldendeGenerasjon)
        val sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, listOf(vedtaksperiode))
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = sykefraværstilfelle, periodeTom = 31.januar) { fail("Denne skal ikke kalles") }
        verify { automatiseringDaoMock.manuellSaksbehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(false)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med egen ansatt er ikke automatiserbar`() {
        every { egenAnsattDao.erEgenAnsatt(any()) } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode plukket ut til stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveFullRefusjon = true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `person med flere arbeidsgivere skal ikke automatisk godkjennes`() {
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.FLERE_ARBEIDSGIVERE
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `periode til revurdering skal ikke automatisk godkjennes`() {
        val hendelseId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(type = Utbetalingtype.REVURDERING, personbeløp = 1)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
        verify(exactly = 1) { automatiseringDaoMock.manuellSaksbehandling(any(), vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 0) { automatiseringDaoMock.automatisert(any(), any(), any()) }
    }

    @Test
    fun `revurdering uten endringer i beløp kan automatisk godkjennes`() {
        val hendelseId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(type = Utbetalingtype.REVURDERING)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar, onSuccessCallback)
        verify(exactly = 1) { onSuccessCallback() }
        verify(exactly = 1) { automatiseringDaoMock.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 0) { automatiseringDaoMock.manuellSaksbehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `periode til revurdering skal automatisk godkjennes om toggle er på`() {
        Toggle.AutomatiserRevuderinger.enable()
        val hendelseId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(type = Utbetalingtype.REVURDERING)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar, onSuccessCallback)
        verify(exactly = 0) { automatiseringDaoMock.manuellSaksbehandling(any(), vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 1) { automatiseringDaoMock.automatisert(any(), any(), any()) }
        verify(exactly = 1) { onSuccessCallback() }
        Toggle.AutomatiserRevuderinger.disable()
    }

    @Test
    fun `periode med vergemål skal ikke automatisk godkjennes`() {
        every { vergemålDaoMock.harVergemål(fødselsnummer) } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `periode med utbetaling til sykmeldt skal ikke automatisk godkjennes`() {
        Toggle.AutomatiserUtbetalingTilSykmeldt.disable()
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(personbeløp = 500)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
        Toggle.AutomatiserUtbetalingTilSykmeldt.enable()
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt skal automatisk godkjennes`() {
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(personbeløp = 500)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar, onSuccessCallback)
        verify { automatiseringDaoMock.automatisert(any(), any(), any()) }
        verify { onSuccessCallback() }
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUTS = true
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(personbeløp = 500)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt skal ikke automatisk godkjennes`() {
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(personbeløp = 500)
        automatisering.utfør(
            fødselsnummer,
            vedtaksperiodeId,
            UUID.randomUUID(),
            utbetalingId,
            Periodetype.FØRSTEGANGSBEHANDLING,
            sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()),
            periodeTom = 1.januar
        ) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `periode med delvis refusjon skal automatisk godkjennes`() {
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns enUtbetaling(
            personbeløp = 500,
            arbeidsgiverbeløp = 500
        )
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar, onSuccessCallback)
        verify { automatiseringDaoMock.automatisert(any(), any(), any()) }
        verify { onSuccessCallback() }
    }

    @Test
    fun `periode med pågående overstyring skal ikke automatisk godkjennes`() {
        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, periodetype, sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()), periodeTom = 1.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `nullrevurdering grunnet saksbehandleroverstyring skal ikke automatisk godkjennes`() {
        val support = support()
        every { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) } returns
                enUtbetaling(type = Utbetalingtype.REVURDERING)
        support.forsøkAutomatisering()
        support.assertBleAutomatiskGodkjent()

        clearMocks(support.onAutomatiserbar)

        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns true
        support.forsøkAutomatisering()
        support.assertGikkTilManuell()
    }

    private fun enUtbetaling(personbeløp: Int = 0, arbeidsgiverbeløp: Int = 0, type: Utbetalingtype = Utbetalingtype.UTBETALING): GraphQLUtbetaling =
        GraphQLUtbetaling(
            id = utbetalingId.toString(),
            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
            arbeidsgiverNettoBelop = arbeidsgiverbeløp,
            personFagsystemId = "EN_FAGSYSTEMID",
            personNettoBelop = personbeløp,
            statusEnum = GraphQLUtbetalingstatus.GODKJENT,
            typeEnum = type,
            vurdering = null,
            personoppdrag = null,
            arbeidsgiveroppdrag = null,
        )

    private fun support() = object {
        val onAutomatiserbar = mockk<() -> Unit>(relaxed = true)
        fun forsøkAutomatisering() = automatisering.utfør(
            fødselsnummer,
            vedtaksperiodeId,
            UUID.randomUUID(),
            utbetalingId,
            periodetype,
            sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, emptyList()),
            periodeTom = 1.januar,
            onAutomatiserbar
        )
        fun assertBleAutomatiskGodkjent() = verify(exactly = 1) { onAutomatiserbar() }
        fun assertGikkTilManuell() = verify { onAutomatiserbar wasNot called }
    }
}

