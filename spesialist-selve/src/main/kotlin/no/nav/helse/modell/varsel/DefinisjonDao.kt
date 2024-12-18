package no.nav.helse.modell.varsel

import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class DefinisjonDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    internal fun definisjonFor(definisjonId: UUID) =
        asSQL(
            "SELECT * FROM api_varseldefinisjon WHERE unik_id = :definisjonId",
            "definisjonId" to definisjonId,
        ).single { it.mapToDefinisjon() }.let(::checkNotNull)

    internal fun sisteDefinisjonFor(varselkode: String) =
        asSQL(
            "SELECT * FROM api_varseldefinisjon WHERE kode = :varselKode ORDER BY opprettet DESC",
            "varselKode" to varselkode,
        ).single { it.mapToDefinisjon() }.let(::checkNotNull)

    internal fun lagreDefinisjon(
        unikId: UUID,
        kode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    ) {
        asSQL(
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unikId, :kode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
            """.trimIndent(),
            "unikId" to unikId,
            "kode" to kode,
            "tittel" to tittel,
            "forklaring" to forklaring,
            "handling" to handling,
            "avviklet" to avviklet,
            "opprettet" to opprettet,
        ).update()
    }

    private fun Row.mapToDefinisjon() =
        Varseldefinisjon(
            id = uuid("unik_id"),
            varselkode = string("kode"),
            tittel = string("tittel"),
            forklaring = stringOrNull("forklaring"),
            handling = stringOrNull("handling"),
            avviklet = boolean("avviklet"),
            opprettet = localDateTime("opprettet"),
        )
}
