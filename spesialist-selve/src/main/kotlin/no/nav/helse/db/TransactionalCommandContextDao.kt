package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.FEIL
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.FERDIG
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.NY
import no.nav.helse.db.TransactionalCommandContextDao.CommandContextTilstand.SUSPENDERT
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.naming.OperationNotSupportedException

internal class TransactionalCommandContextDao(
    private val transactionalSession: TransactionalSession,
) : CommandContextRepository {
    private companion object {
        private val mapper = jacksonObjectMapper()
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
        throw OperationNotSupportedException()
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
        return transactionalSession.run(
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
        transactionalSession.run(
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
