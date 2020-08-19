package no.nav.helse.modell.vedtak

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

@Language("PostgreSQL")
val upsertVedtakQuery = """INSERT INTO vedtak(vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref)
VALUES (:vedtaksperiode_id, :fom, :tom, :person_ref, :arbeidsgiver_ref, :speil_snapshot_ref)
ON CONFLICT (vedtaksperiode_id)
DO UPDATE SET speil_snapshot_ref = :speil_snapshot_ref, fom = :fom, tom = :tom
"""

internal fun Session.upsertVedtak(
    vedtaksperiodeId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    personRef: Int,
    arbeidsgiverRef: Int,
    speilSnapshotRef: Int
): Int =
    requireNotNull(
        this.run(
            queryOf(
                upsertVedtakQuery,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "fom" to fom,
                    "tom" to tom,
                    "person_ref" to personRef,
                    "arbeidsgiver_ref" to arbeidsgiverRef,
                    "speil_snapshot_ref" to speilSnapshotRef
                )
            ).asUpdate
        )
    ).toInt()

internal fun Session.findVedtak(vedtaksperiodeId: UUID): VedtakDto? =
    this.run(
        queryOf("SELECT * FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
            .map {
                VedtakDto(
                    id = it.long("id"),
                    speilSnapshotRef = it.long("speil_snapshot_ref")
                )
            }.asSingle
    )

internal fun Session.deleteVedtak(vedtaksperiodeId: UUID) {
    val vedtak = findVedtak(vedtaksperiodeId) ?: return
    run(queryOf("UPDATE oppgave SET vedtak_ref = null WHERE vedtak_ref = ?", vedtak.id).asUpdate)
    run(queryOf("DELETE FROM vedtak WHERE id = ?", vedtak.id).asUpdate)
    run(queryOf("DELETE FROM speil_snapshot WHERE id = ?", vedtak.speilSnapshotRef).asUpdate)
}

