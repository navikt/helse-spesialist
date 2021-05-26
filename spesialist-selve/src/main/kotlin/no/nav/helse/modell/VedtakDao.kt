package no.nav.helse.modell

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
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
    ) = sessionOf(dataSource).use  { session ->
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
        sessionOf(dataSource).use  { session ->
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

    internal fun opprettKobling(vedtaksperiodeId: UUID, hendelseId: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """
        session.run(queryOf(statement, vedtaksperiodeId, hendelseId).asUpdate)
    }

    internal fun fjernKobling(vedtaksperiodeId: UUID, hendelseId: UUID) =

        sessionOf(dataSource, returnGeneratedKey = true).use  { session ->
            @Language("PostgreSQL")
            val statement = "DELETE FROM vedtaksperiode_hendelse WHERE hendelse_ref = ? AND vedtaksperiode_id = ?"
            session.run(queryOf(statement, hendelseId, vedtaksperiodeId).asUpdate)
        }

    internal fun finnVedtakId(vedtaksperiodeId: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)
    }

    internal fun leggTilVedtaksperiodetype(vedtaksperiodeId: UUID, type: Periodetype, inntektskilde: Inntektskilde) =
        sessionOf(dataSource).use  { session ->
            val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return@use

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

    internal fun erAutomatiskGodkjent(utbetalingId: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val query = "SELECT automatisert FROM automatisering WHERE utbetaling_id = ?;"
        session.run(queryOf(query, utbetalingId).map { it.boolean("automatisert") }.asSingle)
    } ?: false
}
