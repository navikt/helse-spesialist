package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.varsel.Varsel
import org.intellij.lang.annotations.Language

class GenerasjonDao(private val dataSource: DataSource) {

    internal fun finnSisteFor(vedtaksperiodeId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom 
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = ? ORDER BY id DESC;
            """
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map(::toGenerasjon).asSingle)
        }
    }

    internal fun alleFor(utbetalingId: UUID): List<Generasjon> {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom 
            FROM selve_vedtaksperiode_generasjon 
            WHERE utbetaling_id = ?
            """
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, utbetalingId).map(::toGenerasjon).asList)
        }
    }

    internal fun låsFor(generasjonId: UUID, hendelseId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET låst = true, låst_tidspunkt = now(), låst_av_hendelse = ? 
            WHERE unik_id = ?
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom;
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, hendelseId, generasjonId).map(::toGenerasjon).asSingle)
        }
    }

    internal fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET utbetaling_id = ? 
            WHERE unik_id = ?
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom;
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, utbetalingId, generasjonId).map(::toGenerasjon).asSingle)
        }
    }

    internal fun fjernUtbetalingFor(generasjonId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET utbetaling_id = null 
            WHERE unik_id = ?
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom;
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map(::toGenerasjon).asSingle)
        }
    }

    internal fun opprettFor(
        id: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        skjæringstidspunkt: LocalDate?,
        periode: Periode?,
    ): Generasjon {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse, skjæringstidspunkt, fom, tom) 
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom
        """

        @Language("PostgreSQL")
        val søknadMottattQuery = """
            INSERT INTO opprinnelig_soknadsdato 
            SELECT :vedtaksperiodeId, opprettet_tidspunkt
            FROM selve_vedtaksperiode_generasjon
            WHERE vedtaksperiode_id = :vedtaksperiodeId
            ON CONFLICT DO NOTHING;
        """

        return sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                val generasjon = requireNotNull(
                    transactionalSession.run(
                        queryOf(
                            query,
                            id,
                            vedtaksperiodeId,
                            hendelseId,
                            skjæringstidspunkt,
                            periode?.fom(),
                            periode?.tom()
                        ).map(::toGenerasjon).asSingle
                    )
                ) { "Kunne ikke opprette ny generasjon" }
                transactionalSession.run(
                    queryOf(
                        søknadMottattQuery, mapOf(
                            "vedtaksperiodeId" to vedtaksperiodeId,
                            "soknadMottatt" to LocalDateTime.now()
                        )
                    ).asUpdate
                )
                generasjon
            }
        }
    }

    internal fun åpenGenerasjonForVedtaksperiode(vedtaksperiodeId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom 
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = :vedtaksperiode_id AND låst = false
            """
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map(::toGenerasjon).asSingle)
        }
    }

    internal fun oppdaterSykefraværstilfelle(id: UUID, skjæringstidspunkt: LocalDate, periode: Periode) {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET skjæringstidspunkt = :skjaeringstidspunkt, fom = :fom, tom = :tom 
            WHERE unik_id = :unik_id
            """

        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query, mapOf(
                        "unik_id" to id,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                        "fom" to periode.fom(),
                        "tom" to periode.tom(),
                    )
                ).asUpdate
            )
        }
    }

    private fun toGenerasjon(row: Row): Generasjon {
        return Generasjon(
            row.uuid("unik_id"),
            row.uuid("vedtaksperiode_id"),
            row.uuidOrNull("utbetaling_id"),
            row.boolean("låst"),
            row.localDateOrNull("skjæringstidspunkt"),
            row.localDateOrNull("fom")?.let {
                Periode(
                    it,
                    row.localDate("tom"),
                )
            },
            varslerFor(row.long("id")).toSet(),
            dataSource,
        )
    }

    private fun varslerFor(generasjonRef: Long): List<Varsel> {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id, vedtaksperiode_id, kode, opprettet, status FROM selve_varsel WHERE generasjon_ref = ?"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonRef).map {
                Varsel(
                    it.uuid("unik_id"),
                    it.string("kode"),
                    it.localDateTime("opprettet"),
                    it.uuid("vedtaksperiode_id"),
                    enumValueOf(it.string("status"))
                )
            }.asList)
        }
    }
}
