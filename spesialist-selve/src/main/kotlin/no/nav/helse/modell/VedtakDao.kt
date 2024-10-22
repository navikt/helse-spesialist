package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.TransactionalVedtakDao
import no.nav.helse.db.VedtakRepository
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) : VedtakRepository {
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
            ORDER BY opprettet_tidspunkt LIMIT 1
            ON CONFLICT DO NOTHING;
            """.trimIndent()
        run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).asUpdate)
    }

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalVedtakDao(session).opprettKobling(vedtaksperiodeId, hendelseId)
        }
    }

    override fun fjernKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalVedtakDao(session).fjernKobling(vedtaksperiodeId, hendelseId)
        }
    }

    internal fun finnVedtakId(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
            session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)
        }

    override fun erSpesialsak(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalVedtakDao(session).erSpesialsak(vedtaksperiodeId)
        }

    internal fun spesialsakFerdigbehandlet(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val query = """UPDATE spesialsak SET ferdigbehandlet = true WHERE vedtaksperiode_id = :vedtaksperiode_id"""
            it.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).asUpdate)
        }

    override fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    ) = sessionOf(dataSource).use { session ->
        TransactionalVedtakDao(session).leggTilVedtaksperiodetype(vedtaksperiodeId, type, inntektskilde)
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

    override fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? =
        sessionOf(dataSource).use { session ->
            TransactionalVedtakDao(session).finnInntektskilde(vedtaksperiodeId)
        }

    override fun erAutomatiskGodkjent(utbetalingId: UUID) =
        sessionOf(dataSource).use { TransactionalVedtakDao(it).erAutomatiskGodkjent(utbetalingId) }

    override fun finnOrgnummer(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalVedtakDao(session).finnOrgnummer(vedtaksperiodeId)
        }
}
