package no.nav.helse.opprydding

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class CommandContextDao(private val dataSource: DataSource) {
    fun finnAktiveKommandokjeder(fødselsnummer: String): List<Kommandokjedeinfo> =
        sessionOf(dataSource).use { session ->
            @Language("postgresql")
            val query =
                """
                select cc.context_id, h.id as hendelse_id
                from command_context cc
                         join hendelse h on cc.hendelse_id = h.id
                where tilstand in ('NY', 'SUSPENDERT', 'FEIL')
                  and h.fodselsnummer = :fodselsnummer
                """.trimIndent()
            session.run(
                queryOf(
                    query,
                    mapOf("fodselsnummer" to fødselsnummer.toLong()),
                ).map { Kommandokjedeinfo(it.uuid("context_id"), it.uuid("hendelse_id")) }.asList,
            )
        }

    data class Kommandokjedeinfo(val contextId: UUID, val hendelseId: UUID)
}
