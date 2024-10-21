package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.AVBRUTT
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.FEIL
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.FERDIG
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.NY
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.SUSPENDERT
import no.nav.helse.modell.kommando.CommandContext
import org.intellij.lang.annotations.Language
import java.util.UUID

internal class TransactionalCommandContextDao(
    private val session: Session,
) : CommandContextRepository {
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
        val konteksterSomSkalAvbrytes = finnContexterSomSkalAvbrytes(contextId, vedtaksperiodeId)
        konteksterSomSkalAvbrytes.forEach {
            avbrytContext(it.contextId, it.hendelseId, it.data)
        }
        return konteksterSomSkalAvbrytes.map { it.contextId to it.hendelseId }
    }

    private data class ContextRad(
        val contextId: UUID,
        val hendelseId: UUID,
        val data: String,
    )

    private fun avbrytContext(
        contextId: UUID,
        hendelseId: UUID,
        data: String,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO command_context(context_id, hendelse_id, tilstand, data)
            VALUES (:contextId, :hendelseId, :avbrutt, :data::json)
            """.trimIndent()
        session
            .run(
                queryOf(
                    query,
                    mapOf(
                        "avbrutt" to AVBRUTT.name,
                        "contextId" to contextId,
                        "hendelseId" to hendelseId,
                        "data" to data,
                    ),
                ).asUpdate,
            )
    }

    private fun finnContexterSomSkalAvbrytes(
        contextId: UUID,
        vedtaksperiodeId: UUID,
    ): List<ContextRad> {
        @Language("PostgreSQL")
        val query = """
            WITH siste_contexter AS (SELECT DISTINCT ON (context_id) context_id, hendelse_id, tilstand, data
            FROM command_context
            WHERE hendelse_id in (
                SELECT hendelse_ref FROM vedtaksperiode_hendelse WHERE vedtaksperiode_id = :vedtaksperiodeId
            )
            AND context_id != :contextId
            ORDER BY context_id, id DESC)
            SELECT * FROM siste_contexter WHERE tilstand IN (:ny, :suspendert)
        """
        return session.run(
            queryOf(
                query,
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "contextId" to contextId,
                    "ny" to NY.name,
                    "suspendert" to SUSPENDERT.name,
                ),
            ).map { ContextRad(it.uuid("context_id"), it.uuid("hendelse_id"), it.string("data")) }.asList,
        )
    }

    override fun tidsbrukForContext(contextId: UUID): Int {
        @Language("postgresql")
        val query =
            """
            select extract(milliseconds from (max(opprettet) - min(opprettet))) as tid_brukt_ms
            from command_context
            where context_id = :contextId
            """.trimIndent()
        // Kan bruke !! fordi mappingen thrower hvis spørringen ikke fant noe
        return session.run(
            queryOf(
                query,
                mapOf("contextId" to contextId),
            ).map { it.int("tid_brukt_ms") }.asSingle,
        )!!
    }

    private fun lagre(
        hendelseId: UUID,
        contextId: UUID,
        tilstand: CommandContextTilstand,
        hash: UUID?,
        sti: List<Int> = emptyList(),
    ) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO command_context(context_id, hendelse_id, tilstand, data, hash)
            VALUES (:contextId, :hendelseId, :tilstand, :data::json, :hash)
            """.trimIndent()
        session.run(
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

    private class CommandContextDto(
        val sti: List<Int>,
    )

    private enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
}
