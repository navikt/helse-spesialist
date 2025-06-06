package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.db.CommandContextDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.dao.PgCommandContextDao.CommandContextTilstand.AVBRUTT
import no.nav.helse.spesialist.db.dao.PgCommandContextDao.CommandContextTilstand.FEIL
import no.nav.helse.spesialist.db.dao.PgCommandContextDao.CommandContextTilstand.FERDIG
import no.nav.helse.spesialist.db.dao.PgCommandContextDao.CommandContextTilstand.NY
import no.nav.helse.spesialist.db.dao.PgCommandContextDao.CommandContextTilstand.SUSPENDERT
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgCommandContextDao private constructor(
    queryRunner: QueryRunner,
) : CommandContextDao, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    override fun nyContext(meldingId: UUID): CommandContext {
        val contextId = UUID.randomUUID()
        return CommandContext(contextId).apply {
            opprett(meldingId, contextId)
        }
    }

    override fun opprett(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, NY, null)
    }

    override fun ferdig(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, FERDIG, null)
    }

    override fun feil(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, FEIL, null)
    }

    override fun suspendert(
        hendelseId: UUID,
        contextId: UUID,
        hash: UUID,
        sti: List<Int>,
    ) {
        lagre(hendelseId, contextId, SUSPENDERT, hash, sti)
    }

    override fun avbryt(
        vedtaksperiodeId: UUID,
        contextId: UUID,
    ): List<Pair<UUID, UUID>> {
        val kontekster = finnContexterSomSkalAvbrytes(contextId, vedtaksperiodeId)
        kontekster.forEach(::avbrytContext)
        return kontekster.map { it.contextId to it.hendelseId }
    }

    private fun lagre(
        hendelseId: UUID,
        contextId: UUID,
        tilstand: CommandContextTilstand,
        hash: UUID?,
        sti: List<Int> = emptyList(),
    ) {
        asSQL(
            """
            INSERT INTO command_context (context_id, hendelse_id, opprettet, tilstand, data, hash)
            VALUES (:contextId, :hendelseId, :opprettet, :tilstand, :data::json, :hash)
            """.trimIndent(),
            "contextId" to contextId,
            "hendelseId" to hendelseId,
            "opprettet" to LocalDateTime.now(),
            "tilstand" to tilstand.name,
            "data" to mapper.writeValueAsString(CommandContextDto(sti)),
            "hash" to hash,
        ).update()
    }

    private data class ContextRad(
        val contextId: UUID,
        val hendelseId: UUID,
        val data: String,
    )

    private fun avbrytContext(contextRad: ContextRad) {
        asSQL(
            """
            INSERT INTO command_context (context_id, hendelse_id, opprettet, tilstand, data)
            VALUES (:contextId, :hendelseId, :opprettet, :avbrutt, :data::json)
            """.trimIndent(),
            "avbrutt" to AVBRUTT.name,
            "contextId" to contextRad.contextId,
            "hendelseId" to contextRad.hendelseId,
            "opprettet" to LocalDateTime.now(),
            "data" to contextRad.data,
        ).update()
    }

    private fun finnContexterSomSkalAvbrytes(
        contextId: UUID,
        vedtaksperiodeId: UUID,
    ) = asSQL(
        """
        WITH siste_contexter AS (SELECT DISTINCT ON (context_id) context_id, hendelse_id, tilstand, data
        FROM command_context
        WHERE hendelse_id in (
            SELECT hendelse_ref FROM vedtaksperiode_hendelse WHERE vedtaksperiode_id = :vedtaksperiodeId
        )
        AND context_id != :contextId
        ORDER BY context_id, id DESC)
        SELECT * FROM siste_contexter WHERE tilstand IN (:ny, :suspendert)
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "contextId" to contextId,
        "ny" to NY.name,
        "suspendert" to SUSPENDERT.name,
    ).list { ContextRad(it.uuid("context_id"), it.uuid("hendelse_id"), it.string("data")) }

    override fun tidsbrukForContext(contextId: UUID) =
        asSQL(
            """
            select extract(milliseconds from (max(opprettet) - min(opprettet))) as tid_brukt_ms
            from command_context
            where context_id = :contextId
            """.trimIndent(),
            "contextId" to contextId,
        ).single { it.int("tid_brukt_ms") }

    override fun finnSuspendert(contextId: UUID) =
        finnSiste(contextId)?.takeIf { it.first == SUSPENDERT }?.let { (_, dto, hash) ->
            CommandContext(contextId, dto.sti, hash?.let { UUID.fromString(it) })
        }

    override fun finnSuspendertEllerFeil(contextId: UUID) =
        finnSiste(contextId)?.takeIf { it.first == SUSPENDERT || it.first == FEIL }?.let { (_, dto, hash) ->
            CommandContext(contextId, dto.sti, hash?.let { UUID.fromString(it) })
        }

    private fun finnSiste(contextId: UUID) =
        asSQL(
            """SELECT tilstand, data, hash FROM command_context WHERE context_id = :contextId ORDER BY id DESC LIMIT 1""",
            "contextId" to contextId,
        ).singleOrNull {
            Triple(
                enumValueOf<CommandContextTilstand>(it.string("tilstand")),
                mapper.readValue<CommandContextDto>(it.string("data")),
                it.stringOrNull("hash"),
            )
        }

    private class CommandContextDto(
        val sti: List<Int>,
    )

    private enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
}
