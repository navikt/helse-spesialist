package no.nav.helse.vedtaksperiode

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.PersoninfoDto
import java.util.*

internal fun Session.findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) = this.run(
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

internal fun Session.findVedtakByFnr(fnr: String) = this.run(
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

internal fun Session.findVedtakByAktørId(aktørId: String) = this.run(
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

private fun tilVedtaksperiode(row: Row) = VedtaksperiodeDto(
    fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
    aktørId = row.long("aktor_id").toString(),
    personinfo = PersoninfoDto(
        fornavn = row.string("fornavn"),
        mellomnavn = row.stringOrNull("mellomnavn"),
        etternavn = row.string("etternavn"),
        fødselsdato = row.localDateOrNull("fodselsdato"),
        kjønn = row.stringOrNull("kjonn")?.let(Kjønn::valueOf)
    ),
    arbeidsgiverRef = row.int("arbeidsgiver_ref"),
    speilSnapshotRef = row.int("speil_snapshot_ref"),
    infotrygdutbetalingerRef = row.intOrNull("infotrygdutbetalinger_ref")
)

private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
