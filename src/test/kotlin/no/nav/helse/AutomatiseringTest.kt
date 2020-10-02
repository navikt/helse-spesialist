package no.nav.helse

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.risiko.RisikovurderingDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class AutomatiseringTest {

    private val vedtakDaoMock = mockk<VedtakDao>()
    private val risikovurderingDaoMock = mockk<RisikovurderingDao> {
        every { hentRisikovurdering(vedtaksperiodeId) }.returns(risikovurderingDto())
    }
    private val digitalKontaktinformasjonDaoMock = mockk<DigitalKontaktinformasjonDao>(relaxed = true)
    private val automatisering = Automatisering(
        vedtakDaoMock, risikovurderingDaoMock, mockk(relaxed = true),
        digitalKontaktinformasjonDaoMock
    )

    companion object {
        private const val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    @Test
    fun `vedtaksperiode uten warnings, med type forlengelse og ok risikovurdering er automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(vedtaksperiodeId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) }.returns(true)
        assertTrue(automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode med warnings og med type forlengelse er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(vedtaksperiodeId) }.returns(listOf("8.4 - Uenig i diagnose"))
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) }.returns(true)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode uten warnings og med type forskjellig fra forlengelse er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(vedtaksperiodeId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) }.returns(Saksbehandleroppgavetype.OVERGANG_FRA_IT)
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) }.returns(true)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode uten warnings, med type forlengelse og ikke ok risikovurdering er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(vedtaksperiodeId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) }.returns(risikovurderingDto(listOf("8-4 ikke fin")))
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) }.returns(true)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode uten warnings, med type forlengelse, ok risikovurdering og ikke digital er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(vedtaksperiodeId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) }.returns(false)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode uten warnings, med type forlengelse, ok risikovurdering og ukjent dkif-status er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(vedtaksperiodeId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(vedtaksperiodeId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        every { digitalKontaktinformasjonDaoMock.erDigital(any()) }.returns(null)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(fødselsnummer, vedtaksperiodeId))
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
