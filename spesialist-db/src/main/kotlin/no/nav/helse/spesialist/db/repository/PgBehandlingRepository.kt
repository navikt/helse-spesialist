package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.util.UUID

class PgBehandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    BehandlingRepository {
    override fun finn(id: SpleisBehandlingId): Behandling? =
        asSQL(
            """
            SELECT spleis_behandling_id, tags, p.fødselsnummer, b.fom, b.tom, b.skjæringstidspunkt, b.yrkesaktivitetstype
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE spleis_behandling_id = :spleis_behandling_id
        """,
            "spleis_behandling_id" to id.value,
        ).singleOrNull { row ->
            row.behandling()
        }

    override fun finnNyeste(vedtaksperiodeId: UUID): Behandling? =
        asSQL(
            """
            SELECT spleis_behandling_id, tags, p.fødselsnummer, b.fom, b.tom, b.skjæringstidspunkt, b.yrkesaktivitetstype
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE b.vedtaksperiode_id = :vedtaksperiode_id
            ORDER BY b.id DESC
            LIMIT 1
        """,
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).singleOrNull { row ->
            row.behandling()
        }

    private fun Row.behandling(): Behandling =
        Behandling.fraLagring(
            id = SpleisBehandlingId(uuid("spleis_behandling_id")),
            tags = array<String>("tags").toSet(),
            fødselsnummer = string("fødselsnummer"),
            fom = localDate("fom"),
            tom = localDate("tom"),
            skjæringstidspunkt = localDate("skjæringstidspunkt"),
            yrkesaktivitetstype = stringOrNull("yrkesaktivitetstype")?.let { Yrkesaktivitetstype.valueOf(it) } ?: Yrkesaktivitetstype.ARBEIDSTAKER,
        )
}
