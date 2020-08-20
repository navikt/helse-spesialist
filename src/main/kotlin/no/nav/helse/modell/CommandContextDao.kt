package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.CommandContextTilstand.*
import no.nav.helse.modell.command.nyny.CommandContext
import java.util.*
import javax.sql.DataSource

internal class CommandContextDao(private val dataSource: DataSource) {
    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    fun lagre(hendelse: Hendelse, context: CommandContext, tilstand: CommandContextTilstand) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO command_context(context_id,spleisbehov_id,tilstand,vedtaksperiode_id,data) VALUES (?, ?, ?, ?, ?::json)",
                    context.id,
                    hendelse.id,
                    tilstand.name,
                    hendelse.vedtaksperiodeId(),
                    mapper.writeValueAsString(CommandContextDto(context.sti()))
                ).asExecute
            )
        }
    }

    fun avbryt(vedtaksperiodeId: UUID) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "UPDATE command_context SET tilstand = ? WHERE vedtaksperiode_id = ? AND tilstand in (?,?)",
                    AVBRUTT.name,
                    vedtaksperiodeId,
                    SUSPENDERT.name,
                    NY.name
                ).asUpdate
            )
        }
    }

    fun finn(id: UUID) =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT data FROM command_context WHERE context_id = ? ORDER BY id DESC LIMIT 1", id).map {
                val dto = mapper.readValue<CommandContextDto>(it.string("data"))
                CommandContext(id).apply { sti(dto.sti) }
            }.asSingle)
        }

    private class CommandContextDto(val sti: List<Int>)
}

internal enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL, AVBRUTT }
