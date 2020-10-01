package no.nav.helse.modell

import kotliquery.*
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.PersoninfoDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.findVedtak
import no.nav.helse.modell.vedtak.upsertVedtak
import no.nav.helse.vedtaksperiode.VedtaksperiodeDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    internal fun findVedtak(id: UUID) = sessionOf(dataSource).use { it.findVedtak(id) }

    internal fun upsertVedtak(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Int,
        arbeidsgiverRef: Int,
        speilSnapshotRef: Int
    ) = using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.upsertVedtak(vedtaksperiodeId, fom, tom, personRef, arbeidsgiverRef, speilSnapshotRef)
    }

    internal fun opprettKobling(vedtaksperiodeId: UUID, hendelseId: UUID) = using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_hendelse
            SELECT id, :hendelse_id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id
            ON CONFLICT DO NOTHING
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_id" to hendelseId
                )
            ).asUpdate
        )
    }

    internal fun fjernKobling(vedtaksperiodeId: UUID, hendelseId: UUID) = using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
        @Language("PostgreSQL")
        val statement = """
            DELETE FROM vedtaksperiode_hendelse
            WHERE hendelse_ref = :hendelse_id AND vedtaksperiode_ref = (
                SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_id" to hendelseId
                )
            ).asUpdate
        )
    }

    internal fun leggTilWarnings(vedtaksperiodeId: UUID, meldinger: List<String>) = using(sessionOf(dataSource)) { session ->
        meldinger.forEach { melding -> session.insertWarning(melding, vedtaksperiodeId) }
    }

    private fun Session.insertWarning(melding: String, vedtaksperiodeId: UUID) = this.run(
        queryOf(
            "INSERT INTO warning (melding, vedtak_ref) VALUES (?, (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?))",
            melding,
            vedtaksperiodeId
        ).asUpdate
    )

    internal fun fjernVedtaksperioder(vedtaksperiodeIder: List<UUID>) {
        @Language("PostgreSQL")
        val statement = """
            DELETE FROM vedtak WHERE vedtaksperiode_id in (${vedtaksperiodeIder.joinToString { "?" }})
        """

        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, *vedtaksperiodeIder.toTypedArray()).asUpdate)
        }
    }

    internal fun leggTilVedtaksperiodetype(vedtaksperiodeId: UUID, type: Saksbehandleroppgavetype) =
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO saksbehandleroppgavetype (type, vedtak_ref) VALUES (?, (SELECT id FROM vedtak WHERE vedtaksperiode_id = ? LIMIT 1))",
                    type.name,
                    vedtaksperiodeId
                ).asUpdate
            )
        }

    internal fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { it.findVedtakByVedtaksperiodeId(vedtaksperiodeId) }

    private fun Session.findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) = this.run(
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

    internal fun findVedtakByFnr(fnr: String) = using(sessionOf(dataSource)) { it.findVedtakByFnr(fnr) }

    private fun Session.findVedtakByFnr(fnr: String) = this.run(
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

    internal fun findVedtakByAktørId(aktørId: String) = using(sessionOf(dataSource)) { it.findVedtakByAktørId(aktørId) }

    private fun Session.findVedtakByAktørId(aktørId: String) = this.run(
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
}
