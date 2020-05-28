package no.nav.helse.modell.vedtak

import kotliquery.Session
import kotliquery.queryOf
import java.time.LocalDate
import java.util.*

internal fun Session.insertVedtak(
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
                "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref) VALUES(?, ?, ?, ?, ?, ?);",
                vedtaksperiodeId,
                fom,
                tom,
                personRef,
                arbeidsgiverRef,
                speilSnapshotRef
            ).asUpdateAndReturnGeneratedKey
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
    this.transaction { transaction ->
        transaction.run(queryOf("UPDATE oppgave SET vedtak_ref = null WHERE vedtak_ref = ?", vedtak.id).asUpdate)
        transaction.run(queryOf("DELETE FROM vedtak WHERE id = ?", vedtak.id).asUpdate)
        transaction.run(queryOf("DELETE FROM speil_snapshot WHERE id = ?", vedtak.speilSnapshotRef).asUpdate)
    }
}

