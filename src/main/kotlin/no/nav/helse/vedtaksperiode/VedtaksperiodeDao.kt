package no.nav.helse.vedtaksperiode

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

class VedtaksperiodeDao(private val dataSource: DataSource) {

    internal fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """
                SELECT *
                FROM vedtak AS v
                         INNER JOIN person AS p ON v.person_ref = p.id
                         INNER JOIN person_info as pi ON pi.id=p.info_ref
                WHERE v.vedtaksperiode_id = ?
                ORDER BY v.id DESC
                LIMIT 1;
            """, vedtaksperiodeId
                )
                    .map(::tilVedtaksperiode)
                    .asSingle
            )
        }

    fun findVedtakByFnr(fnr: String) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                """
                SELECT *
                FROM vedtak AS v
                         INNER JOIN person AS p ON v.person_ref = p.id
                         INNER JOIN person_info as pi ON pi.id=p.info_ref
                WHERE p.fodselsnummer = ?
                ORDER BY v.id DESC
                LIMIT 1;
            """, fnr.toLong()
            )
                .map(::tilVedtaksperiode)
                .asSingle
        )
    }

    fun findVedtakByAktørId(aktørId: String) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                """
                SELECT *
                FROM vedtak AS v
                         INNER JOIN person AS p ON v.person_ref = p.id
                         INNER JOIN person_info AS pi ON pi.id=p.info_ref
                WHERE p.aktor_id = ?
                ORDER BY v.id DESC
                LIMIT 1;
            """, aktørId.toLong()
            )
                .map(::tilVedtaksperiode)
                .asSingle
        )
    }

    private fun tilVedtaksperiode(row: Row) = VedtaksperiodeDto(
        fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
        aktørId = row.long("aktor_id").toString(),
        fornavn = row.string("fornavn"),
        mellomnavn = row.stringOrNull("mellomnavn"),
        etternavn = row.string("etternavn"),
        arbeidsgiverRef = row.long("arbeidsgiver_ref"),
        speilSnapshotRef = row.long("speil_snapshot_ref"),
        infotrygdutbetalingerRef = row.intOrNull("infotrygdutbetalinger_ref")
    )

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
