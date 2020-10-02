package no.nav.helse.modell.risiko

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    private val risikovurderingDaoMock = mockk<RisikovurderingDao>()

    @Test
    fun `Fullstending vurdering kan behandles automatisk`() {
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) }.returns(risikovurderingDto())
        val risikovurdering = requireNotNull(risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) })
        assertTrue(risikovurdering.kanBehandlesAutomatisk())
    }

    @Test
    fun `Ufullstendig vurdering kan ikke behandles automatisk`() {
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) }.returns(risikovurderingDto(ufullstendig = true))
        val risikovurdering = requireNotNull(risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) })
        assertFalse(risikovurdering.kanBehandlesAutomatisk())
    }

    @Test
    fun `Fullstending vurdering med 8-4 feil kan ikke behandles automatisk`() {
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) }.returns(risikovurderingDto(arbeidsuførhetsvurdering = listOf("8-4 feil")))
        val risikovurdering = requireNotNull(risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) })
        assertFalse(risikovurdering.kanBehandlesAutomatisk())
    }

    private fun risikovurderingDto(arbeidsuførhetsvurdering: List<String> = emptyList(), ufullstendig: Boolean = false) = RisikovurderingDto(
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        samletScore = 10.0,
        faresignaler = emptyList(),
        arbeidsuførhetvurdering = arbeidsuførhetsvurdering,
        ufullstendig = ufullstendig
    )
}
