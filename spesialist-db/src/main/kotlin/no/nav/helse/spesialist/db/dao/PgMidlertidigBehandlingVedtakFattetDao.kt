package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.spesialist.application.MidlertidigBehandlingVedtakFattetDao
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class PgMidlertidigBehandlingVedtakFattetDao(
    session: Session,
) : MidlertidigBehandlingVedtakFattetDao {
    private val dbQuery = SessionDbQuery(session)

    override fun vedtakFattet(spleisBehandlingId: SpleisBehandlingId) {
        dbQuery.update(
            """
                INSERT INTO midlertidig_behandling_vedtak_fattet(spleis_behandling_id) VALUES (:spleis_behandling_id)
            """,
            "spleis_behandling_id" to spleisBehandlingId.value,
        )
    }

    override fun erVedtakFattet(spleisBehandlingId: SpleisBehandlingId) =
        dbQuery.singleOrNull(
            """
            SELECT true FROM midlertidig_behandling_vedtak_fattet WHERE spleis_behandling_id = :spleis_behandling_id
        """,
            "spleis_behandling_id" to spleisBehandlingId.value,
        ) { it.boolean(1) } ?: false
}
