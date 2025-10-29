package no.nav.helse.spesialist.db.repository

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

    override fun finnGjeldendeFor(kode: String): Varseldefinisjon? =
        dbQuery.singleOrNull(
            """
                SELECT DISTINCT ON (kode) unik_id, tittel FROM api_varseldefinisjon WHERE kode = :kode
                ORDER BY kode, opprettet DESC
            """,
            "kode" to kode,
        ) {
            Varseldefinisjon.fraLagring(
                id = VarseldefinisjonId(it.uuid("unik_id")),
                kode = kode,
                tittel = it.string("tittel"),
            )
        }
}
