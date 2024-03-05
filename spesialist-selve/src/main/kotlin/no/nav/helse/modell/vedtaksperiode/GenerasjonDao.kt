package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Query
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselDto
import no.nav.helse.modell.varsel.VarselStatusDto
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

    internal fun lagre(generasjonDto: GenerasjonDto) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(generasjonDto)
                generasjonDto.varsler.forEach { varselDto ->
                    tx.lagre(varselDto, generasjonDto.id)
                }
                tx.slettVarsler(generasjonDto.id, generasjonDto.varsler.map { it.id })
            }
        }
    }

    private fun TransactionalSession.lagre(generasjonDto: GenerasjonDto) {
        @Language("PostgreSQL")
        val query =
            """
                INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, fom, tom, skjæringstidspunkt, tilstand) 
                VALUES (:unik_id, :vedtaksperiode_id, :utbetaling_id, now(), gen_random_uuid(), now(), gen_random_uuid(), :fom, :tom, :skjaeringstidspunkt, :tilstand)
                ON CONFLICT (unik_id) DO UPDATE SET utbetaling_id = excluded.utbetaling_id, fom = excluded.fom, tom = excluded.tom, skjæringstidspunkt = excluded.skjæringstidspunkt, tilstand = excluded.tilstand
                """.trimIndent()
        this.run(
            queryOf(
                query,
                mapOf(
                    "unik_id" to generasjonDto.id,
                    "vedtaksperiode_id" to generasjonDto.vedtaksperiodeId,
                    "utbetaling_id" to generasjonDto.utbetalingId,
                    "fom" to generasjonDto.fom,
                    "tom" to generasjonDto.tom,
                    "skjaeringstidspunkt" to generasjonDto.skjæringstidspunkt,
                    "tilstand" to generasjonDto.tilstand.name
                )
            ).asUpdate
        )
    }

    private fun TransactionalSession.lagre(varselDto: VarselDto, generasjonId: UUID) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status_endret_ident, status_endret_tidspunkt, status) 
            VALUES (:unik_id, :kode, :vedtaksperiode_id, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id), null, :opprettet, null, null, :status)
            ON CONFLICT (generasjon_ref, kode) DO UPDATE SET status = excluded.status
        """.trimIndent()

        this.run(
            queryOf(
                query,
                mapOf(
                    "unik_id" to varselDto.id,
                    "kode" to varselDto.varselkode,
                    "vedtaksperiode_id" to varselDto.vedtaksperiodeId,
                    "generasjon_id" to generasjonId,
                    "opprettet" to varselDto.opprettet,
                    "status" to varselDto.status.name
                )
            ).asUpdate
        )
    }

    private fun TransactionalSession.slettVarsler(generasjonId: UUID, varselIder: List<UUID>) {
        @Language("PostgreSQL")
        val query = if (varselIder.isEmpty()) """
            DELETE FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? LIMIT 1)
        """.trimIndent()
        else """
            DELETE FROM selve_varsel 
            WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? LIMIT 1) 
            AND selve_varsel.unik_id NOT IN (${varselIder.joinToString { "?" }})
        """.trimIndent()

        this.run(queryOf(query, generasjonId, *varselIder.toTypedArray()).asExecute)
    }

    internal fun finnGjeldendeGenerasjon(vedtaksperiodeId: UUID): GenerasjonDto? {
        return sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.finnGenerasjon(vedtaksperiodeId)
            }
        }
    }

    private fun TransactionalSession.finnGenerasjon(vedtaksperiodeId: UUID): GenerasjonDto? {
        return run(
            finnSiste(vedtaksperiodeId).map { row ->
                val generasjonRef = row.long("id")
                GenerasjonDto(
                    id = row.uuid("unik_id"),
                    vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                    utbetalingId = row.uuidOrNull("utbetaling_id"),
                    skjæringstidspunkt = row.localDate("skjæringstidspunkt"),
                    fom = row.localDate("fom"),
                    tom = row.localDate("tom"),
                    tilstand = when (val tilstand = row.string("tilstand")) {
                        "Låst" -> TilstandDto.Låst
                        "Ulåst" -> TilstandDto.Ulåst
                        "AvsluttetUtenUtbetaling" -> TilstandDto.AvsluttetUtenUtbetaling
                        "UtenUtbetalingMåVurderes" -> TilstandDto.UtenUtbetalingMåVurderes
                        else -> throw IllegalArgumentException("$tilstand er ikke en gyldig generasjontilstand")
                    },
                    varsler = finnVarsler(generasjonRef)
                )
            }.asSingle
        )
    }

    private fun TransactionalSession.finnVarsler(generasjonRef: Long): List<VarselDto> {
        @Language("PostgreSQL")
        val query = """
            SELECT 
            unik_id, 
            kode, 
            vedtaksperiode_id, 
            opprettet, 
            status 
            FROM selve_varsel sv WHERE generasjon_ref = :generasjon_ref
        """.trimIndent()
        return this.run(
                queryOf(
                    query,
                    mapOf("generasjon_ref" to generasjonRef),
                ).map { row ->
                    VarselDto(
                        row.uuid("unik_id"),
                        row.string("kode"),
                        row.localDateTime("opprettet"),
                        row.uuid("vedtaksperiode_id"),
                        when (val status = row.string("status")) {
                            "AKTIV" -> VarselStatusDto.AKTIV
                            "INAKTIV" -> VarselStatusDto.INAKTIV
                            "GODKJENT" -> VarselStatusDto.GODKJENT
                            "VURDERT" -> VarselStatusDto.VURDERT
                            "AVVIST" -> VarselStatusDto.AVVIST
                            "AVVIKLET" -> VarselStatusDto.AVVIKLET
                            else -> throw IllegalArgumentException("$status er ikke en gyldig varselstatus")
                        }
                    )
                }.asList
            )
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

    internal fun førsteKjenteDag(fødselsnummer: String): LocalDate {
        @Language("PostgreSQL") val query = """
            select min(svg.fom) as foersteFom
            from selve_vedtaksperiode_generasjon svg
            join vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            join person p on p.id = v.person_ref
            where p.fodselsnummer = :fodselsnummer
        """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query, mapOf("fodselsnummer" to fødselsnummer.toLong())
                ).map { it.localDate("foersteFom") }.asSingle
            ) ?: throw IllegalStateException("Forventet å kunne slå opp første kjente dag")
        }
    }
}
