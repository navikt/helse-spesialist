package no.nav.helse.modell.stoppknapp

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

internal class UnntaFraAutomatiseringDao(private val dataSource: DataSource) {
    fun lagre(
        fødselsnummer: String,
        unntaFraAutomatisering: Boolean,
        årsaker: Set<String>,
    ) {
        @Language("postgresql")
        val statement =
            """
            insert into unnta_fra_automatisk_godkjenning
            values (:fnr, :unnta, ARRAY[:aarsaker])
            on conflict (fødselsnummer)
            do update set unnta = :unnta, årsaker = ARRAY[:aarsaker], oppdatert = now() 
            """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "fnr" to fødselsnummer.toLong(),
                        "unnta" to unntaFraAutomatisering,
                        "aarsaker" to årsaker.somDbArray(),
                    ),
                ).asUpdate,
            )
        }
    }

    fun sistOppdatert(fødselsnummer: String): LocalDateTime? {
        @Language("postgresql")
        val statement =
            """
            select oppdatert from unnta_fra_automatisk_godkjenning
            where fødselsnummer = :fnr
            """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf("fnr" to fødselsnummer.toLong())).map { it.localDateTimeOrNull(1) }.asSingle)
        }
    }

    fun erUnntatt(fødselsnummer: String): Boolean {
        @Language("postgresql")
        val statement =
            """
            select unnta from unnta_fra_automatisk_godkjenning
            where fødselsnummer = :fnr
            """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf("fnr" to fødselsnummer.toLong())).map { it.boolean(1) }.asSingle)
        } ?: false
    }
}

fun <T> Iterable<T>.somDbArray() = joinToString { "'$it'" }
