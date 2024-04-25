package no.nav.helse.db

import no.nav.helse.HelseDao
import java.time.LocalDateTime
import javax.sql.DataSource

internal class UnntaFraAutomatiseringDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun lagre(
        fødselsnummer: String,
        unntaFraAutomatisering: Boolean,
        årsaker: Set<String>,
    ) = asSQL(
        """
        insert into unnta_fra_automatisk_godkjenning
        values (:fnr, :unnta, ARRAY[:aarsaker])
        on conflict (fødselsnummer)
        do update set unnta = :unnta, årsaker = ARRAY[:aarsaker], oppdatert = now() 
        """.trimIndent(),
        mapOf(
            "fnr" to fødselsnummer.toLong(),
            "unnta" to unntaFraAutomatisering,
            "aarsaker" to årsaker.somDbArray(),
        ),
    ).update()

    fun sistOppdatert(fødselsnummer: String): LocalDateTime? =
        asSQL(
            """
            select oppdatert from unnta_fra_automatisk_godkjenning
            where fødselsnummer = :fnr
            """.trimIndent(),
            mapOf(
                "fnr" to fødselsnummer.toLong(),
            ),
        ).single { it.localDateTimeOrNull(1) }

    fun erUnntatt(fødselsnummer: String): Boolean =
        asSQL(
            """
            select unnta from unnta_fra_automatisk_godkjenning
            where fødselsnummer = :fnr
            """.trimIndent(),
            mapOf(
                "fnr" to fødselsnummer.toLong(),
            ),
        ).single { it.boolean(1) } ?: false
}

fun <T> Iterable<T>.somDbArray() = joinToString { "'$it'" }
