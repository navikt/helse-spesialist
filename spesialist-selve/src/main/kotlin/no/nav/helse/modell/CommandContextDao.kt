package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.*
import no.nav.helse.modell.kommando.CommandContext
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class CommandContextDao(private val dataSource: DataSource) {
    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    internal fun opprett(hendelse: Hendelse, contextId: UUID) {
        lagre(hendelse, contextId, NY)
    }

    internal fun ferdig(hendelse: Hendelse, contextId: UUID) {
        lagre(hendelse, contextId, FERDIG)
    }

    internal fun feil(hendelse: Hendelse, contextId: UUID) {
        lagre(hendelse, contextId, FEIL)
    }

    internal fun suspendert(hendelse: Hendelse, contextId: UUID, sti: List<Int>) {
        lagre(hendelse, contextId, SUSPENDERT, sti)
    }

    fun avbryt(vedtaksperiodeId: UUID, contextId: UUID) {
        sessionOf(dataSource).use  {
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
                WHERE tilstand IN (:ny, :suspendert)
            """
            it.run(
                queryOf(
                    query,
                    mapOf(
                        "avbrutt" to AVBRUTT.name,
                        "vedtaksperiodeId" to vedtaksperiodeId,
                        "contextId" to contextId,
                        "ny" to NY.name,
                        "suspendert" to SUSPENDERT.name
                    )
                ).asUpdate
            )
        }
    }

    private fun lagre(
        hendelse: Hendelse,
        contextId: UUID,
        tilstand: CommandContextTilstand,
        sti: List<Int> = emptyList()
    ) {
        sessionOf(dataSource).use  {
            it.run(
                queryOf(
                    "INSERT INTO command_context(context_id,hendelse_id,tilstand,data) VALUES (?, ?, ?, ?::json)",
                    contextId,
                    hendelse.id,
                    tilstand.name,
                    mapper.writeValueAsString(CommandContextDto(sti))
                ).asExecute
            )
        }
    }

    fun finnSuspendert(id: UUID) = finnSiste(id)?.takeIf { it.first == SUSPENDERT }?.let { (_, dto) ->
        CommandContext(id, dto.sti)
    }

    private fun finnSiste(id: UUID) =
        sessionOf(dataSource).use  { session ->
            session.run(
                queryOf(
                    "SELECT tilstand, data FROM command_context WHERE context_id = ? ORDER BY id DESC LIMIT 1",
                    id
                ).map {
                    enumValueOf<CommandContextTilstand>(it.string("tilstand")) to
                        mapper.readValue<CommandContextDto>(it.string("data"))
                }.asSingle
            )
        }

    private class CommandContextDto(val sti: List<Int>)

    private enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
}

