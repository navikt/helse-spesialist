package no.nav.helse.db

import no.nav.helse.HelseDao
import java.time.LocalDateTime
import javax.sql.DataSource

class StansAutomatiskBehandlingDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun lagre(
        fødselsnummer: String,
        status: String,
        årsaker: Set<String>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    ) = asSQL(
        """
        insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
        values (:fnr, :status, '{${årsaker.somDbArray()}}', :opprettet, :kilde, cast(:originalMelding as json))
        """.trimIndent(),
        mapOf(
            "fnr" to fødselsnummer,
            "status" to status,
            "opprettet" to opprettet,
            "kilde" to kilde,
            "originalMelding" to originalMelding,
        ),
    ).update()

    fun hent(fødselsnummer: String) =
        asSQL(
            """
            select * from stans_automatisering where fødselsnummer = :fnr
            """.trimIndent(),
            mapOf(
                "fnr" to fødselsnummer,
            ),
        ).list { row ->
            StansAutomatiskBehandlingFraDatabase(
                fødselsnummer = row.string("fødselsnummer"),
                status = row.string("status"),
                årsaker = row.array<String>("årsaker").toSet(),
                opprettet = row.localDateTime("opprettet"),
            )
        }
}

fun <T> Iterable<T>.somDbArray() = joinToString { " $it " }
