package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.VarseldefinisjonRepository
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId

class PgVarseldefinisjonRepository private constructor(
    private val dbQuery: DbQuery,
) : VarseldefinisjonRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun finnOrNull(id: VarseldefinisjonId): Varseldefinisjon? =
        dbQuery.singleOrNull(
            """
                SELECT kode, unik_id, tittel, forklaring, handling, avviklet, opprettet FROM api_varseldefinisjon WHERE unik_id = :unik_id
            """,
            "unik_id" to id.value,
        ) {
            it.mapTilVarseldefinisjon()
        }

    override fun finnGjeldendeForOrNull(kode: String): Varseldefinisjon? =
        dbQuery.singleOrNull(
            """
                SELECT DISTINCT ON (kode) kode, unik_id, tittel, forklaring, handling, avviklet, opprettet FROM api_varseldefinisjon WHERE kode = :kode
                ORDER BY kode, opprettet DESC
            """,
            "kode" to kode,
        ) {
            it.mapTilVarseldefinisjon()
        }

    override fun lagre(varseldefinisjon: Varseldefinisjon) {
        dbQuery.update(
            """
                INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
                VALUES (:unik_id, :kode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
                ON CONFLICT (unik_id) DO UPDATE SET
                    kode = EXCLUDED.kode,
                    tittel = EXCLUDED.tittel,
                    forklaring = EXCLUDED.forklaring,
                    handling = EXCLUDED.handling,
                    avviklet = EXCLUDED.avviklet,
                    opprettet = EXCLUDED.opprettet
            """,
            "unik_id" to varseldefinisjon.id.value,
            "kode" to varseldefinisjon.kode,
            "tittel" to varseldefinisjon.tittel,
            "forklaring" to varseldefinisjon.forklaring,
            "handling" to varseldefinisjon.handling,
            "avviklet" to varseldefinisjon.avviklet,
            "opprettet" to varseldefinisjon.opprettet,
        )
    }

    private fun Row.mapTilVarseldefinisjon(): Varseldefinisjon =
        Varseldefinisjon.fraLagring(
            id = VarseldefinisjonId(uuid("unik_id")),
            kode = string("kode"),
            tittel = string("tittel"),
            forklaring = stringOrNull("forklaring"),
            handling = stringOrNull("handling"),
            avviklet = boolean("avviklet"),
            opprettet = localDateTime("opprettet"),
        )
}
