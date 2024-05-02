package no.nav.helse.db

import no.nav.helse.HelseDao
import no.nav.helse.modell.stoppautomatiskbehandling.Kilde
import no.nav.helse.modell.stoppautomatiskbehandling.Status
import no.nav.helse.modell.stoppautomatiskbehandling.Årsak
import java.time.LocalDateTime
import javax.sql.DataSource

class StansAutomatiskBehandlingDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun lagre(
        fødselsnummer: String,
        status: Status,
        årsaker: Set<Årsak>,
        opprettet: LocalDateTime,
        originalMelding: String,
        kilde: Kilde,
    ) = asSQL(
        """
        insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
        values (:fnr, :status, '{${årsaker.somDbArray()}}', :opprettet, :kilde, cast(:originalMelding as json))
        """.trimIndent(),
        mapOf(
            "fnr" to fødselsnummer,
            "status" to status.name,
            "opprettet" to opprettet,
            "kilde" to kilde.name,
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
                status = enumValueOf(row.string("status")),
                årsaker = row.array<String>("årsaker").map { enumValueOf<Årsak>(it) }.toSet(),
                opprettet = row.localDateTime("opprettet"),
            )
        }
}

fun Iterable<Årsak>.somDbArray() = joinToString { """ "${it.name}" """ }
