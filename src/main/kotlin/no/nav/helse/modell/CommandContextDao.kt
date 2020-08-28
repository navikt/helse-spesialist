package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.CommandContextDao.CommandContextTilstand.*
import no.nav.helse.modell.command.nyny.CommandContext
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
        using(sessionOf(dataSource)) {
            @Language("PostgreSQL")
            val query = """
                INSERT INTO command_context(context_id,spleisbehov_id,tilstand,vedtaksperiode_id,data)
                    SELECT context_id, spleisbehov_id,?, vedtaksperiode_id, data
                    FROM command_context
                    WHERE vedtaksperiode_id = ? AND context_id != ? AND context_id NOT IN (
                        SELECT context_id
                        FROM command_context
                        WHERE vedtaksperiode_id = ? AND tilstand IN (?,?,?)
                    )
            """
            it.run(
                queryOf(
                    query,
                    AVBRUTT.name,
                    vedtaksperiodeId,
                    contextId,
                    vedtaksperiodeId,
                    FEIL.name,
                    FERDIG.name,
                    AVBRUTT.name
                ).asExecute
            )
        }
    }

    private fun lagre(hendelse: Hendelse, contextId: UUID, tilstand: CommandContextTilstand, sti: List<Int> = emptyList()) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO command_context(context_id,spleisbehov_id,tilstand,vedtaksperiode_id,data) VALUES (?, ?, ?, ?, ?::json)",
                    contextId,
                    hendelse.id,
                    tilstand.name,
                    hendelse.vedtaksperiodeId(),
                    mapper.writeValueAsString(CommandContextDto(sti))
                ).asExecute
            )
        }
    }

    fun finn(id: UUID) =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT data FROM command_context WHERE context_id = ? ORDER BY id DESC LIMIT 1", id).map {
                val dto = mapper.readValue<CommandContextDto>(it.string("data"))
                CommandContext(id, dto.sti)
            }.asSingle)
        }

    private class CommandContextDto(val sti: List<Int>)

    private enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
}

