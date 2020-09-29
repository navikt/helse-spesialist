package no.nav.helse

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
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
    private val automatisering = Automatisering(vedtakDaoMock, risikovurderingDaoMock, mockk(relaxed = true))

    companion object {
        private val eventId = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    @Test
    fun `vedtaksperiode uten warnings, med type forlengelse og ok risikovurdering er automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(eventId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(eventId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        assertTrue(automatisering.godkjentForAutomatisertBehandling(eventId, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode med warnings og med type forlengelse er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(eventId) }.returns(listOf("8.4 - Uenig i diagnose"))
        every { vedtakDaoMock.finnVedtaksperiodetype(eventId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(eventId, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode uten warnings og med type forskjellig fra forlengelse er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(eventId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(eventId) }.returns(Saksbehandleroppgavetype.OVERGANG_FRA_IT)
        assertFalse(automatisering.godkjentForAutomatisertBehandling(eventId, vedtaksperiodeId))
    }

    @Test
    fun `vedtaksperiode uten warnings, med type forlengelse og ikke ok risikovurdering er ikke automatiserbar`() {
        every { vedtakDaoMock.finnWarnings(eventId) }.returns(emptyList())
        every { vedtakDaoMock.finnVedtaksperiodetype(eventId) }.returns(Saksbehandleroppgavetype.FORLENGELSE)
        every { risikovurderingDaoMock.hentRisikovurdering(vedtaksperiodeId) }.returns(risikovurderingDto(listOf("8-4 ikke fin")))
        assertFalse(automatisering.godkjentForAutomatisertBehandling(eventId, vedtaksperiodeId))
    }

    private fun risikovurderingDto(arbeidsuførhetsvurdering: List<String> = emptyList()) = RisikovurderingDto(
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        samletScore = 10.0,
        faresignaler = emptyList(),
        arbeidsuførhetvurdering = arbeidsuførhetsvurdering,
        ufullstendig = true
    )
}
