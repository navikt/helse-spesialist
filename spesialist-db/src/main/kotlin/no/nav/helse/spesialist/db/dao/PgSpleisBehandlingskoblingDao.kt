package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.SpleisBehandlingskoblingDao
import no.nav.helse.db.SpleisBehandlingskoblingDao.SpesialistBehandling
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.list
import no.nav.helse.spesialist.db.HelseDao.Companion.single
import no.nav.helse.spesialist.db.HelseDao.Companion.update
import java.util.UUID

class PgSpleisBehandlingskoblingDao internal constructor(
    private val session: Session,
) : SpleisBehandlingskoblingDao {
    override fun finnBehandlinger(vedtaksperiodeId: UUID): List<SpesialistBehandling> =
        asSQL(
            """
            SELECT id, utbetaling_id, spleis_behandling_id, opprettet_tidspunkt FROM behandling
            WHERE vedtaksperiode_id = :vedtaksperiode_id AND opprettet_tidspunkt IS NOT NULL
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list(session) { row ->
            SpesialistBehandling(
                id = row.long("id"),
                utbetalingId = row.uuidOrNull("utbetaling_id"),
                spleisBehandlingId = row.uuidOrNull("spleis_behandling_id"),
                opprettet = row.localDateTime("opprettet_tidspunkt"),
            )
        }

    override fun antallVarslerUtenSpleisBehandlingId(behandlingRef: Long): Int =
        asSQL(
            "SELECT count(*) FROM varsel WHERE behandling_ref = :behandling_ref AND spleis_behandling_id IS NULL",
            "behandling_ref" to behandlingRef,
        ).single(session) { it.int(1) } ?: 0

    override fun settSpleisBehandlingIdPåVarsler(
        behandlingRef: Long,
        spleisBehandlingId: UUID,
    ): Int =
        asSQL(
            """
            UPDATE varsel SET spleis_behandling_id = :spleis_behandling_id
            WHERE behandling_ref = :behandling_ref AND spleis_behandling_id IS NULL
            """.trimIndent(),
            "spleis_behandling_id" to spleisBehandlingId,
            "behandling_ref" to behandlingRef,
        ).update(session)
}
