package no.nav.helse.modell.risiko

import DatabaseIntegrationTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class RisikovurderingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `les og skriv`() {
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
                samletScore = 10.0,
                faresignaler = listOf("Signal1", "Signal2"),
                arbeidsuførhetvurdering = listOf("Vurdering1", "Vurdering2"),
                ufullstendig = true
            )
        )

        val vurdering = requireNotNull(risikovurderingDao.hentRisikovurderingDto(vedtaksperiodeId))
        assertEquals(vurdering.vedtaksperiodeId, vedtaksperiodeId)
        assertEquals(vurdering.opprettet, LocalDate.of(2020, 9, 22).atStartOfDay())
        assertEquals(vurdering.samletScore, 10.0)
        assertEquals(vurdering.faresignaler, listOf("Signal1", "Signal2"))
        assertEquals(vurdering.arbeidsuførhetvurdering, listOf("Vurdering1", "Vurdering2"))
        assertEquals(vurdering.ufullstendig, true)
    }

    @Test
    fun `filterer ut duplikate warnings`() {
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
                samletScore = 10.0,
                faresignaler = listOf("Signal1", "Signal1", "Signal2"),
                arbeidsuførhetvurdering = listOf("Vurdering1", "Vurdering1", "Vurdering2"),
                ufullstendig = true
            )
        )

        val vurdering = requireNotNull(risikovurderingDao.hentRisikovurderingDto(vedtaksperiodeId))
        assertEquals(vurdering.vedtaksperiodeId, vedtaksperiodeId)
        assertEquals(vurdering.opprettet, LocalDate.of(2020, 9, 22).atStartOfDay())
        assertEquals(vurdering.samletScore, 10.0)
        assertEquals(vurdering.faresignaler, listOf("Signal1", "Signal2"))
        assertEquals(vurdering.arbeidsuførhetvurdering, listOf("Vurdering1", "Vurdering2"))
        assertEquals(vurdering.ufullstendig, true)
    }

    @Test
    fun `dobbel insert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
                samletScore = 10.0,
                faresignaler = listOf("Signal1", "Signal1", "Signal2"),
                arbeidsuførhetvurdering = listOf("Vurdering1", "Vurdering1", "Vurdering2"),
                ufullstendig = true
            )
        )
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 23).atStartOfDay(),
                samletScore = 10.0,
                faresignaler = listOf("Signal1", "Signal1", "Signal2"),
                arbeidsuførhetvurdering = listOf("Vurdering1", "Vurdering1", "Vurdering2"),
                ufullstendig = true
            )
        )

        val vurdering = requireNotNull(risikovurderingDao.hentRisikovurderingDto(vedtaksperiodeId))
        assertEquals(vurdering.vedtaksperiodeId, vedtaksperiodeId)
        assertEquals(vurdering.opprettet, LocalDate.of(2020, 9, 23).atStartOfDay())
        assertEquals(vurdering.samletScore, 10.0)
        assertEquals(vurdering.faresignaler, listOf("Signal1", "Signal2"))
        assertEquals(vurdering.arbeidsuførhetvurdering, listOf("Vurdering1", "Vurdering2"))
        assertEquals(vurdering.ufullstendig, true)
    }

    @Test
    fun `leser manglende risikovurdering`() {
        assertNull(risikovurderingDao.hentRisikovurderingDto(UUID.randomUUID()))
    }
}
