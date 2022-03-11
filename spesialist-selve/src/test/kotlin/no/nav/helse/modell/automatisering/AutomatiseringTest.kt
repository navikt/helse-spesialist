package no.nav.helse.modell.automatisering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class AutomatiseringTest {

    private val vedtakDaoMock = mockk<VedtakDao>()
    private val warningDaoMock = mockk<WarningDao>()
    private val risikovurderingDaoMock = mockk<RisikovurderingDao> {
        every { hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
    }
    private val digitalKontaktinformasjonDaoMock = mockk<DigitalKontaktinformasjonDao>(relaxed = true)
    private val åpneGosysOppgaverDaoMock = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val personDaoMock = mockk<PersonDao>(relaxed = true)
    private val automatiseringDaoMock = mockk<AutomatiseringDao>(relaxed = true)
    private val plukkTilManuellMock = mockk<PlukkTilManuell>()
    private val vergemålDaoMock = mockk<VergemålDao>(relaxed = true)

    private val automatisering =
        Automatisering(
            warningDao = warningDaoMock,
            risikovurderingDao = risikovurderingDaoMock,
            automatiseringDao = automatiseringDaoMock,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDaoMock,
            åpneGosysOppgaverDao = åpneGosysOppgaverDaoMock,
            egenAnsattDao = egenAnsattDao,
            personDao = personDaoMock,
            vedtakDao = vedtakDaoMock,
            plukkTilManuell = plukkTilManuellMock,
            vergemålDao = vergemålDaoMock
        )

    companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
    }

    @BeforeEach
    fun setupDefaultTilHappyCase() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(true)
        every { warningDaoMock.finnWarnings(vedtaksperiodeId) } returns emptyList()
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns Periodetype.FORLENGELSE
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.EN_ARBEIDSGIVER
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns true
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 0
        every { egenAnsattDao.erEgenAnsatt(any()) } returns false
        every { plukkTilManuellMock() } returns false
    }

    @Test
    fun `vedtaksperiode som oppfyller krav blir automatisk godkjent og lagret`() {
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar, onSuccessCallback)

        verify { automatiseringDaoMock.automatisert(any(), any(), any()) }
        verify { onSuccessCallback() }
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        every { warningDaoMock.finnWarnings(vedtaksperiodeId) } returns listOf(
            Warning(
                "8.4 - Uenig i diagnose",
                WarningKilde.Spesialist
            )
        )
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
        verify { automatiseringDaoMock.manuellSaksbehandling(any(), any(), any(), any()) }
        verify(exactly = 0) {
            plukkTilManuellMock()
        }
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(false)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode hvor bruker ikke er digital er ikke automatiserbar`() {
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns false
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med ukjent dkif-status er ikke automatiserbar`() {
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med egen ansatt er ikke automatiserbar`() {
        every { egenAnsattDao.erEgenAnsatt(any()) } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode plukket ut til stikkprøve skal ikke automatisk godkjennes`() {
        every { plukkTilManuellMock() } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `person med flere arbeidsgivere skal ikke automatisk godkjennes`() {
        every { vedtakDaoMock.finnInntektskilde(vedtaksperiodeId) } returns Inntektskilde.FLERE_ARBEIDSGIVERE
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `periode til revurdering skal ikke automatisk godkjennes`() {
        val hendelseId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, hendelseId, utbetalingId, Utbetalingtype.REVURDERING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
        verify(exactly = 1) { automatiseringDaoMock.manuellSaksbehandling(any(), vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 0) { automatiseringDaoMock.automatisert(any(), any(), any()) }
    }

    @Test
    fun `periode med vergemål skal ikke automatisk godkjennes`() {
        every { vergemålDaoMock.harVergemål(fødselsnummer) } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), UUID.randomUUID(), Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `periode med utbetaling til sykmeldt skal ikke automatisk godkjennes`() {
        every { personDaoMock.findVedtaksperiodeUtbetalingElement(fødselsnummer, utbetalingId) } returns PersonDao.Utbetalingen(
            utbetalingId, listOf(PersonDao.Utbetalingen.Utbetalingstidslinjedag(1.januar, 500, null))
        )
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `periode med delvis refusjon skal ikke automatisk godkjennes`() {
        every { personDaoMock.findVedtaksperiodeUtbetalingElement(fødselsnummer, utbetalingId) } returns PersonDao.Utbetalingen(
            utbetalingId, listOf(PersonDao.Utbetalingen.Utbetalingstidslinjedag(1.januar, 500, 500))
        )
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), utbetalingId, Utbetalingtype.UTBETALING, 1.januar, 31.januar) { fail("Denne skal ikke kalles") }
    }
}
