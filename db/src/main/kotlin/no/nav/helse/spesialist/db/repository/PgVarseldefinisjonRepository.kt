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

    override fun finn(id: VarseldefinisjonId): Varseldefinisjon? =
        dbQuery.singleOrNull(
            """
                SELECT kode, unik_id, tittel, forklaring, handling FROM api_varseldefinisjon WHERE unik_id = :unik_id
            """,
            "unik_id" to id.value,
        ) {
            it.mapTilVarseldefinisjon()
        }

    override fun finnGjeldendeFor(kode: String): Varseldefinisjon? =
        dbQuery.singleOrNull(
            """
                SELECT DISTINCT ON (kode) kode, unik_id, tittel, forklaring, handling FROM api_varseldefinisjon WHERE kode = :kode
                ORDER BY kode, opprettet DESC
            """,
            "kode" to kode,
        ) {
            it.mapTilVarseldefinisjon()
        }

    private fun Row.mapTilVarseldefinisjon(): Varseldefinisjon =
        Varseldefinisjon.fraLagring(
            id = VarseldefinisjonId(uuid("unik_id")),
            kode = string("kode"),
            tittel = string("tittel"),
            forklaring = stringOrNull("forklaring"),
            handling = stringOrNull("handling"),
        )
}
