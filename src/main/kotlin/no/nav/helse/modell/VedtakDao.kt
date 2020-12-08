package no.nav.helse.modell

import kotliquery.*
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.vedtak.PersoninfoDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.VedtakDto
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    internal fun findVedtak(id: UUID) = sessionOf(dataSource).use { it.findVedtak(id) }

    internal fun opprett(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Int,
        arbeidsgiverRef: Long,
        speilSnapshotRef: Int
    ) = using(sessionOf(dataSource)) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtak(vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref)
            VALUES (:vedtaksperiode_id, :fom, :tom, :person_ref, :arbeidsgiver_ref, :speil_snapshot_ref)
        """
        it.run(
            queryOf(
                statement, mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "fom" to fom,
                    "tom" to tom,
                    "person_ref" to personRef,
                    "arbeidsgiver_ref" to arbeidsgiverRef,
                    "speil_snapshot_ref" to speilSnapshotRef
                )
            ).asUpdate
        )
    }

    internal fun oppdater(vedtakRef: Long, fom: LocalDate, tom: LocalDate, speilSnapshotRef: Int) =
        using(sessionOf(dataSource)) {
            @Language("PostgreSQL")
            val statement =
                "UPDATE vedtak SET fom=:fom, tom=:tom, speil_snapshot_ref=:speil_snapshot_ref WHERE id=:vedtak_ref"
            it.run(
                queryOf(
                    statement, mapOf(
                        "vedtak_ref" to vedtakRef,
                        "fom" to fom,
                        "tom" to tom,
                        "speil_snapshot_ref" to speilSnapshotRef
                    )
                ).asUpdate
            )
        }

    private fun Session.findVedtak(vedtaksperiodeId: UUID): VedtakDto? {
        @Language("PostgreSQL")
        val statement = "SELECT * FROM vedtak WHERE vedtaksperiode_id=?"
        return this.run(queryOf(statement, vedtaksperiodeId).map {
            VedtakDto(
                id = it.long("id"),
                speilSnapshotRef = it.long("speil_snapshot_ref")
            )
        }.asSingle)
    }

    internal fun opprettKobling(vedtaksperiodeId: UUID, hendelseId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """
        session.run(queryOf(statement, vedtaksperiodeId, hendelseId).asUpdate)
    }

    internal fun fjernKobling(vedtaksperiodeId: UUID, hendelseId: UUID) =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            @Language("PostgreSQL")
            val statement = "DELETE FROM vedtaksperiode_hendelse WHERE hendelse_ref = ? AND vedtaksperiode_id = ?"
            session.run(queryOf(statement, hendelseId, vedtaksperiodeId).asUpdate)
        }

    internal fun finnVedtakId(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)
    }

    internal fun fjernVedtaksperioder(vedtaksperiodeIder: List<UUID>) {
        @Language("PostgreSQL")
        val statement = "DELETE FROM vedtak WHERE vedtaksperiode_id in (${vedtaksperiodeIder.joinToString { "?" }})"
        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, *vedtaksperiodeIder.toTypedArray()).asUpdate)
        }
    }

    internal fun leggTilVedtaksperiodetype(vedtaksperiodeId: UUID, type: Saksbehandleroppgavetype) =
        using(sessionOf(dataSource)) {
            val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return@using

            @Language("PostgreSQL")
            val statement = "INSERT INTO saksbehandleroppgavetype (type, vedtak_ref) VALUES (?, ?)"
            it.run(queryOf(statement, type.name, vedtakRef).asUpdate)
        }

    internal fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Saksbehandleroppgavetype? =
        sessionOf(dataSource).use { session ->
            val vedtakRef =
                requireNotNull(finnVedtakId(vedtaksperiodeId)) { "Finner ikke vedtakRef for $vedtaksperiodeId" }

            @Language("PostgreSQL")
            val statement = "SELECT type FROM saksbehandleroppgavetype where vedtak_ref = ?"
            session.run(queryOf(statement, vedtakRef).map {
                enumValueOf<Saksbehandleroppgavetype>(it.string("type"))
            }.asSingle)
        }

    internal fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) {
        it.findVedtakByVedtaksperiodeId(vedtaksperiodeId)
    }

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

    internal fun oppdaterSnapshot(fødselsnummer: String, snapshot: String) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { tx ->
                val sisteReferanse = insertSpeilSnapshot(tx, snapshot)
                val referanser = findSpeilSnapshotRefs(fødselsnummer)
                oppdaterSnapshotRef(tx, fødselsnummer, sisteReferanse)
                referanser.forEach { ref -> slett(tx, ref) }
            }
        }
    }

    private fun insertSpeilSnapshot(transactionalSession: TransactionalSession, personBlob: String): Long {
        @Language("PostgreSQL")
        val statement = "INSERT INTO speil_snapshot(data) VALUES(CAST(:personBlob as json));"
        return requireNotNull(
            transactionalSession.run(
                queryOf(
                    statement,
                    mapOf("personBlob" to personBlob)
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    private fun slett(session: Session, ref: Long) {
        @Language("PostgreSQL")
        val query = """DELETE FROM speil_snapshot WHERE id = :ref"""
        session.execute(queryOf(query, mapOf("ref" to ref)))
    }

    internal fun erAutomatiskGodkjent(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query =
        """
            SELECT automatisert FROM automatisering WHERE vedtaksperiode_ref = (
                SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId
            )
        """
        session.run(queryOf(query, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map { it.boolean("automatisert") }.asSingle)
    } ?: false

    internal fun findVedtakByFnr(fnr: String) = using(sessionOf(dataSource)) { it.findVedtakByFnr(fnr) }

    private fun findSpeilSnapshotRefs(fnr: String) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val query = """
           SELECT v.speil_snapshot_ref FROM vedtak v join person p on v.person_ref = p.id WHERE p.fodselsnummer = :fnr
        """
        it.run(queryOf(query, mapOf("fnr" to fnr.toLong())).map { it.long("speil_snapshot_ref") }.asList)
    }

    private fun oppdaterSnapshotRef(session: Session, fnr: String, ref: Long) {
        @Language("PostgreSQL")
        val query = """
           UPDATE vedtak SET speil_snapshot_ref = :ref WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :fnr)
        """
        session.execute(
            queryOf(
                query,
                mapOf(
                    "fnr" to fnr.toLong(),
                    "ref" to ref
                )
            )
        )
    }

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
