package no.nav.helse.spesialist.application.modell

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.Automatiseringsresultat
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.spesialist.application.januar
import no.nav.helse.spesialist.application.lagFødselsnummer
import no.nav.helse.spesialist.application.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class AutomatiseringTest {
    private val fødselsnummer = lagFødselsnummer()
    private val orgnummer = lagOrganisasjonsnummer()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val periodeFom = LocalDate.now()

    private val vedtakDaoMock = mockk<VedtakDao>()
    private val risikovurderingDaoMock =
        mockk<RisikovurderingDao> {
            every { hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
        }
    private val stansAutomatiskBehandlingMediatorMock =
        mockk<StansAutomatiskBehandlingMediator> {
            every { sjekkOmAutomatiseringErStanset(fødselsnummer, vedtaksperiodeId, orgnummer) } returns false
        }
    private val åpneGosysOppgaverDaoMock = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val personDaoMock =
        mockk<PersonDao>(relaxed = true) {
            every { finnAdressebeskyttelse(any()) } returns Adressebeskyttelse.Ugradert
        }
    private val automatiseringDaoMock = mockk<AutomatiseringDao>(relaxed = true)
    private val vergemålDaoMock = mockk<VergemålDao>(relaxed = true)
    private val overstyringDaoMock = mockk<OverstyringDao>(relaxed = true)
    private val meldingDaoMock = mockk<MeldingDao>(relaxed = true)
    private val generasjonDaoMock = mockk<GenerasjonDao>(relaxed = true)
    private var stikkprøveFullRefusjonEnArbeidsgiver = false
    private var stikkprøveUtsEnArbeidsgiverFørstegangsbehandling = false
    private var stikkprøveUtsEnArbeidsgiverForlengelse = false
    private val stikkprøver =
        object : Stikkprøver {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = false

            override fun utsFlereArbeidsgivereForlengelse() = false

            override fun utsEnArbeidsgiverFørstegangsbehandling() = stikkprøveUtsEnArbeidsgiverFørstegangsbehandling

            override fun utsEnArbeidsgiverForlengelse() = stikkprøveUtsEnArbeidsgiverForlengelse

            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false

            override fun fullRefusjonFlereArbeidsgivereForlengelse() = false

            override fun fullRefusjonEnArbeidsgiver() = stikkprøveFullRefusjonEnArbeidsgiver
        }

    private val automatisering =
        Automatisering(
            risikovurderingDao = risikovurderingDaoMock,
            automatiseringStansetSjekker = stansAutomatiskBehandlingMediatorMock,
            automatiseringDao = automatiseringDaoMock,
            åpneGosysOppgaverDao = åpneGosysOppgaverDaoMock,
            vergemålDao = vergemålDaoMock,
            personDao = personDaoMock,
            vedtakDao = vedtakDaoMock,
            overstyringDao = overstyringDaoMock,
            stikkprøver = stikkprøver,
            meldingDao = meldingDaoMock,
            generasjonDao = generasjonDaoMock,
            egenAnsattDao = egenAnsattDao,
        )

    @BeforeEach
    fun setupDefaultTilHappyCase() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        every { åpneGosysOppgaverDaoMock.antallÅpneOppgaver(any()) } returns 0
        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns false
        every { meldingDaoMock.sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId) } returns
                MeldingDao.OverstyringIgangsattKorrigertSøknad(
                    meldingId = hendelseId.toString(),
                    periodeForEndringFom = periodeFom,
                    berørtePerioder =
                        listOf(
                            MeldingDao.BerørtPeriode(
                                vedtaksperiodeId = vedtaksperiodeId,
                                orgnummer = orgnummer,
                                periodeFom = periodeFom,
                            ),
                        ),
                )
        every { vedtakDaoMock.finnOrganisasjonsnummer(vedtaksperiodeId) } returns orgnummer
        every { meldingDaoMock.finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId) } returns 1
        every { meldingDaoMock.erKorrigertSøknadAutomatiskBehandlet(hendelseId) } returns false
        every {
            generasjonDaoMock.førsteGenerasjonVedtakFattetTidspunkt(
                vedtaksperiodeId,
            )
        } returns LocalDateTime.now().minusMonths(6).plusDays(1)
        stikkprøveFullRefusjonEnArbeidsgiver = false
        stikkprøveUtsEnArbeidsgiverForlengelse = false
    }

    @Test
    fun `vedtaksperiode som oppfyller krav blir automatisk godkjent og lagret`() {
        blirAutomatiskBehandlet(enUtbetaling())
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        val gjeldendeGenerasjon = enGenerasjon()
        gjeldendeGenerasjon.håndterNyttVarsel(etVarsel())
        blirManuellOppgave(behandling = gjeldendeGenerasjon)
    }

    @Test
    fun `vedtaksperiode med 2 tidligere korrigerte søknader er ikke automatiserbar`() {
        every { meldingDaoMock.finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId) } returns 3
        blirManuellOppgaveMedFeil(problems = listOf("Antall automatisk godkjente korrigerte søknader er større eller lik 2"))
    }

    @Test
    fun `vedtaksperiode som mottok første søknad for mer enn 6 måneder er ikke automatiserbar`() {
        every { generasjonDaoMock.førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId) } returns LocalDateTime.now().minusMonths(6)
        blirManuellOppgaveMedFeil(problems = listOf("Mer enn 6 måneder siden vedtak på første mottatt søknad"))
    }

    @Test
    fun `Automatisering av korrigert søknad er allerede håndtert for tidligere sykefraværstilfelle`() {
        every { meldingDaoMock.erKorrigertSøknadAutomatiskBehandlet(hendelseId) } returns true
        blirAutomatiskBehandlet()
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(false)
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns null
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.antallÅpneOppgaver(any()) } returns 1
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.antallÅpneOppgaver(any()) } returns null
        blirManuellOppgave()
    }

    @Test
    fun `vedtaksperiode plukket ut til stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        blirStikkprøve()
    }

    @Test
    fun `person med flere arbeidsgivere skal automatisk godkjennes`() {
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.FLERE_ARBEIDSGIVERE
        blirAutomatiskBehandlet()
    }

    @Test
    fun `periode med positiv revurdering skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 1, type = REVURDERING))
    }

    @Test
    fun `periode med negativ revurdering skal ikke automatisk godkjennes`() {
        blirManuellOppgave(enUtbetaling(personbeløp = -1, type = REVURDERING))
    }

    @Test
    fun `revurdering uten endringer i beløp kan automatisk godkjennes`() {
        blirAutomatiskBehandlet(
            enUtbetaling(
                arbeidsgiverbeløp = 0,
                personbeløp = 0,
                type = REVURDERING,
            ),
        )
    }

    @Test
    fun `periode med vergemål skal ikke automatisk godkjennes`() {
        every { vergemålDaoMock.harVergemål(fødselsnummer) } returns true
        blirManuellOppgave()
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUtsEnArbeidsgiverForlengelse = true
        blirStikkprøve(enUtbetaling(personbeløp = 500), periodetype = FORLENGELSE)
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUtsEnArbeidsgiverFørstegangsbehandling = true
        val utbetaling = enUtbetaling(personbeløp = 500)
        blirStikkprøve(utbetaling, periodetype = FØRSTEGANGSBEHANDLING)
    }

    @Test
    fun `egenansatt går ikke til stikkprøve`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        blirStikkprøve()
        every { egenAnsattDao.erEgenAnsatt(any()) } returns true
        blirAutomatiskBehandlet()
    }

    @Test
    fun `tar ikke stikkprøve når det er gradert adresse`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        blirStikkprøve()
        every { personDaoMock.finnAdressebeskyttelse(any()) } returns Adressebeskyttelse.Fortrolig
        blirAutomatiskBehandlet()
    }

    @Test
    fun `periode med delvis refusjon skal automatisk godkjennes`() {
        blirAutomatiskBehandlet(enUtbetaling(personbeløp = 500, arbeidsgiverbeløp = 500))
    }

    @Test
    fun `periode med pågående overstyring skal ikke automatisk godkjennes`() {
        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns true
        blirManuellOppgave()
    }

    @Test
    fun `nullrevurdering grunnet saksbehandleroverstyring skal ikke automatisk godkjennes`() {
        val utbetaling = enUtbetaling(arbeidsgiverbeløp = 0, personbeløp = 0, type = REVURDERING)
        blirAutomatiskBehandlet(utbetaling)
        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns true
        blirManuellOppgave()
    }

    private fun assertKanIkkeAutomatiseres(resultat: Automatiseringsresultat) {
        assertTrue(resultat is Automatiseringsresultat.KanIkkeAutomatiseres, "Expected ${Automatiseringsresultat.KanIkkeAutomatiseres::class.simpleName}, got ${resultat::class.simpleName}")
    }

    private fun assertStikkprøve(resultat: Automatiseringsresultat) {
        assertTrue(resultat is Automatiseringsresultat.Stikkprøve, "Expected ${Automatiseringsresultat.Stikkprøve::class.simpleName}, got ${resultat::class.simpleName}")
    }

    private fun assertKanAutomatiseres(resultat: Automatiseringsresultat) {
        assertTrue(resultat is Automatiseringsresultat.KanAutomatiseres, "Expected ${Automatiseringsresultat.KanAutomatiseres::class.simpleName}, got ${resultat::class.simpleName}")
    }

    private fun forsøkAutomatisering(
        periodetype: Periodetype = FORLENGELSE,
        generasjoners: List<Behandling> = listOf(Behandling(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)),
        utbetaling: Utbetaling = enUtbetaling(),
    ) = automatisering.utfør(
        fødselsnummer,
        vedtaksperiodeId,
        utbetaling,
        periodetype,
        sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, generasjoners),
        orgnummer,
    )

    private fun enUtbetaling(
        arbeidsgiverbeløp: Int = 500,
        personbeløp: Int = 0,
        type: Utbetalingtype = Utbetalingtype.UTBETALING,
    ) = Utbetaling(utbetalingId, arbeidsgiverbeløp, personbeløp, type)

    private fun enGenerasjon(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        vedtaksperiodeId: UUID = this.vedtaksperiodeId,
        generasjonId: UUID = UUID.randomUUID(),
    ) = Behandling(generasjonId, vedtaksperiodeId, fom, tom, skjæringstidspunkt)

    private fun etVarsel(
        varselId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = this.vedtaksperiodeId,
        varselkode: String = "RV_IM_1",
    ) = Varsel(varselId, varselkode, LocalDateTime.now(), vedtaksperiodeId)

    private fun blirManuellOppgave(utbetaling: Utbetaling = enUtbetaling(), behandling: Behandling = enGenerasjon()) =
        assertKanIkkeAutomatiseres(forsøkAutomatisering(utbetaling = utbetaling, generasjoners = listOf(behandling)))

    private fun blirStikkprøve(utbetaling: Utbetaling = enUtbetaling(), periodetype: Periodetype = FØRSTEGANGSBEHANDLING) =
        assertStikkprøve(forsøkAutomatisering(utbetaling = utbetaling, periodetype = periodetype))

    private fun blirManuellOppgaveMedFeil(
        utbetaling: Utbetaling = enUtbetaling(),
        problems: List<String>,
    ) {
        val resultat = forsøkAutomatisering(utbetaling = utbetaling)
        assertKanIkkeAutomatiseres(resultat)
        check(resultat is Automatiseringsresultat.KanIkkeAutomatiseres)
        assertEquals(problems.toSet(), resultat.problemer.toSet())
    }

    private fun blirAutomatiskBehandlet(utbetaling: Utbetaling = enUtbetaling()) =
        assertKanAutomatiseres(forsøkAutomatisering(utbetaling = utbetaling))

}
