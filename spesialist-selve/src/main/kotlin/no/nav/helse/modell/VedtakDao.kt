package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.*
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import no.nav.helse.person.Kjønn
import no.nav.helse.person.PersoninfoApiDto
import no.nav.helse.vedtaksperiode.VedtaksperiodeApiDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    internal fun opprett(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Long,
        arbeidsgiverRef: Long,
        speilSnapshotRef: Int
    ) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO vedtak(vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, speil_snapshot_ref)
            VALUES (:vedtaksperiode_id, :fom, :tom, :person_ref, :arbeidsgiver_ref, :speil_snapshot_ref);
        """
        session.run(
            queryOf(
                query, mapOf(
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
        using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val query = """
                UPDATE vedtak
                SET fom = :fom, tom = :tom, speil_snapshot_ref = :speil_snapshot_ref
                WHERE id = :vedtak_ref
            """
            session.run(
                queryOf(
                    query, mapOf(
                        "vedtak_ref" to vedtakRef,
                        "fom" to fom,
                        "tom" to tom,
                        "speil_snapshot_ref" to speilSnapshotRef
                    )
                ).asUpdate
            )
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

    internal fun leggTilVedtaksperiodetype(vedtaksperiodeId: UUID, type: Periodetype, inntektskilde: Inntektskilde) =
        using(sessionOf(dataSource)) { session ->
            val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return@using

            @Language("PostgreSQL")
            val statement = "INSERT INTO saksbehandleroppgavetype (type, inntektskilde, vedtak_ref) VALUES (?, ?, ?)"
            session.run(queryOf(statement, type.name, inntektskilde.name, vedtakRef).asUpdate)
        }

    internal fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype? =
        sessionOf(dataSource).use { session ->
            val vedtakRef =
                requireNotNull(finnVedtakId(vedtaksperiodeId)) { "Finner ikke vedtakRef for $vedtaksperiodeId" }

            @Language("PostgreSQL")
            val statement = "SELECT type FROM saksbehandleroppgavetype where vedtak_ref = ?"
            session.run(queryOf(statement, vedtakRef).map {
                enumValueOf<Periodetype>(it.string("type"))
            }.asSingle)
        }

    internal fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? =
        sessionOf(dataSource).use { session ->
            val vedtakRef =
                requireNotNull(finnVedtakId(vedtaksperiodeId)) { "Finner ikke vedtakRef for $vedtaksperiodeId" }

            @Language("PostgreSQL")
            val statement = "SELECT inntektskilde FROM saksbehandleroppgavetype where vedtak_ref = ?"
            session.run(queryOf(statement, vedtakRef).map {
                enumValueOf<Inntektskilde>(it.string("inntektskilde"))
            }.asSingle)
        }

    internal fun oppdaterSnapshot(fødselsnummer: String, snapshot: String) {
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.transaction { tx ->
                val sisteReferanse = insertSpeilSnapshot(tx, snapshot)
                val referanser = findSpeilSnapshotRefs(fødselsnummer)
                oppdaterSnapshotRef(tx, fødselsnummer, sisteReferanse)
                referanser.forEach { ref -> slett(tx, ref) }
            }
        }
    }

    internal fun erAutomatiskGodkjent(utbetalingId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = "SELECT automatisert FROM automatisering WHERE utbetaling_id = ?;"
        session.run(queryOf(query, utbetalingId).map { it.boolean("automatisert") }.asSingle)
    } ?: false

    internal fun findVedtakByFnr(fnr: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                INNER JOIN person AS p ON v.person_ref = p.id
                INNER JOIN person_info as pi ON pi.id=p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE p.fodselsnummer = ? ORDER BY v.id DESC LIMIT 1;
        """
        session.run(queryOf(query, fnr.toLong()).map(::tilVedtaksperiode).asSingle)
    }

    internal fun findVedtakByAktørId(aktørId: String) = using(sessionOf(dataSource)) {
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                INNER JOIN person AS p ON v.person_ref = p.id
                INNER JOIN person_info AS pi ON pi.id=p.info_ref
                INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE p.aktor_id = ? ORDER BY v.id DESC LIMIT 1;
        """
        it.run(queryOf(query, aktørId.toLong()).map(::tilVedtaksperiode).asSingle)
    }

    internal fun findVedtakByVedtaksperiodeId(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM vedtak AS v
                 INNER JOIN person AS p ON v.person_ref = p.id
                 INNER JOIN person_info as pi ON pi.id=p.info_ref
                 INNER JOIN speil_snapshot AS ss ON ss.id = v.speil_snapshot_ref
            WHERE v.vedtaksperiode_id = ? ORDER BY v.id DESC LIMIT 1;
        """
        session.run(queryOf(query, vedtaksperiodeId).map(::tilVedtaksperiode).asSingle)
    }

    private fun findSpeilSnapshotRefs(fnr: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT v.speil_snapshot_ref FROM vedtak v
                JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = ?;
        """
        session.run(queryOf(query, fnr.toLong()).map { it.long("speil_snapshot_ref") }.asList)
    }

    private fun oppdaterSnapshotRef(session: Session, fnr: String, ref: Long) {
        @Language("PostgreSQL")
        val query = """
           UPDATE vedtak SET speil_snapshot_ref = ?
           WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = ?)
        """
        session.execute(queryOf(query, ref, fnr.toLong()))
    }

    private fun insertSpeilSnapshot(transactionalSession: TransactionalSession, personBlob: String): Long {
        @Language("PostgreSQL")
        val statement = "INSERT INTO speil_snapshot(data) VALUES(CAST(? as json));"
        return requireNotNull(transactionalSession.run(queryOf(statement, personBlob).asUpdateAndReturnGeneratedKey))
    }

    private fun slett(session: Session, ref: Long) {
        @Language("PostgreSQL")
        val query = "DELETE FROM speil_snapshot WHERE id = ?"
        session.execute(queryOf(query, ref))
    }

    private fun tilVedtaksperiode(row: Row): Pair<VedtaksperiodeApiDto, PersonFraSpleisDto> {
        val vedtak = VedtaksperiodeApiDto(
            fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
            aktørId = row.long("aktor_id").toString(),
            personinfo = PersoninfoApiDto(
                fornavn = row.string("fornavn"),
                mellomnavn = row.stringOrNull("mellomnavn"),
                etternavn = row.string("etternavn"),
                fødselsdato = row.localDateOrNull("fodselsdato"),
                kjønn = row.stringOrNull("kjonn")?.let(Kjønn::valueOf)
            ),
            arbeidsgiverRef = row.long("arbeidsgiver_ref"),
            speilSnapshotRef = row.int("speil_snapshot_ref"),
            infotrygdutbetalingerRef = row.intOrNull("infotrygdutbetalinger_ref")
        )
        val snapshot = objectMapper.readValue<PersonFraSpleisDto>(row.string("data"))
        return vedtak to snapshot
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
