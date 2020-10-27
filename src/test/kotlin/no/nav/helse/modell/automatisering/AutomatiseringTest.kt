package no.nav.helse.modell.automatisering

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.risiko.RisikovurderingDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    private val automatisering =
        Automatisering(
            vedtakDao = vedtakDaoMock,
            warningDao = warningDaoMock,
            risikovurderingDao = risikovurderingDaoMock,
            automatiseringDao = mockk(relaxed = true),
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDaoMock,
            åpneGosysOppgaverDao = åpneGosysOppgaverDaoMock,
            egenAnsattDao = egenAnsattDao,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggleMock
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
    }

    @Test
    fun `vedtaksperiode som oppfyller krav er automatiserbar`() {
        assertTrue(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med warnings er ikke automatiserbar`() {
        every { warningDaoMock.finnWarnings(vedtaksperiodeId) } returns listOf(Warning("8.4 - Uenig i diagnose", WarningKilde.Spesialist))
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med type forskjellig fra forlengelse er ikke automatiserbar`() {
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) } returns Saksbehandleroppgavetype.OVERGANG_FRA_IT
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode uten ok risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns Risikovurdering.restore(risikovurderingDto(listOf("8-4 ikke fin")))
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med null risikovurdering er ikke automatiserbar`() {
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) } returns null
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode hvor bruker ikke er digital er ikke automatiserbar`() {
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns false
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med ukjent dkif-status er ikke automatiserbar`() {
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) } returns null
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns 1
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med _null_ åpne oppgaver er ikke automatiserbar`() {
        every { åpneGosysOppgaverDaoMock.harÅpneOppgaver(any()) } returns null
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med egen ansatt er ikke automatiserbar`() {
        every { egenAnsattDao.erEgenAnsatt(any()) } returns true
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med risikofeaturetoggle av er ikke automatiserbar`() {
        every { miljøstyrtFeatureToggleMock.risikovurdering() } returns false
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
    }

    @Test
    fun `vedtaksperiode med automatiseringsfeaturetoggle av er ikke automatiserbar`() {
        every { miljøstyrtFeatureToggleMock.automatisering() } returns false
        assertFalse(automatisering.vurder(fødselsnummer, vedtaksperiodeId).erAutomatiserbar())
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
