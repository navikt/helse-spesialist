package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class PgBehandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    BehandlingRepository {
    override fun finn(id: SpleisBehandlingId): Behandling? =
        asSQL(
            """
            SELECT spleis_behandling_id, tags, p.fødselsnummer
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE spleis_behandling_id = :spleis_behandling_id
        """,
            "spleis_behandling_id" to id.value,
        ).singleOrNull { row ->
            Behandling.fraLagring(
                id = SpleisBehandlingId(row.uuid("spleis_behandling_id")),
                tags = row.array<String>("tags").toSet(),
                fødselsnummer = row.string("fødselsnummer"),
            )
        }
}
