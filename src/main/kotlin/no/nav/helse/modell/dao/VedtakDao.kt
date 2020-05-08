package no.nav.helse.modell.dao

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

    internal fun findVedtakRef(vedtaksperiodeId: UUID): Long? = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT id FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
            .map { it.long("id") }
            .asSingle)
    }
}
