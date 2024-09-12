package no.nav.helse.modell.vedtaksperiode

import kotliquery.Query
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.AvslagDao
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class GenerasjonDao(private val dataSource: DataSource) {
    private val avslagDao = AvslagDao(dataSource)

    internal fun TransactionalSession.finnGenerasjoner(vedtaksperiodeId: UUID): List<GenerasjonDto> {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, skjæringstidspunkt, fom, tom, tilstand, tags
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = :vedtaksperiode_id ORDER BY id;
        """
        return run(
            queryOf(
                query,
                mapOf("vedtaksperiode_id" to vedtaksperiodeId),
            ).map { row ->
                val generasjonRef = row.long("id")
                GenerasjonDto(
                    id = row.uuid("unik_id"),
                    vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                    utbetalingId = row.uuidOrNull("utbetaling_id"),
                    spleisBehandlingId = row.uuidOrNull("spleis_behandling_id"),
                    skjæringstidspunkt = row.localDateOrNull("skjæringstidspunkt") ?: row.localDate("fom"),
                    fom = row.localDate("fom"),
                    tom = row.localDate("tom"),
                    tilstand = enumValueOf(row.string("tilstand")),
                    tags = row.array<String>("tags").toList(),
                    varsler = finnVarsler(generasjonRef),
                    avslag =
                        with(avslagDao) {
                            this@finnGenerasjoner.finnAvslag(vedtaksperiodeId, generasjonRef)
                        },
                )
            }.asList,
        )
    }

    internal fun TransactionalSession.lagreGenerasjon(generasjonDto: GenerasjonDto) {
        this.lagre(generasjonDto)
        this.slettVarsler(generasjonDto.id, generasjonDto.varsler.map { it.id })
        generasjonDto.varsler.forEach { varselDto ->
            this.lagre(varselDto, generasjonDto.vedtaksperiodeId, generasjonDto.id)
        }
    }

    private fun TransactionalSession.lagre(generasjonDto: GenerasjonDto) {
        val tags = generasjonDto.tags.joinToString { """ $it """ }

        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, opprettet_tidspunkt, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, fom, tom, skjæringstidspunkt, tilstand, tags) 
            VALUES (:unik_id, :vedtaksperiode_id, :utbetaling_id, :spleis_behandling_id, now(), gen_random_uuid(), now(), gen_random_uuid(), :fom, :tom, :skjaeringstidspunkt, :tilstand::generasjon_tilstand, '{$tags}')
            ON CONFLICT (unik_id) DO UPDATE SET utbetaling_id = excluded.utbetaling_id, spleis_behandling_id = excluded.spleis_behandling_id, fom = excluded.fom, tom = excluded.tom, skjæringstidspunkt = excluded.skjæringstidspunkt, tilstand = excluded.tilstand, tags = excluded.tags
            """.trimIndent()
        this.run(
            queryOf(
                query,
                mapOf(
                    "unik_id" to generasjonDto.id,
                    "vedtaksperiode_id" to generasjonDto.vedtaksperiodeId,
                    "utbetaling_id" to generasjonDto.utbetalingId,
                    "spleis_behandling_id" to generasjonDto.spleisBehandlingId,
                    "fom" to generasjonDto.fom,
                    "tom" to generasjonDto.tom,
                    "skjaeringstidspunkt" to generasjonDto.skjæringstidspunkt,
                    "tilstand" to generasjonDto.tilstand.name,
                ),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.lagre(
        varselDto: VarselDto,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status_endret_ident, status_endret_tidspunkt, status) 
            VALUES (:unik_id, :kode, :vedtaksperiode_id, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id), null, :opprettet, null, null, :status)
            ON CONFLICT (generasjon_ref, kode) DO UPDATE SET status = excluded.status, generasjon_ref = excluded.generasjon_ref
            """.trimIndent()

        this.run(
            queryOf(
                query,
                mapOf(
                    "unik_id" to varselDto.id,
                    "kode" to varselDto.varselkode,
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "generasjon_id" to generasjonId,
                    "opprettet" to varselDto.opprettet,
                    "status" to varselDto.status.name,
                ),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.slettVarsler(
        generasjonId: UUID,
        varselIder: List<UUID>,
    ) {
        @Language("PostgreSQL")
        val query =
            if (varselIder.isEmpty()) {
                """
                DELETE FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? LIMIT 1)
                """.trimIndent()
            } else {
                """
                DELETE FROM selve_varsel 
                WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? LIMIT 1) 
                AND selve_varsel.unik_id NOT IN (${varselIder.joinToString { "?" }})
                """.trimIndent()
            }

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
                    spleisBehandlingId = row.uuidOrNull("spleis_behandling_id"),
                    skjæringstidspunkt = row.localDate("skjæringstidspunkt"),
                    fom = row.localDate("fom"),
                    tom = row.localDate("tom"),
                    tilstand = enumValueOf(row.string("tilstand")),
                    tags = row.array<String>("tags").toList(),
                    varsler = finnVarsler(generasjonRef),
                    avslag = with(avslagDao) { this@finnGenerasjon.finnAvslag(vedtaksperiodeId, generasjonRef) },
                )
            }.asSingle,
        )
    }

    private fun TransactionalSession.finnVarsler(generasjonRef: Long): List<VarselDto> {
        @Language("PostgreSQL")
        val query =
            """
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
                    },
                )
            }.asList,
        )
    }

    private fun finnSiste(vedtaksperiodeId: UUID): Query {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, skjæringstidspunkt, fom, tom, tilstand, tags
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = ? ORDER BY id DESC;
            """
        return queryOf(query, vedtaksperiodeId)
    }

    internal fun TransactionalSession.finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> {
        @Language("PostgreSQL")
        val query = """
            SELECT svg.vedtaksperiode_id FROM selve_vedtaksperiode_generasjon svg 
            INNER JOIN vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE fodselsnummer = ?
            """

        return run(queryOf(query, fødselsnummer.toLong()).map { it.uuid("vedtaksperiode_id") }.asList).toSet()
    }

    internal fun førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? {
        @Language("PostgreSQL")
        val query = """
                SELECT tilstand_endret_tidspunkt 
                FROM selve_vedtaksperiode_generasjon 
                WHERE vedtaksperiode_id = :vedtaksperiodeId AND tilstand = 'VedtakFattet'
                ORDER BY tilstand_endret_tidspunkt
                LIMIT 1
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId,
                    ),
                ).map {
                    it.localDateTimeOrNull("tilstand_endret_tidspunkt")
                }.asSingle,
            )
        }
    }

    internal fun førsteKjenteDag(fødselsnummer: String): LocalDate {
        @Language("PostgreSQL")
        val query =
            """
            select min(svg.fom) as foersteFom
            from selve_vedtaksperiode_generasjon svg
            join vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            join person p on p.id = v.person_ref
            where p.fodselsnummer = :fodselsnummer
            """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf("fodselsnummer" to fødselsnummer.toLong()),
                ).map { it.localDate("foersteFom") }.asSingle,
            ) ?: throw IllegalStateException("Forventet å kunne slå opp første kjente dag")
        }
    }
}
