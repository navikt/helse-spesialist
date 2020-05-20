package no.nav.helse.modell.vedtak

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {

    internal fun insertVedtak(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Int,
        arbeidsgiverRef: Int,
        speilSnapshotRef: Int
    ): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
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
        }?.toInt())

    internal fun findVedtak(vedtaksperiodeId: UUID): VedtakDto? = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT * FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
            .map { VedtakDto(
                id = it.long("id"),
                speilSnapshotRef = it.long("speil_snapshot_ref")
            ) }
            .asSingle)
    }

    internal fun deleteVedtak(vedtaksperiodeId: UUID) {
        val vedtak = findVedtak(vedtaksperiodeId)
        using(sessionOf(dataSource)) {session ->
            session.transaction { transaction ->
//                transaction.

            }

        }
    }
}
