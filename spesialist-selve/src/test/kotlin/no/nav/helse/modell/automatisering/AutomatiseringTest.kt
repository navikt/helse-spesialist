package no.nav.helse.modell.automatisering

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.Toggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class AutomatiseringTest {
    private val fødselsnummer = lagFødselsnummer()
    private val orgnummer = lagOrganisasjonsnummer()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val periodetype = Periodetype.FORLENGELSE
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
    private val personDaoMock = mockk<PersonDao>(relaxed = true)
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
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlingMediatorMock,
            automatiseringDao = automatiseringDaoMock,
            åpneGosysOppgaverDao = åpneGosysOppgaverDaoMock,
            vergemålDao = vergemålDaoMock,
            personDao = personDaoMock,
            vedtakDao = vedtakDaoMock,
            overstyringDao = overstyringDaoMock,
            stikkprøver = stikkprøver,
            meldingDao = meldingDaoMock,
            generasjonDao = generasjonDaoMock,
        )

    @BeforeEach
    fun setupDefaultTilHappyCase() {
        every { vedtakDaoMock.erSpesialsak(vedtaksperiodeId) } returns false
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns periodetype
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 0
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
        every { vedtakDaoMock.finnOrgnummer(vedtaksperiodeId) } returns orgnummer
        every { meldingDaoMock.finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId) } returns 1
        every { meldingDaoMock.erAutomatisertKorrigertSøknadHåndtert(hendelseId) } returns false
        every {
            generasjonDaoMock.førsteGenerasjonVedtakFattetTidspunkt(
                vedtaksperiodeId,
            )
        } returns LocalDateTime.now().minusMonths(6).plusDays(1)
        stikkprøveFullRefusjonEnArbeidsgiver = false
        stikkprøveUtsEnArbeidsgiverForlengelse = false
    }

    @Test
    @ResourceLock("Toggle_AutomatiserSpesialsak")
    fun `går automatisk hvis det er spesialsak og ikke noen svartelistede varsler og ingen utbetaling og toggle er på`() {
        Toggle.AutomatiserSpesialsak.enable()
        every { vedtakDaoMock.erSpesialsak(vedtaksperiodeId) } returns true
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(false)
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns periodetype
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        every { vergemålDaoMock.harVergemål(fødselsnummer) } returns true
        val gjeldendeGenerasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        gjeldendeGenerasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId),
            hendelseId,
        )
        support.run {
            forsøkAutomatisering(
                generasjoner = listOf(gjeldendeGenerasjon),
                utbetaling = enUtbetaling(arbeidsgiverbeløp = 0, personbeløp = 0),
            )
            assertBleAutomatiskGodkjent()
        }
        Toggle.AutomatiserSpesialsak.disable()
    }

    @Test
    @ResourceLock("Toggle_AutomatiserSpesialsak")
    fun `går ikke automatisk hvis det er spesialsak og ikke noen svartelistede varsler og ingen utbetaling og toggle er av`() {
        Toggle.AutomatiserSpesialsak.disable()
        every { vedtakDaoMock.erSpesialsak(vedtaksperiodeId) } returns true
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(false)
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns periodetype
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        every { vergemålDaoMock.harVergemål(fødselsnummer) } returns true
        val gjeldendeGenerasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        gjeldendeGenerasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId),
            hendelseId,
        )
        support.run {
            forsøkAutomatisering(
                generasjoner = listOf(gjeldendeGenerasjon),
                utbetaling = enUtbetaling(arbeidsgiverbeløp = 0, personbeløp = 0),
            )
            assertGikkTilManuell()
        }
        Toggle.AutomatiserSpesialsak.enable()
    }

    @Test
    fun `vedtaksperiode som oppfyller krav blir automatisk godkjent og lagret`() {
        gårAutomatisk()
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        val gjeldendeGenerasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        gjeldendeGenerasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId),
            hendelseId,
        )
        support.run {
            forsøkAutomatisering(generasjoner = listOf(gjeldendeGenerasjon))
            assertGikkTilManuell()
        }
    }

    @Test
    fun `vedtaksperiode med 2 tidligere korrigerte søknader er ikke automatiserbar`() {
        every { meldingDaoMock.finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId) } returns 3
        gårTilManuellMedError(problems = listOf("Antall automatisk godkjente korrigerte søknader er større eller lik 2"))
    }

    @Test
    fun `vedtaksperiode som mottok første søknad for mer enn 6 måneder er ikke automatiserbar`() {
        every { generasjonDaoMock.førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId) } returns LocalDateTime.now().minusMonths(6)
        gårTilManuellMedError(problems = listOf("Mer enn 6 måneder siden vedtak på første mottatt søknad"))
    }

    @Test
    fun `Automatisering av korrigert søknad er allerede håndtert for tidligere sykefraværstilfelle`() {
        every { meldingDaoMock.erAutomatisertKorrigertSøknadHåndtert(hendelseId) } returns true
        gårAutomatisk()
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(false)
        gårTilManuell()
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns null
        gårTilManuell()
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        gårTilManuell()
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns null
        gårTilManuell()
    }

    @Test
    fun `vedtaksperiode med egen ansatt skal automatiseres`() {
        every { egenAnsattDao.erEgenAnsatt(any()) } returns true
        gårAutomatisk()
    }

    @Test
    fun `vedtaksperiode plukket ut til stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveFullRefusjonEnArbeidsgiver = true
        gårTilManuell()
    }

    @Test
    fun `person med flere arbeidsgivere skal automatisk godkjennes`() {
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.FLERE_ARBEIDSGIVERE
        gårAutomatisk()
    }

    @Test
    fun `periode med positiv revurdering skal automatisk godkjennes`() {
        gårAutomatisk(enUtbetaling(personbeløp = 1, type = Utbetalingtype.REVURDERING))
    }

    @Test
    fun `periode med negativ revurdering skal ikke automatisk godkjennes`() {
        gårTilManuell(enUtbetaling(personbeløp = -1, type = Utbetalingtype.REVURDERING))
    }

    @Test
    fun `revurdering uten endringer i beløp kan automatisk godkjennes`() {
        gårAutomatisk(
            enUtbetaling(
                arbeidsgiverbeløp = 0,
                personbeløp = 0,
                type = Utbetalingtype.REVURDERING,
            ),
        )
    }

    @Test
    fun `periode med vergemål skal ikke automatisk godkjennes`() {
        every { vergemålDaoMock.harVergemål(fødselsnummer) } returns true
        gårTilManuell()
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt skal automatisk godkjennes`() {
        gårAutomatisk(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `forlengelse med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUtsEnArbeidsgiverForlengelse = true
        gårTilManuell(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt skal automatisk godkjennes`() {
        gårAutomatisk(enUtbetaling(personbeløp = 500))
    }

    @Test
    fun `førstegangsbehandling med utbetaling til sykmeldt som plukkes ut som stikkprøve skal ikke automatisk godkjennes`() {
        stikkprøveUtsEnArbeidsgiverFørstegangsbehandling = true
        support.run {
            forsøkAutomatisering(
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
                utbetaling = enUtbetaling(personbeløp = 500),
            )
            assertGikkTilManuell()
        }
    }

    @Test
    fun `periode med delvis refusjon skal automatisk godkjennes`() {
        gårAutomatisk(enUtbetaling(personbeløp = 500, arbeidsgiverbeløp = 500))
    }

    @Test
    fun `periode med pågående overstyring skal ikke automatisk godkjennes`() {
        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns true
        gårTilManuell()
    }

    @Test
    fun `nullrevurdering grunnet saksbehandleroverstyring skal ikke automatisk godkjennes`() {
        support.forsøkAutomatisering(
            utbetaling =
                enUtbetaling(
                    arbeidsgiverbeløp = 0,
                    personbeløp = 0,
                    type = Utbetalingtype.REVURDERING,
                ),
        )
        support.assertBleAutomatiskGodkjent()

        clearMocks(support.onAutomatiserbar, automatiseringDaoMock)

        every { overstyringDaoMock.harVedtaksperiodePågåendeOverstyring(any()) } returns true
        support.forsøkAutomatisering()
        support.assertGikkTilManuell()
    }

    private val support =
        object {
            val onAutomatiserbar = mockk<() -> Unit>(relaxed = true)

            fun forsøkAutomatisering(
                periodetype: Periodetype = Periodetype.FORLENGELSE,
                generasjoner: List<Generasjon> = listOf(Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)),
                utbetaling: Utbetaling = enUtbetaling(),
            ) = automatisering.utfør(
                fødselsnummer,
                vedtaksperiodeId,
                hendelseId,
                utbetaling,
                periodetype,
                sykefraværstilfelle = Sykefraværstilfelle(fødselsnummer, 1.januar, generasjoner, emptyList()),
                orgnummer,
                onAutomatiserbar,
            )

            fun assertBleAutomatiskGodkjent() {
                verify(exactly = 1) { onAutomatiserbar() }
                verify(exactly = 1) { automatiseringDaoMock.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
                verify(exactly = 0) { automatiseringDaoMock.manuellSaksbehandling(any(), any(), any(), any()) }
            }

            fun assertGikkTilManuell(problems: List<String>? = null) {
                verify(exactly = 0) { onAutomatiserbar() }
                verify(exactly = 0) { automatiseringDaoMock.automatisert(any(), any(), any()) }
                verify(exactly = 1) {
                    if (stikkprøveUtsEnArbeidsgiverForlengelse || stikkprøveFullRefusjonEnArbeidsgiver || stikkprøveUtsEnArbeidsgiverFørstegangsbehandling) {
                        automatiseringDaoMock.stikkprøve(any(), any(), any())
                    } else {
                        automatiseringDaoMock.manuellSaksbehandling(problems ?: any(), vedtaksperiodeId, hendelseId, utbetalingId)
                    }
                }
            }
        }

    private fun enUtbetaling(
        arbeidsgiverbeløp: Int = 500,
        personbeløp: Int = 0,
        type: Utbetalingtype = Utbetalingtype.UTBETALING,
    ) = Utbetaling(utbetalingId, arbeidsgiverbeløp, personbeløp, type)

    private fun gårTilManuell(utbetaling: Utbetaling = enUtbetaling()) =
        support.run {
            forsøkAutomatisering(utbetaling = utbetaling)
            assertGikkTilManuell()
        }

    private fun gårTilManuellMedError(
        utbetaling: Utbetaling = enUtbetaling(),
        problems: List<String>,
    ) = support.run {
        forsøkAutomatisering(utbetaling = utbetaling)
        assertGikkTilManuell(problems)
    }

    private fun gårAutomatisk(utbetaling: Utbetaling = enUtbetaling()) =
        support.run {
            forsøkAutomatisering(utbetaling = utbetaling)
            assertBleAutomatiskGodkjent()
        }
}
