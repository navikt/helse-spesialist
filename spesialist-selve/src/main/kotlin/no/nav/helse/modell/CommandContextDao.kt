package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.AVBRUTT
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.FEIL
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.FERDIG
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.NY
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.SUSPENDERT
import no.nav.helse.modell.kommando.CommandContext
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

internal class CommandContextDao(private val dataSource: DataSource) {
    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    internal fun opprett(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, NY, null)
    }

    internal fun ferdig(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, FERDIG, null)
    }

    internal fun avbrutt(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, AVBRUTT, null)
    }

    internal fun feil(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        lagre(hendelseId, contextId, FEIL, null)
    }

    internal fun suspendert(
        hendelseId: UUID,
        contextId: UUID,
        hash: UUID,
        sti: List<Int>,
    ) {
        lagre(hendelseId, contextId, SUSPENDERT, hash, sti)
    }

    fun avbryt(
        vedtaksperiodeId: UUID,
        contextId: UUID,
    ): List<Pair<UUID, UUID>> {
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = """
                INSERT INTO command_context(context_id, hendelse_id, tilstand, data)
                    SELECT context_id, hendelse_id, :avbrutt, data
                    FROM (
                        SELECT DISTINCT ON (context_id) *
                        FROM command_context
                        WHERE hendelse_id in (
                            SELECT hendelse_ref FROM vedtaksperiode_hendelse WHERE vedtaksperiode_id = :vedtaksperiodeId
                        )
                        AND context_id != :contextId
                        ORDER BY context_id, id DESC
                ) AS command_contexts
                WHERE tilstand IN (:ny, :suspendert) RETURNING context_id, hendelse_id
            """
            return it.run(
                queryOf(
                    query,
                    mapOf(
                        "avbrutt" to AVBRUTT.name,
                        "vedtaksperiodeId" to vedtaksperiodeId,
                        "contextId" to contextId,
                        "ny" to NY.name,
                        "suspendert" to SUSPENDERT.name,
                    ),
                ).map { rad -> rad.uuid("context_id") to rad.uuid("hendelse_id") }.asList,
            )
        }
    }

    private fun lagre(
        hendelseId: UUID,
        contextId: UUID,
        tilstand: CommandContextTilstand,
        hash: UUID?,
        sti: List<Int> = emptyList(),
    ) {
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query =
                """
                INSERT INTO command_context(context_id, hendelse_id, tilstand, data, hash)
                VALUES (:contextId, :hendelseId, :tilstand, :data::json, :hash)
                """.trimIndent()
            it.run(
                queryOf(
                    query,
                    mapOf(
                        "contextId" to contextId,
                        "hendelseId" to hendelseId,
                        "tilstand" to tilstand.name,
                        "data" to mapper.writeValueAsString(CommandContextDto(sti)),
                        "hash" to hash,
                    ),
                ).asExecute,
            )
        }
    }

    internal fun tidsbrukForContext(contextId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("postgresql")
            val query =
                """
                select extract(milliseconds from (max(opprettet) - min(opprettet))) as tid_brukt_ms
                from command_context
                where context_id = :contextId
                """.trimIndent()
            // Kan bruke !! fordi mappingen thrower hvis spørringen ikke fant noe
            session.run(queryOf(query, mapOf("contextId" to contextId)).map { it.int("tid_brukt_ms") }.asSingle)!!
        }

    fun finnSuspendert(id: UUID) =
        finnSiste(id)?.takeIf { it.first == SUSPENDERT }?.let { (_, dto, hash) ->
            CommandContext(id, dto.sti, hash?.let { UUID.fromString(it) })
        }

    fun finnSuspendertEllerFeil(id: UUID) =
        finnSiste(id)?.takeIf { it.first == SUSPENDERT || it.first == FEIL }?.let { (_, dto, hash) ->
            CommandContext(id, dto.sti, hash?.let { UUID.fromString(it) })
        }

    private fun finnSiste(id: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """SELECT tilstand, data, hash FROM command_context WHERE context_id = ? ORDER BY id DESC LIMIT 1"""
            session.run(
                queryOf(
                    query,
                    id,
                ).map {
                    Triple(
                        enumValueOf<CommandContextTilstand>(it.string("tilstand")),
                        mapper.readValue<CommandContextDto>(it.string("data")),
                        it.stringOrNull("hash"),
                    )
                }.asSingle,
            )
        }

    private class CommandContextDto(val sti: List<Int>)

    private enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
}
