package no.nav.helse.modell.risiko

import DatabaseIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class RisikovurderingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `les og skriv`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val data = objectMapper.createObjectNode().set<JsonNode>("funn", objectMapper.createArrayNode())
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
                kanGodkjennesAutomatisk = false,
                kreverSupersaksbehandler = false,
                data = data,
            )
        )

        val vurdering = requireNotNull(risikovurderingDao.hentRisikovurderingDto(vedtaksperiodeId))
        assertEquals(vurdering.vedtaksperiodeId, vedtaksperiodeId)
        assertEquals(vurdering.opprettet, LocalDate.of(2020, 9, 22).atStartOfDay())
        assertEquals(vurdering.kanGodkjennesAutomatisk, false)
        assertEquals(vurdering.kreverSupersaksbehandler, false)
        assertEquals(vurdering.data, data)
    }

    @Test
    fun `dobbel insert`() {
        val data = objectMapper.createObjectNode().set<JsonNode>("funn", objectMapper.createArrayNode())
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
                kanGodkjennesAutomatisk = false,
                kreverSupersaksbehandler = false,
                data = data,
            )
        )
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = LocalDate.of(2020, 9, 23).atStartOfDay(),
                kanGodkjennesAutomatisk = false,
                kreverSupersaksbehandler = false,
                data = data,
            )
        )

        val vurdering = requireNotNull(risikovurderingDao.hentRisikovurderingDto(vedtaksperiodeId))
        assertEquals(vurdering.vedtaksperiodeId, vedtaksperiodeId)
        assertEquals(vurdering.opprettet, LocalDate.of(2020, 9, 23).atStartOfDay())
    }

    @Test
    fun `leser manglende risikovurdering`() {
        assertNull(risikovurderingDao.hentRisikovurderingDto(UUID.randomUUID()))
    }
}
