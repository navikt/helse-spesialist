package no.nav.helse.sidegig

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class MyDao(
    private val dataSource: DataSource,
) {
    fun find10Annulleringer(): List<AnnullertAvSaksbehandlerRow> =
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

    fun findUtbetalingId(
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

    fun finnBehandlingISykefraværstilfelle(utbetalingId: UUID): BehandlingISykefraværstilfelleRow? =
        query(
            query =
                """
                SELECT behandling.id,
                       behandling.vedtaksperiode_id,
                       behandling.skjæringstidspunkt,
                       person.id as person_id,
                       vedtak.arbeidsgiver_identifikator
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
            )
        }

    fun finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandlingISykefraværstilfelleRow: BehandlingISykefraværstilfelleRow): UUID? =
        listQuery(
            query =
                """
                SELECT behandling.vedtaksperiode_id,
                    behandling.fom,
                    behandling.tom
                FROM behandling,
                     vedtak,
                     person
                WHERE person.id = :personId
                  AND vedtak.arbeidsgiver_identifikator = :arbeidsgiverId
                  AND vedtak.person_ref = person.id
                  AND vedtak.vedtaksperiode_id = behandling.vedtaksperiode_id
                ORDER BY behandling.fom 
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
            )
        }.fold(emptyList<BehandlingsperiodeRow>()) { acc, row ->
            if (acc.isEmpty()) {
                acc.plus(row)
            } else if (acc
                    .last()
                    .tom
                    .plusDays(16)
                    .isAfter(row.fom)
            ) {
                acc.plus(row)
            } else {
                acc
            }
        }.firstOrNull()
            ?.vedtaksperiodeId

    data class BehandlingsperiodeRow(
        val vedtaksperiodeId: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    data class AnnullertAvSaksbehandlerRow(
        val id: Int,
        val arbeidsgiver_fagsystem_id: String,
        val person_fagsystem_id: String,
    )

    data class BehandlingISykefraværstilfelleRow(
        val behandlingId: Long,
        val vedtaksperiodeId: UUID,
        val skjæringstidspunkt: LocalDate,
        val personId: Long,
        val arbeidsgiverId: String,
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
}
