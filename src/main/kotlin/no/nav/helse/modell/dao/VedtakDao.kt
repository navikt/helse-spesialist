package no.nav.helse.modell.dao

import kotliquery.Row
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
                .map (::vedtakDto)
                .asSingle
        ))
    }

    internal fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID): VedtakDto? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT * FROM vedtak WHERE vedtaksperiode_id=?;", vedtaksperiodeId)
                    .map (::vedtakDto)
                    .asSingle
            )
        }
    internal fun findVedtakByPersonRef(personRef: Int): VedtakDto? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf("SELECT * FROM vedtak WHERE person_ref=?;", personRef)
                    .map (::vedtakDto)
                    .asSingle
            )
        }

    private fun vedtakDto(row: Row): VedtakDto {
        return VedtakDto(
            id = row.long("id"),
            vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
            fom = row.localDate("fom"),
            tom = row.localDate("fom"),
            arbeidsgiverRef = row.long("arbeidsgiver_ref"),
            personRef = row.long("person_ref"),
            speilSnapshotRef = row.long("speil_snapshot_ref")
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
