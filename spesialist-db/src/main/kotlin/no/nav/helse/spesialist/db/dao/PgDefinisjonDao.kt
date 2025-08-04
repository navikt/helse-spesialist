package no.nav.helse.spesialist.db.dao

import kotliquery.Row
import no.nav.helse.db.DefinisjonDao
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgDefinisjonDao internal constructor(
    dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource),
    DefinisjonDao {
    override fun definisjonFor(definisjonId: UUID) =
        asSQL(
            "SELECT * FROM api_varseldefinisjon WHERE unik_id = :definisjonId LIMIT 1",
            "definisjonId" to definisjonId,
        ).single { it.mapToDefinisjon() }.let(::checkNotNull)

    override fun sisteDefinisjonFor(varselkode: String) =
        asSQL(
            "SELECT * FROM api_varseldefinisjon WHERE kode = :varselKode ORDER BY opprettet DESC LIMIT 1",
            "varselKode" to varselkode,
        ).single { it.mapToDefinisjon() }.let(::checkNotNull)

    override fun lagreDefinisjon(
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
