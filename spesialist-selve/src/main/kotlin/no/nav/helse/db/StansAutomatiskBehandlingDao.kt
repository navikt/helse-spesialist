package no.nav.helse.db

import no.nav.helse.HelseDao
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import java.time.LocalDateTime
import javax.sql.DataSource

class StansAutomatiskBehandlingDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun lagreFraISyfo(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    ) = asSQL(
        """
        insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
        values (:fnr, :status, '{${
            årsaker.map(StoppknappÅrsak::name).somDbArray()
        }}', :opprettet, :kilde, cast(:originalMelding as json))
        """.trimIndent(),
        mapOf(
            "fnr" to fødselsnummer,
            "status" to status,
            "opprettet" to opprettet,
            "kilde" to kilde,
            "originalMelding" to originalMelding,
        ),
    ).update()

    fun lagreFraSpeil(fødselsnummer: String) =
        asSQL(
            """
            insert into stans_automatisering (fødselsnummer, status, årsaker, opprettet, kilde, original_melding) 
            values (:fnr, 'NORMAL', '{}', now(), 'SPEIL', cast(:originalMelding as json))
            """.trimIndent(),
            mapOf(
                "fnr" to fødselsnummer,
                "originalMelding" to null,
            ),
        ).update()

    fun hentFor(fødselsnummer: String) =
        asSQL(
            """
            select fødselsnummer, status, årsaker, opprettet, original_melding->>'uuid' as melding_id 
            from stans_automatisering 
            where fødselsnummer = :fnr
            """.trimIndent(),
            mapOf(
                "fnr" to fødselsnummer,
            ),
        ).list { row ->
            StansAutomatiskBehandlingFraDatabase(
                fødselsnummer = row.string("fødselsnummer"),
                status = row.string("status"),
                årsaker = row.array<String>("årsaker").map { enumValueOf<StoppknappÅrsak>(it) }.toSet(),
                opprettet = row.localDateTime("opprettet"),
                meldingId = row.stringOrNull("melding_id"),
            )
        }
}

fun <T> Iterable<T>.somDbArray() = joinToString { " $it " }
