package no.nav.helse.modell.vedtaksperiode

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.db.AvslagDao
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgGenerasjonDao private constructor(private val queryRunner: QueryRunner) : GenerasjonDao, QueryRunner by queryRunner {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    internal fun finnGenerasjoner(vedtaksperiodeId: UUID): List<GenerasjonDto> {
        return asSQL(
            """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, skjæringstidspunkt, fom, tom, tilstand, tags
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = :vedtaksperiode_id ORDER BY id;
        """,
            "vedtaksperiode_id" to vedtaksperiodeId,
        )
            .list { row ->
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
                    avslag = AvslagDao(queryRunner).finnAvslag(vedtaksperiodeId, generasjonRef),
                    saksbehandlerVurdering = AvslagDao(queryRunner).finnVurdering(vedtaksperiodeId, generasjonRef),
                )
            }
    }

    internal fun lagreGenerasjon(generasjonDto: GenerasjonDto) {
        lagre(generasjonDto)
        slettVarsler(generasjonDto.id, generasjonDto.varsler.map { it.id })
        generasjonDto.varsler.forEach { varselDto ->
            lagre(varselDto, generasjonDto.vedtaksperiodeId, generasjonDto.id)
        }
    }

    private fun lagre(generasjonDto: GenerasjonDto) {
        asSQL(
            """
            INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, opprettet_tidspunkt, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, fom, tom, skjæringstidspunkt, tilstand, tags) 
            VALUES (:unik_id, :vedtaksperiode_id, :utbetaling_id, :spleis_behandling_id, now(), gen_random_uuid(), now(), gen_random_uuid(), :fom, :tom, :skjaeringstidspunkt, :tilstand::generasjon_tilstand, :tags::varchar[])
            ON CONFLICT (unik_id) DO UPDATE SET utbetaling_id = excluded.utbetaling_id, spleis_behandling_id = excluded.spleis_behandling_id, fom = excluded.fom, tom = excluded.tom, skjæringstidspunkt = excluded.skjæringstidspunkt, tilstand = excluded.tilstand, tags = excluded.tags
            """,
            "unik_id" to generasjonDto.id,
            "vedtaksperiode_id" to generasjonDto.vedtaksperiodeId,
            "utbetaling_id" to generasjonDto.utbetalingId,
            "spleis_behandling_id" to generasjonDto.spleisBehandlingId,
            "fom" to generasjonDto.fom,
            "tom" to generasjonDto.tom,
            "skjaeringstidspunkt" to generasjonDto.skjæringstidspunkt,
            "tilstand" to generasjonDto.tilstand.name,
            "tags" to generasjonDto.tags.joinToString(prefix = "{", postfix = "}"),
        ).update()
    }

    private fun lagre(
        varselDto: VarselDto,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
    ) {
        asSQL(
            """
            INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status_endret_ident, status_endret_tidspunkt, status) 
            VALUES (:unik_id, :kode, :vedtaksperiode_id, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id), null, :opprettet, null, null, :status)
            ON CONFLICT (generasjon_ref, kode) DO UPDATE SET status = excluded.status, generasjon_ref = excluded.generasjon_ref
            """,
            "unik_id" to varselDto.id,
            "kode" to varselDto.varselkode,
            "vedtaksperiode_id" to vedtaksperiodeId,
            "generasjon_id" to generasjonId,
            "opprettet" to varselDto.opprettet,
            "status" to varselDto.status.name,
        ).update()
    }

    private fun slettVarsler(
        generasjonId: UUID,
        varselIder: List<UUID>,
    ) {
        asSQLWithQuestionMarks(
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
            },
            generasjonId,
            *varselIder.toTypedArray(),
        ).update()
    }

    private fun finnVarsler(generasjonRef: Long): List<VarselDto> {
        return asSQL(
            """
            SELECT 
            unik_id, 
            kode, 
            vedtaksperiode_id, 
            opprettet, 
            status 
            FROM selve_varsel sv WHERE generasjon_ref = :generasjon_ref
            """,
            "generasjon_ref" to generasjonRef,
        ).list { row ->
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
        }
    }

    internal fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> {
        return asSQL(
            """
            SELECT svg.vedtaksperiode_id FROM selve_vedtaksperiode_generasjon svg 
            INNER JOIN vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to fødselsnummer,
        ).list {
            it.uuid("vedtaksperiode_id")
        }.toSet()
    }

    override fun førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? {
        return asSQL(
            """
            SELECT tilstand_endret_tidspunkt 
            FROM selve_vedtaksperiode_generasjon 
            WHERE vedtaksperiode_id = :vedtaksperiodeId AND tilstand = 'VedtakFattet'
            ORDER BY tilstand_endret_tidspunkt
            LIMIT 1
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            it.localDateTimeOrNull("tilstand_endret_tidspunkt")
        }
    }

    internal fun førsteKjenteDag(fødselsnummer: String): LocalDate {
        return asSQL(
            """
            select min(svg.fom) as foersteFom
            from selve_vedtaksperiode_generasjon svg
            join vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
            join person p on p.id = v.person_ref
            where p.fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to fødselsnummer,
        ).single {
            it.localDate("foersteFom")
        }
    }
}
