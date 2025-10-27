package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.spesialist.application.VedtaksperiodeRepository
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class PgVedtaksperiodeRepository private constructor(
    private val dbQuery: DbQuery,
) : VedtaksperiodeRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun finn(vedtaksperiodeId: VedtaksperiodeId): Vedtaksperiode? =
        dbQuery.singleOrNull(
            """
                SELECT vedtaksperiode_id, fødselsnummer
                FROM vedtak v
                         JOIN person p ON v.person_ref = p.id
                WHERE v.vedtaksperiode_id = :vedtaksperiodeId
                """,
            "vedtaksperiodeId" to vedtaksperiodeId.value,
        ) {
            Vedtaksperiode(
                id = VedtaksperiodeId(it.uuid("vedtaksperiode_id")),
                fødselsnummer = it.string("fødselsnummer"),
            )
        }
}
