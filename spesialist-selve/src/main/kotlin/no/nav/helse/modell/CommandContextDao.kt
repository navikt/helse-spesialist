package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.TransactionalCommandContextDao
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.AVBRUTT
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.FEIL
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.NY
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.SUSPENDERT
import no.nav.helse.modell.kommando.CommandContext
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

internal class CommandContextDao(
    private val dataSource: DataSource,
) : CommandContextRepository {
    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    override fun opprett(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalCommandContextDao(transaction).opprett(hendelseId, contextId)
            }
        }
    }

    override fun ferdig(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalCommandContextDao(transaction).ferdig(hendelseId, contextId)
            }
        }
    }

    override fun feil(
        hendelseId: UUID,
        contextId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalCommandContextDao(transaction).feil(hendelseId, contextId)
            }
        }
    }

    override fun suspendert(
        hendelseId: UUID,
        contextId: UUID,
        hash: UUID,
        sti: List<Int>,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalCommandContextDao(transaction).suspendert(hendelseId, contextId, hash, sti)
            }
        }
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

    override fun tidsbrukForContext(contextId: UUID): Int =
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalCommandContextDao(transaction).tidsbrukForContext(contextId)
            }
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
            val query =
                """SELECT tilstand, data, hash FROM command_context WHERE context_id = ? ORDER BY id DESC LIMIT 1"""
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

    private class CommandContextDto(
        val sti: List<Int>,
    )

    private enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
}
