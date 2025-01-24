package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.objectMapper
import java.time.LocalDateTime
import java.util.UUID

class PgRisikovurderingDao internal constructor(session: Session) : RisikovurderingDao, QueryRunner by MedSession(session) {
    override fun hentRisikovurdering(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT kan_godkjennes_automatisk FROM risikovurdering_2021
            WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { it.boolean("kan_godkjennes_automatisk") }
            ?.let(Risikovurdering.Companion::restore)

    override fun måTilManuell(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT kan_godkjennes_automatisk
            FROM risikovurdering_2021
            WHERE vedtaksperiode_id = :vedtaksperiodeId
            ORDER BY id DESC
            LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { !it.boolean("kan_godkjennes_automatisk") } ?: true

    override fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    ) {
        asSQL(
            """
            INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, data, opprettet)
            VALUES (:vedtaksperiodeId, :kanGodkjennesAutomatisk, CAST (:data AS JSON), :opprettet);
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "kanGodkjennesAutomatisk" to kanGodkjennesAutomatisk,
            "data" to objectMapper.writeValueAsString(data),
            "opprettet" to opprettet,
        ).update()
    }
}
