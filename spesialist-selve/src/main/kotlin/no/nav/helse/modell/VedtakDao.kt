package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    internal fun TransactionalSession.finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto? {
        @Language("PostgreSQL")
        val query =
            """
            SELECT 
            vedtaksperiode_id,
            (SELECT orgnummer FROM arbeidsgiver WHERE id = arbeidsgiver_ref),
            forkastet
            from vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id
            """.trimIndent()

        return run(
            queryOf(
                query,
                mapOf("vedtaksperiode_id" to vedtaksperiodeId),
            ).map {
                VedtaksperiodeDto(
                    organisasjonsnummer = it.long("orgnummer").toString(),
                    vedtaksperiodeId = it.uuid("vedtaksperiode_id"),
                    forkastet = it.boolean("forkastet"),
                    generasjoner = emptyList(),
                )
            }.asSingle,
        )
    }

    internal fun TransactionalSession.lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeDto: VedtaksperiodeDto,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet)
            VALUES (:vedtaksperiode_id, :fom, :tom, (SELECT id FROM arbeidsgiver WHERE orgnummer = :organisasjonsnummer), (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer), :forkastet)
            ON CONFLICT (vedtaksperiode_id) DO UPDATE SET forkastet = excluded.forkastet
            """.trimIndent()

        this.run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "organisasjonsnummer" to vedtaksperiodeDto.organisasjonsnummer.toLong(),
                    "vedtaksperiode_id" to vedtaksperiodeDto.vedtaksperiodeId,
                    "fom" to vedtaksperiodeDto.generasjoner.last().fom,
                    "tom" to vedtaksperiodeDto.generasjoner.last().tom,
                    "forkastet" to vedtaksperiodeDto.forkastet,
                ),
            ).asUpdate,
        )
    }

    internal fun TransactionalSession.lagreOpprinneligSøknadsdato(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO opprinnelig_soknadsdato 
            SELECT vedtaksperiode_id, opprettet_tidspunkt
            FROM selve_vedtaksperiode_generasjon
            WHERE vedtaksperiode_id = :vedtaksperiode_id
            ORDER BY opprettet_tidspunkt ASC LIMIT 1
            ON CONFLICT DO NOTHING;
            """.trimIndent()
        run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).asUpdate)
    }

    internal fun opprett(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Long,
        arbeidsgiverRef: Long,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO vedtak(vedtaksperiode_id, fom, tom, person_ref, arbeidsgiver_ref, forkastet)
            VALUES (:vedtaksperiode_id, :fom, :tom, :person_ref, :arbeidsgiver_ref, false);
        """
        session.run(
            queryOf(
                query,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "fom" to fom,
                    "tom" to tom,
                    "person_ref" to personRef,
                    "arbeidsgiver_ref" to arbeidsgiverRef,
                ),
            ).asUpdate,
        )
    }

    internal fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """
        session.run(queryOf(statement, vedtaksperiodeId, hendelseId).asUpdate)
    }

    internal fun fjernKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val statement = "DELETE FROM vedtaksperiode_hendelse WHERE hendelse_ref = ? AND vedtaksperiode_id = ?"
        session.run(queryOf(statement, hendelseId, vedtaksperiodeId).asUpdate)
    }

    internal fun finnVedtakId(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
            session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)
        }

    internal fun erSpesialsak(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = """SELECT true FROM spesialsak WHERE vedtaksperiode_id = :vedtaksperiode_id AND ferdigbehandlet = false"""
            it.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.boolean(1) }.asSingle) ?: false
        }

    internal fun spesialsakFerdigbehandlet(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = """UPDATE spesialsak SET ferdigbehandlet = true WHERE vedtaksperiode_id = :vedtaksperiode_id"""
            it.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).asUpdate)
        }

    internal fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    ) = sessionOf(dataSource).use { session ->
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return@use

        @Language("PostgreSQL")
        val statement = """
                INSERT INTO saksbehandleroppgavetype (type, inntektskilde, vedtak_ref) VALUES (:type, :inntektskilde, :vedtak_ref)
                ON CONFLICT (vedtak_ref) DO UPDATE SET type = :type, inntektskilde = :inntektskilde
            """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "type" to type.name,
                    "inntektskilde" to inntektskilde.name,
                    "vedtak_ref" to vedtakRef,
                ),
            ).asUpdate,
        )
    }

    internal fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype =
        sessionOf(dataSource).use { session ->
            val vedtakRef =
                checkNotNull(finnVedtakId(vedtaksperiodeId)) { "Finner ikke vedtakRef for $vedtaksperiodeId" }

            @Language("PostgreSQL")
            val statement = "SELECT type FROM saksbehandleroppgavetype WHERE vedtak_ref = ?"
            checkNotNull(
                session.run(
                    queryOf(statement, vedtakRef).map {
                        enumValueOf<Periodetype>(it.string("type"))
                    }.asSingle,
                ),
            ) { "Forventet å finne saksbehandleroppgavetype for vedtaksperiodeId $vedtaksperiodeId" }
        }

    internal fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                "SELECT inntektskilde FROM saksbehandleroppgavetype WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)"
            session.run(
                queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map {
                    enumValueOf<Inntektskilde>(it.string("inntektskilde"))
                }.asSingle,
            )
        }

    internal fun erAutomatiskGodkjent(utbetalingId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
            SELECT automatisert FROM automatisering 
            WHERE utbetaling_id = ?
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
        """
            session.run(queryOf(query, utbetalingId).map { it.boolean("automatisert") }.asSingle)
        } ?: false

    internal fun markerForkastet(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "UPDATE vedtak SET forkastet = true, forkastet_av_hendelse = :hendelseId, forkastet_tidspunkt = now() WHERE vedtaksperiode_id = :vedtaksperiodeId"
        session.run(
            queryOf(
                query,
                mapOf(
                    "hendelseId" to hendelseId,
                    "vedtaksperiodeId" to vedtaksperiodeId,
                ),
            ).asUpdate,
        )
    }

    internal fun finnOrgnummer(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
            SELECT orgnummer FROM arbeidsgiver a
            INNER JOIN vedtak v ON a.id = v.arbeidsgiver_ref
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
        """
            session.run(queryOf(query, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map { it.long("orgnummer").toString() }.asSingle)
        }
}
