package no.nav.helse.sidegig

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class PgBehandlingDao(
    private val dataSource: DataSource,
) : BehandlingDao {
    override fun lagreBehandling(behandling: Behandling) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO behandling_v2 (vedtaksperiode_id, behandling_id, fom, tom, skjæringstidspunkt, opprettet)
            VALUES (:vedtaksperiodeId, :behandlingId, :fom, :tom, :skjaeringstidspunkt, :opprettet)
            ON CONFLICT (behandling_id) DO NOTHING
            """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "vedtaksperiodeId" to behandling.vedtaksperiodeId,
                        "behandlingId" to behandling.behandlingId,
                        "fom" to behandling.fom,
                        "tom" to behandling.tom,
                        "skjaeringstidspunkt" to behandling.skjæringstidspunkt,
                        "opprettet" to behandling.opprettet,
                    ),
                ).asUpdate,
            )
        }
    }
}
