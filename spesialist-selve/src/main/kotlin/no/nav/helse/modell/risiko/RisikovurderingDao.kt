package no.nav.helse.modell.risiko

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class RisikovurderingDao(val dataSource: DataSource) {
    internal fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        kreverSupersaksbehandler: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    ) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, krever_supersaksbehandler, data, opprettet)
                VALUES (?, ?, ?, CAST (? AS JSON), ?);
            """
            session.run(
                queryOf(
                    statement,
                    vedtaksperiodeId,
                    kanGodkjennesAutomatisk,
                    kreverSupersaksbehandler,
                    objectMapper.writeValueAsString(data),
                    opprettet,
                ).asUpdate,
            )
        }
    }

    internal fun hentRisikovurdering(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT kan_godkjennes_automatisk FROM risikovurdering_2021 WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1"
            session.run(
                queryOf(statement, vedtaksperiodeId).map { it.boolean("kan_godkjennes_automatisk") }.asSingle,
            )?.let(Risikovurdering::restore)
        }

    internal fun kreverSupersaksbehandler(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT krever_supersaksbehandler FROM risikovurdering_2021 WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1"
            session.run(
                queryOf(statement, vedtaksperiodeId).map { it.boolean("krever_supersaksbehandler") }.asSingle,
            ) ?: false
        }
}
