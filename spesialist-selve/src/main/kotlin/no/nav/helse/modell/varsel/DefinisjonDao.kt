package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import org.intellij.lang.annotations.Language

class DefinisjonDao(private val dataSource: DataSource) {

    internal fun definisjonFor(definisjonId: UUID): Varseldefinisjon {
        @Language("PostgreSQL")
        val query = "SELECT * FROM api_varseldefinisjon WHERE unik_id = ?;"

        return requireNotNull(sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    definisjonId
                ).map(this::mapToDefinisjon).asSingle
            )
        })
    }

    internal fun sisteDefinisjonFor(varselkode: String): Varseldefinisjon {
        @Language("PostgreSQL")
        val query = "SELECT * FROM api_varseldefinisjon WHERE kode = ? ORDER BY opprettet DESC;"

        return requireNotNull(sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    varselkode
                ).map(this::mapToDefinisjon).asSingle
            )
        })
    }

    internal fun lagreDefinisjon(
        unikId: UUID,
        kode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    ) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING;"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, unikId, kode, tittel, forklaring, handling, avviklet, opprettet).asUpdate)
        }
    }

    // Midlertidig - Oppdater opprettet-tidspunkt for eksisterende definisjoner
    internal fun oppdaterOpprettetTidspunkt(
        unikId: UUID,
        opprettet: LocalDateTime,
    ) {
        @Language("PostgreSQL")
        val query =
            "UPDATE api_varseldefinisjon SET opprettet = :opprettet WHERE unik_id = :unik_id;"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("opprettet" to opprettet, "unik_id" to unikId)).asUpdate)
        }
    }

    private fun mapToDefinisjon(row: Row): Varseldefinisjon {
        return Varseldefinisjon(
            id = row.uuid("unik_id"),
            varselkode = row.string("kode"),
            tittel = row.string("tittel"),
            forklaring = row.stringOrNull("forklaring"),
            handling = row.stringOrNull("handling"),
            avviklet = row.boolean("avviklet"),
            opprettet = row.localDateTime("opprettet")
        )
    }
}