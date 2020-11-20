package no.nav.helse.modell.automatisering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.risiko.RisikovurderingDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDateTime
import java.util.*

internal class AutomatiseringTest {

    private val vedtakDaoMock = mockk<VedtakDao>()
    private val warningDaoMock = mockk<WarningDao>()
    private val risikovurderingDaoMock = mockk<RisikovurderingDao> {
        every { hentRisikovurderingDto(vedtaksperiodeId) } returns risikovurderingDto()
        every { hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(risikovurderingDto())
    }
    private val digitalKontaktinformasjonDaoMock = mockk<DigitalKontaktinformasjonDao>(relaxed = true)
    private val åpneGosysOppgaverDaoMock = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val egenAnsattDao = mockk<EgenAnsattDao>(relaxed = true)
    private val miljøstyrtFeatureToggleMock = mockk<MiljøstyrtFeatureToggle>(relaxed = true)
    private val personDaoMock = mockk<PersonDao>(relaxed = true)
    private val automatiseringDaoMock = mockk<AutomatiseringDao>(relaxed = true)
    private val stikkprøveMock = mockk<StikkprøveVelger>()

    private val automatisering =
        Automatisering(
            vedtakDao = vedtakDaoMock,
            warningDao = warningDaoMock,
            risikovurderingDao = risikovurderingDaoMock,
            automatiseringDao = automatiseringDaoMock,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDaoMock,
            åpneGosysOppgaverDao = åpneGosysOppgaverDaoMock,
            egenAnsattDao = egenAnsattDao,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggleMock,
            personDao = personDaoMock,
            stikkprøveMock
        )

    companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    @BeforeEach
    fun setupDefaultTilHappyCase() {
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) } returns risikovurderingDto()
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(risikovurderingDto())
        every { warningDaoMock.finnWarnings(vedtaksperiodeId) } returns emptyList()
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns Saksbehandleroppgavetype.FORLENGELSE
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns true
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 0
        every { egenAnsattDao.erEgenAnsatt(any()) } returns false
        every { miljøstyrtFeatureToggleMock.automatisering() } returns true
        every { miljøstyrtFeatureToggleMock.risikovurdering() } returns true
        every { stikkprøveMock() } returns false
    }

    @Test
    fun `vedtaksperiode som oppfyller krav blir automatisk godkjent og lagret`() {
        val onSuccessCallback = mockk<() -> Unit>(relaxed = true)
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID(), onSuccessCallback)

        verify { automatiseringDaoMock.lagre(true, any(), any(), any()) }
        verify { onSuccessCallback() }
    }

    @Test
    fun `lagrer automatiseringen som ikke automatisk godkjent hvis ikke automatiserbar`() {
        every { miljøstyrtFeatureToggleMock.automatisering() } returns false
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) {
            fail("Denne skal ikke kalles når perioden blir automatisk behandlet")
        }
        verify { automatiseringDaoMock.lagre(false, any(), any(), any())}
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        every { warningDaoMock.finnWarnings(vedtaksperiodeId) } returns listOf(Warning("8.4 - Uenig i diagnose", WarningKilde.Spesialist))
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med type førstegangsbehandling er ikke automatiserbar`() {
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(
            risikovurderingDto(listOf("8-4 ikke fin"))
        )
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode hvor bruker ikke er digital er ikke automatiserbar`() {
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns false
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med ukjent dkif-status er ikke automatiserbar`() {
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns null
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med egen ansatt er ikke automatiserbar`() {
        every { egenAnsattDao.erEgenAnsatt(any()) } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med risikofeaturetoggle av er ikke automatiserbar`() {
        every { miljøstyrtFeatureToggleMock.risikovurdering() } returns false
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode med automatiseringsfeaturetoggle av er ikke automatiserbar`() {
        every { miljøstyrtFeatureToggleMock.automatisering() } returns false
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `vedtaksperiode plukket ut til stikkprøve skal ikke automatisk godkjennes`() {
        every { stikkprøveMock() } returns true
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { fail("Denne skal ikke kalles") }
    }

    @Test
    fun `Tar ikke stikkprøve av ikke-automatiserbar periode`() {
        every { miljøstyrtFeatureToggleMock.automatisering() } returns false
        automatisering.utfør(fødselsnummer, vedtaksperiodeId, UUID.randomUUID()) { }
        verify(exactly = 0) {
            stikkprøveMock()
        }
    }

    private fun risikovurderingDto(arbeidsuførhetsvurdering: List<String> = emptyList()) = RisikovurderingDto(
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        samletScore = 10.0,
        faresignaler = emptyList(),
        arbeidsuførhetvurdering = arbeidsuførhetsvurdering,
        ufullstendig = false
    )
}
