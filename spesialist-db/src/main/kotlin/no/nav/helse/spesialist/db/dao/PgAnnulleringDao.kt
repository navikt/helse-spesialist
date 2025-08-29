package no.nav.helse.spesialist.db.dao

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.AnnullertAvSaksbehandlerRow
import no.nav.helse.db.BehandlingISykefraværstilfelleRow
import no.nav.helse.db.BehandlingsperiodeRow
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class PgAnnulleringDao(
    private val dataSource: DataSource,
) : AnnulleringDao {
    override fun find10Annulleringer(): List<AnnullertAvSaksbehandlerRow> =
        listQuery(
            query =
                """
                SELECT id, arbeidsgiver_fagsystem_id, person_fagsystem_id
                FROM annullert_av_saksbehandler
                WHERE vedtaksperiode_id IS NULL
                  AND arbeidsgiver_fagsystem_id IS NOT NULL
                  AND person_fagsystem_id IS NOT NULL
                LIMIT 10
                """.trimIndent(),
        ) { row ->
            AnnullertAvSaksbehandlerRow(
                id = row.int("id"),
                arbeidsgiver_fagsystem_id = row.string("arbeidsgiver_fagsystem_id"),
                person_fagsystem_id = row.string("person_fagsystem_id"),
            )
        }

    override fun findUtbetalingId(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): UUID? =
        listQuery(
            query =
                """
                SELECT uid.utbetaling_id
                FROM oppdrag a_oppdrag,
                     oppdrag p_oppdrag,
                     utbetaling_id uid
                WHERE :arbeidsgiver_fagsystem_id = a_oppdrag.fagsystem_id
                  AND :person_fagsystem_id = p_oppdrag.fagsystem_id
                  AND uid.arbeidsgiver_fagsystem_id_ref = a_oppdrag.id
                  AND uid.person_fagsystem_id_ref = p_oppdrag.id
                """.trimIndent(),
            paramMap =
                mapOf(
                    "arbeidsgiver_fagsystem_id" to arbeidsgiverFagsystemId,
                    "person_fagsystem_id" to personFagsystemId,
                ),
        ) { row -> row.uuid("utbetaling_id") }.firstOrNull()

    override fun finnBehandlingISykefraværstilfelle(utbetalingId: UUID): BehandlingISykefraværstilfelleRow? =
        query(
            query =
                """
                SELECT behandling.id,
                       behandling.vedtaksperiode_id,
                       behandling.skjæringstidspunkt,
                       person.id as person_id,
                       vedtak.arbeidsgiver_identifikator,
                       behandling.utbetaling_id
                FROM behandling,
                     vedtak,
                     person
                WHERE behandling.utbetaling_id = :utbetalingId
                  AND behandling.vedtaksperiode_id = vedtak.vedtaksperiode_id
                  AND vedtak.person_ref = person.id
                """.trimIndent(),
            paramMap = mapOf("utbetalingId" to utbetalingId),
        ) { row ->
            BehandlingISykefraværstilfelleRow(
                behandlingId = row.long("id"),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                skjæringstidspunkt = row.localDate("skjæringstidspunkt"),
                personId = row.long("person_id"),
                arbeidsgiverId = row.string("arbeidsgiver_identifikator"),
                utbetalingId = row.uuid("utbetaling_id"),
            )
        }

    override fun finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandlingISykefraværstilfelleRow: BehandlingISykefraværstilfelleRow): UUID? =
        listQuery(
            query =
                """
                SELECT behandling.vedtaksperiode_id,
                    behandling.fom,
                    behandling.tom,
                    behandling.utbetaling_id
                FROM behandling,
                     vedtak,
                     person
                WHERE person.id = :personId
                  AND vedtak.arbeidsgiver_identifikator = :arbeidsgiverId
                  AND vedtak.person_ref = person.id
                  AND vedtak.vedtaksperiode_id = behandling.vedtaksperiode_id
                ORDER BY behandling.fom DESC
                """.trimIndent(),
            paramMap =
                mapOf(
                    "personId" to behandlingISykefraværstilfelleRow.personId,
                    "arbeidsgiverId" to behandlingISykefraværstilfelleRow.arbeidsgiverId,
                ),
        ) { row ->
            BehandlingsperiodeRow(
                vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
                fom = row.localDate("fom"),
                tom = row.localDate("tom"),
                utbetalingId = row.uuidOrNull("utbetaling_id"),
            )
        }.let {
            val behandlingForUtbetaling =
                it.find { behandling -> behandling.vedtaksperiodeId == behandlingISykefraværstilfelleRow.vedtaksperiodeId }
            checkNotNull(behandlingForUtbetaling)
            it
                .filter { behandling -> behandling.fom < behandlingForUtbetaling.fom } // Fjerne alle nyere enn den "vilkårlige" behandlingen man har funnet
                .filter { behandling -> behandling.vedtaksperiodeId != behandlingForUtbetaling.vedtaksperiodeId } // Fjerne den "vilkårlige" behandlingen, siden initial state på fold er den
                .fold(listOf(behandlingForUtbetaling)) { acc, row ->
                    // Sjekk bakover i tid
                    if (acc.last().fom.minusDays(16) < row.tom) {
                        acc.plus(row)
                    } else {
                        acc
                    }
                }
        }.lastOrNull()
            ?.vedtaksperiodeId

    override fun oppdaterAnnulleringMedVedtaksperiodeId(
        annulleringId: Int,
        vedtaksperiodeId: UUID,
    ) = update(
        query =
            """
            UPDATE annullert_av_saksbehandler
            SET vedtaksperiode_id = :vedtaksperiodeId
            WHERE id = :annulleringId
            """.trimIndent(),
        paramMap =
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "annulleringId" to annulleringId,
            ),
    )

    private fun <T> query(
        @Language("PostgreSQL") query: String,
        paramMap: Map<String, Any> = emptyMap(),
        extractor: (row: Row) -> T,
    ): T? =
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, paramMap).map(extractor).asSingle)
        }

    private fun <T> listQuery(
        @Language("PostgreSQL") query: String,
        paramMap: Map<String, Any> = emptyMap(),
        extractor: (row: Row) -> T,
    ): List<T> =
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, paramMap).map(extractor).asList)
        }

    private fun update(
        @Language("PostgreSQL") query: String,
        paramMap: Map<String, Any> = emptyMap(),
    ) = sessionOf(dataSource).use { session ->
        session.update(
            Query(
                statement = query,
                paramMap = paramMap,
            ),
        )
    }
}
