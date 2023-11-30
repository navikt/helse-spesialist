package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.varsel.Varsel
import org.intellij.lang.annotations.Language

class GenerasjonDao(private val dataSource: DataSource) {

    internal fun byggSisteFor(vedtaksperiodeId: UUID, generasjonBuilder: GenerasjonBuilder) {
        @Language("PostgreSQL")
        val query = """
            SELECT DISTINCT ON (vedtaksperiode_id) id, vedtaksperiode_id, unik_id, utbetaling_id, skjæringstidspunkt, fom, tom, tilstand
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = ? ORDER BY vedtaksperiode_id, id DESC;
            """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { row ->
                generasjonBuilder.generasjonId(row.uuid("unik_id"))
                row.uuidOrNull("utbetaling_id")?.let(generasjonBuilder::utbetalingId)
                generasjonBuilder.skjæringstidspunkt(row.localDate("skjæringstidspunkt"))
                generasjonBuilder.tilstand(mapToTilstand(row.string("tilstand")))
                generasjonBuilder.periode(row.localDate("fom"), row.localDate("tom"))
            }.asSingle)
        }
    }

    internal fun finnSkjæringstidspunktFor(vedtaksperiodeId: UUID): LocalDate? {
        return sessionOf(dataSource).use { session ->
            session.run(finnSiste(vedtaksperiodeId).map { it.localDate("skjæringstidspunkt") }.asSingle)
        }
    }

    internal fun finnSisteGenerasjonFor(vedtaksperiodeId: UUID): UUID? {
        return sessionOf(dataSource).use { session ->
            session.run(finnSiste(vedtaksperiodeId).map { it.uuid("unik_id") }.asSingle)
        }
    }

    internal fun harGenerasjonFor(vedtaksperiodeId: UUID): Boolean = finnSisteGenerasjonFor(vedtaksperiodeId) != null

    private fun finnSiste(vedtaksperiodeId: UUID): Query {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, skjæringstidspunkt, fom, tom, tilstand
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = ? ORDER BY id DESC;
            """
        return queryOf(query, vedtaksperiodeId)
    }

    internal fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_vedtaksperiode_generasjon 
            SET utbetaling_id = ? 
            WHERE unik_id = ?
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, skjæringstidspunkt, fom, tom, tilstand;
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
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, skjæringstidspunkt, fom, tom, tilstand;
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map(::toGenerasjon).asSingle)
        }
    }

    internal fun finnVedtaksperiodeIderFor(utbetalingId: UUID): Set<UUID> {
        @Language("PostgreSQL")
        val query = """SELECT vedtaksperiode_id FROM selve_vedtaksperiode_generasjon WHERE utbetaling_id = ?"""
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, utbetalingId).map { it.uuid("vedtaksperiode_id") }.asList).toSet()
        }
    }

    internal fun finnVedtaksperiodeIderFor(fødselsnummer: String, skjæringstidspunkt: LocalDate): Set<UUID> {
        @Language("PostgreSQL")
        val query = """
            SELECT svg.vedtaksperiode_id FROM selve_vedtaksperiode_generasjon svg 
            INNER JOIN vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE fodselsnummer = ? AND svg.skjæringstidspunkt = ? AND forkastet = false
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong(), skjæringstidspunkt).map { it.uuid("vedtaksperiode_id") }.asList).toSet()
        }
    }


    internal fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> {
        @Language("PostgreSQL")
        val query = """
            SELECT svg.vedtaksperiode_id FROM selve_vedtaksperiode_generasjon svg 
            INNER JOIN vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE fodselsnummer = ? AND forkastet = false
            """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong()).map { it.uuid("vedtaksperiode_id") }.asList).toSet()
        }
    }

    internal fun opprettFor(
        id: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        skjæringstidspunkt: LocalDate,
        periode: Periode,
        tilstand: Generasjon.Tilstand,
    ): Generasjon {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, opprettet_av_hendelse, skjæringstidspunkt, fom, tom, tilstand) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, unik_id, vedtaksperiode_id, utbetaling_id, skjæringstidspunkt, fom, tom, tilstand
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
                            periode.fom(),
                            periode.tom(),
                            tilstand.navn()
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
        return Generasjon.fraLagring(
            row.uuid("unik_id"),
            row.uuid("vedtaksperiode_id"),
            row.uuidOrNull("utbetaling_id"),
            row.localDate("skjæringstidspunkt"),
            row.localDate("fom"),
            row.localDate("tom"),
            mapToTilstand(row.string("tilstand")),
            varslerFor(row.long("id")).toSet(),
        )
    }

    private fun mapToTilstand(tilstand: String): Generasjon.Tilstand {
        val tilstandKlasser = Generasjon.Tilstand::class.sealedSubclasses
        val tilstander = tilstandKlasser.mapNotNull { it.objectInstance }.associateBy { it.navn() }
        return tilstander.getValue(tilstand)
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

    internal fun oppdaterTilstandFor(generasjonId: UUID, ny: Generasjon.Tilstand, endretAv: UUID) {
        @Language("PostgreSQL")
        val query = """
                UPDATE selve_vedtaksperiode_generasjon 
                SET tilstand = :tilstand, tilstand_endret_tidspunkt = :endret_tidspunkt, tilstand_endret_av_hendelse = :endret_av_hendelse 
                WHERE unik_id = :generasjon_id
            """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "tilstand" to ny.navn(),
                        "endret_tidspunkt" to LocalDateTime.now(),
                        "endret_av_hendelse" to endretAv,
                        "generasjon_id" to generasjonId
                    )
                ).asUpdate
            )
        }
    }

    internal fun førsteGenerasjonLåstTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? {
        @Language("PostgreSQL")
        val query = """
                SELECT tilstand_endret_tidspunkt 
                FROM selve_vedtaksperiode_generasjon 
                WHERE vedtaksperiode_id = :vedtaksperiodeId AND tilstand = 'Låst'
                ORDER BY tilstand_endret_tidspunkt
                LIMIT 1
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map {
                    it.localDateTimeOrNull("tilstand_endret_tidspunkt")
                }.asSingle
            )
        }
    }
}
