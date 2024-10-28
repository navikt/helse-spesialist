package no.nav.helse.modell.risiko

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.RisikovurderingRepository
import no.nav.helse.objectMapper
import java.time.LocalDateTime
import java.util.UUID

internal class RisikovurderingDao(session: Session) : RisikovurderingRepository, QueryRunner by MedSession(session) {
    override fun hentRisikovurdering(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT kan_godkjennes_automatisk FROM risikovurdering_2021
            WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { it.boolean("kan_godkjennes_automatisk") }
            ?.let(Risikovurdering::restore)

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
        ).singleOrNull { it.boolean("krever_supersaksbehandler") } ?: false

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
        ).update()
    }
}
