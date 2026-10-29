package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PgRisikovurderingDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `lagrer risikovurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val data = objectMapper.createObjectNode().set("funn", objectMapper.createArrayNode())
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
            kanGodkjennesAutomatisk = false,
            data = data,
        )

        risikovurdering(vedtaksperiodeId).first().assertEquals(
            forventetVedtaksperiodeId = vedtaksperiodeId,
            forventetKanGodkjennesAutomatisk = false,
            forventetData = data,
            forventetOpprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
        )
    }

    @Test
    fun `dobbel insert medfører to innslag`() {
        val data = objectMapper.createObjectNode().set("funn", objectMapper.createArrayNode())
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
            kanGodkjennesAutomatisk = false,
            data = data,
        )
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 23).atStartOfDay(),
            kanGodkjennesAutomatisk = false,
            data = data,
        )

        risikovurdering(vedtaksperiodeId).also {
            assertEquals(2, it.size)
            it.first().assertEquals(
                forventetVedtaksperiodeId = vedtaksperiodeId,
                forventetKanGodkjennesAutomatisk = false,
                forventetData = data,
                forventetOpprettet = LocalDate.of(2020, 9, 23).atStartOfDay(),
            )
            it[1].assertEquals(
                forventetVedtaksperiodeId = vedtaksperiodeId,
                forventetKanGodkjennesAutomatisk = false,
                forventetData = data,
                forventetOpprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
            )
        }
    }

    @Test
    fun `henter funn og kontrollertOk fra siste risikovurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val gammel = objectMapper.readTree("""{ "funn": [], "kontrollertOk": [] }""")
        val ny =
            objectMapper.readTree(
                """{ "funn": [{ "beskrivelse": "Et funn", "kategori": ["8-4"] }], "kontrollertOk": [] }""",
            )
        risikovurderingDao.lagre(vedtaksperiodeId, false, gammel, LocalDate.of(2020, 9, 22).atStartOfDay())
        risikovurderingDao.lagre(vedtaksperiodeId, false, ny, LocalDate.of(2020, 9, 23).atStartOfDay())

        val data = risikovurderingDao.hentRisikovurderingsdata(vedtaksperiodeId)

        assertEquals(ny["funn"].toList(), data?.funn)
        assertEquals(emptyList<JsonNode>(), data?.kontrollertOk)
    }

    @Test
    fun `henter ingen risikovurderingsdata når vedtaksperioden ikke har risikovurdering`() {
        assertEquals(null, risikovurderingDao.hentRisikovurderingsdata(UUID.randomUUID()))
    }

    private fun risikovurdering(vedtaksperiodeId: UUID) =
        dbQuery.list(
            "SELECT * FROM risikovurdering_2021 WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ) { row ->
            RisikovurderingAssertions(
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                kanGodkjennesAutomatisk = row.boolean("kan_godkjennes_automatisk"),
                data = objectMapper.readTree(row.string("data")),
                opprettet = row.localDateTime("opprettet"),
            )
        }

    private class RisikovurderingAssertions(
        private val vedtaksperiodeId: UUID,
        private val kanGodkjennesAutomatisk: Boolean,
        private val data: JsonNode,
        private val opprettet: LocalDateTime,
    ) {
        fun assertEquals(
            forventetVedtaksperiodeId: UUID,
            forventetKanGodkjennesAutomatisk: Boolean,
            forventetData: JsonNode,
            forventetOpprettet: LocalDateTime,
        ) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetKanGodkjennesAutomatisk, kanGodkjennesAutomatisk)
            assertEquals(forventetData, data)
            assertEquals(forventetOpprettet, opprettet)
        }
    }
}
