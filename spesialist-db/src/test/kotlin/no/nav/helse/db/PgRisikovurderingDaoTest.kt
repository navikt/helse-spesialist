package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PgRisikovurderingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagrer risikovurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val data = objectMapper.createObjectNode().set<JsonNode>("funn", objectMapper.createArrayNode())
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
            kanGodkjennesAutomatisk = false,
            data = data
        )

        risikovurdering(vedtaksperiodeId).first().assertEquals(
            forventetVedtaksperiodeId = vedtaksperiodeId,
            forventetKanGodkjennesAutomatisk = false,
            forventetData = data,
            forventetOpprettet = LocalDate.of(2020, 9, 22).atStartOfDay()
        )
    }

    @Test
    fun `dobbel insert medfører to innslag`() {
        val data = objectMapper.createObjectNode().set<JsonNode>("funn", objectMapper.createArrayNode())
        val vedtaksperiodeId = UUID.randomUUID()
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 22).atStartOfDay(),
            kanGodkjennesAutomatisk = false,
            data = data
        )
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = LocalDate.of(2020, 9, 23).atStartOfDay(),
            kanGodkjennesAutomatisk = false,
            data = data
        )

        risikovurdering(vedtaksperiodeId).also {
            assertEquals(2, it.size)
            it.first().assertEquals(
                forventetVedtaksperiodeId = vedtaksperiodeId,
                forventetKanGodkjennesAutomatisk = false,
                forventetData = data,
                forventetOpprettet = LocalDate.of(2020, 9, 23).atStartOfDay()
            )
            it[1].assertEquals(
                forventetVedtaksperiodeId = vedtaksperiodeId,
                forventetKanGodkjennesAutomatisk = false,
                forventetData = data,
                forventetOpprettet = LocalDate.of(2020, 9, 22).atStartOfDay()
            )
        }
    }

    private fun risikovurdering(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use {
            @Language("PostgreSQL") val statement =
                "SELECT * FROM risikovurdering_2021 WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC"
            it.run(queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map { row ->
                RisikovurderingAssertions(
                    vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                    kanGodkjennesAutomatisk = row.boolean("kan_godkjennes_automatisk"),
                    data = objectMapper.readTree(row.string("data")),
                    opprettet = row.localDateTime("opprettet")
                )
            }.asList)
        }

    private class RisikovurderingAssertions(
        private val vedtaksperiodeId: UUID,
        private val kanGodkjennesAutomatisk: Boolean,
        private val data: JsonNode,
        private val opprettet: LocalDateTime
    ) {
        fun assertEquals(
            forventetVedtaksperiodeId: UUID,
            forventetKanGodkjennesAutomatisk: Boolean,
            forventetData: JsonNode,
            forventetOpprettet: LocalDateTime
        ) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetKanGodkjennesAutomatisk, kanGodkjennesAutomatisk)
            assertEquals(forventetData, data)
            assertEquals(forventetOpprettet, opprettet)
        }
    }
}
