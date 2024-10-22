package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.single
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class TransactionalRisikovurderingDao(private val session: Session) : RisikovurderingRepository {
    override fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering? {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT kan_godkjennes_automatisk FROM risikovurdering_2021 WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1
            """.trimIndent()
        return session.run(
            queryOf(statement, vedtaksperiodeId)
                .map { it.boolean("kan_godkjennes_automatisk") }
                .asSingle,
        )?.let(Risikovurdering::restore)
    }

    override fun kreverSupersaksbehandler(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT krever_supersaksbehandler
            FROM risikovurdering_2021
            WHERE vedtaksperiode_id = :vedtaksperiodeId
            ORDER BY id DESC
            LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single(session) { it.boolean("krever_supersaksbehandler") } ?: false

    override fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        kreverSupersaksbehandler: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    ) {
        asSQL(
            """
            INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, krever_supersaksbehandler, data, opprettet)
            VALUES (:vedtaksperiodeId, :kanGodkjennesAutomatisk, :kreverSupersaksbehandler, CAST (:data AS JSON), :opprettet);
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "kanGodkjennesAutomatisk" to kanGodkjennesAutomatisk,
            "kreverSupersaksbehandler" to kreverSupersaksbehandler,
            "data" to objectMapper.writeValueAsString(data),
            "opprettet" to opprettet,
        ).update(session)
    }
}
