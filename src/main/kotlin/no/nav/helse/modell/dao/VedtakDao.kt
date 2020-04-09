package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.dto.VedtakDto
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {

    internal fun findVedtaksperiode(vedtaksperiodeId: UUID): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
                .map { it.int("id") }
                .asSingle
        )
    }

    internal fun findVedtak(id: Long): VedtakDto = using(sessionOf(dataSource)) { session ->
        requireNotNull(session.run(
            queryOf("SELECT * FROM vedtak WHERE id=?;", id)
                .map {
                    VedtakDto(
                        id = it.long("id"),
                        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
                        fom = it.localDate("fom"),
                        tom = it.localDate("fom"),
                        arbeidsgiverRef = it.long("arbeidsgiver_ref"),
                        personRef = it.long("person_ref"),
                        speilSnapshotRef = it.long("speil_snapshot_ref")
                    )
                }
                .asSingle
        ))
    }

    internal fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID): VedtakDto? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT * FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
                    .map {
                        VedtakDto(
                            id = it.long("id"),
                            vedtaksperiodeId = UUID.fromString(it.string("UUID")),
                            fom = it.localDate("fom"),
                            tom = it.localDate("fom"),
                            arbeidsgiverRef = it.long("arbeidsgiver_ref"),
                            personRef = it.long("person_ref"),
                            speilSnapshotRef = it.long("speil_snapshot_ref")
                        )
                    }
                    .asSingle
            )
        }

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
}
