package no.nav.helse.spesialist.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.LegacyBehandlingDao
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.mediator.asLocalDateTime
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgLegacyBehandlingDao private constructor(
    private val queryRunner: QueryRunner,
) : LegacyBehandlingDao,
    QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    internal constructor(session: Session) : this(MedSession(session))

    override fun finnLegacyBehandlinger(vedtaksperiodeId: UUID): List<BehandlingDto> =
        asSQL(
            """
            WITH behandlinger AS (
                SELECT b.id, b.unik_id, b.vedtaksperiode_id, utbetaling_id, spleis_behandling_id, skjæringstidspunkt, fom, tom, tilstand, tags, yrkesaktivitetstype, json_agg(DISTINCT to_jsonb(sv.*)) FILTER (WHERE sv.id IS NOT NULL) AS varsler
                FROM behandling b
                LEFT JOIN selve_varsel sv ON b.id = sv.generasjon_ref
                WHERE b.vedtaksperiode_id = :vedtaksperiode_id
                GROUP BY b.id
                ORDER BY b.id
            ), begrunnelser AS (
                SELECT b.type, b.tekst, vb.generasjon_ref FROM vedtak_begrunnelse vb
                LEFT JOIN begrunnelse b ON b.id = vb.begrunnelse_ref
                WHERE vb.vedtaksperiode_id = :vedtaksperiode_id AND vb.invalidert = false
                ORDER BY vb.opprettet DESC LIMIT 1
            )
            SELECT bh.*, beg.*
            FROM behandlinger AS bh
            LEFT JOIN begrunnelser beg ON beg.generasjon_ref = bh.id
            """,
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list { row ->
            BehandlingDto(
                id = row.uuid("unik_id"),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                utbetalingId = row.uuidOrNull("utbetaling_id"),
                spleisBehandlingId = row.uuidOrNull("spleis_behandling_id"),
                skjæringstidspunkt = row.localDateOrNull("skjæringstidspunkt") ?: row.localDate("fom"),
                fom = row.localDate("fom"),
                tom = row.localDate("tom"),
                tilstand = enumValueOf(row.string("tilstand")),
                tags = row.array<String>("tags").toList(),
                vedtakBegrunnelse = row.mapVedtaksbegrunnelse(),
                varsler = row.toDto(),
                yrkesaktivitetstype = row.stringOrNull("yrkesaktivitetstype")?.let { Yrkesaktivitetstype.valueOf(it) } ?: Yrkesaktivitetstype.ARBEIDSTAKER,
            )
        }

    fun Row.mapVedtaksbegrunnelse(): VedtakBegrunnelse? {
        val begrunnelse = this.stringOrNull("tekst") ?: return null
        val type = enumValueOf<VedtakBegrunnelseTypeFraDatabase>(this.stringOrNull("type") ?: return null)
        return VedtakBegrunnelse(utfall = type.toDomain(), begrunnelse = begrunnelse)
    }

    fun VedtakBegrunnelseTypeFraDatabase.toDomain() =
        when (this) {
            VedtakBegrunnelseTypeFraDatabase.AVSLAG -> Utfall.AVSLAG
            VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> Utfall.DELVIS_INNVILGELSE
            VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> Utfall.INNVILGELSE
        }

    override fun finnLegacyBehandling(behandlingDto: BehandlingDto) {
        lagre(behandlingDto)
        slettVarsler(behandlingDto.id, behandlingDto.varsler.map { it.id })
        behandlingDto.varsler.forEach { varselDto ->
            lagre(varselDto, behandlingDto.vedtaksperiodeId, behandlingDto.id)
        }
    }

    private fun lagre(behandlingDto: BehandlingDto) {
        asSQL(
            """
            INSERT INTO behandling (unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, opprettet_tidspunkt, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, fom, tom, skjæringstidspunkt, tilstand, tags, yrkesaktivitetstype) 
            VALUES (:unik_id, :vedtaksperiode_id, :utbetaling_id, :spleis_behandling_id, now(), gen_random_uuid(), now(), gen_random_uuid(), :fom, :tom, :skjaeringstidspunkt, :tilstand::generasjon_tilstand, :tags::varchar[], :yrkesaktivitetstype)
            ON CONFLICT (unik_id) DO UPDATE SET utbetaling_id = excluded.utbetaling_id, spleis_behandling_id = excluded.spleis_behandling_id, fom = excluded.fom, tom = excluded.tom, skjæringstidspunkt = excluded.skjæringstidspunkt, tilstand = excluded.tilstand, tags = excluded.tags, yrkesaktivitetstype = excluded.yrkesaktivitetstype
            """,
            "unik_id" to behandlingDto.id,
            "vedtaksperiode_id" to behandlingDto.vedtaksperiodeId,
            "utbetaling_id" to behandlingDto.utbetalingId,
            "spleis_behandling_id" to behandlingDto.spleisBehandlingId,
            "fom" to behandlingDto.fom,
            "tom" to behandlingDto.tom,
            "skjaeringstidspunkt" to behandlingDto.skjæringstidspunkt,
            "tilstand" to behandlingDto.tilstand.name,
            "tags" to behandlingDto.tags.somDbArray(),
            "yrkesaktivitetstype" to behandlingDto.yrkesaktivitetstype.name,
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
            VALUES (:unik_id, :kode, :vedtaksperiode_id, (SELECT id FROM behandling WHERE unik_id = :generasjon_id), null, :opprettet, null, null, :status)
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
                DELETE FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM behandling b WHERE b.unik_id = ? LIMIT 1)
                """.trimIndent()
            } else {
                """
                DELETE FROM selve_varsel 
                WHERE generasjon_ref = (SELECT id FROM behandling b WHERE b.unik_id = ? LIMIT 1) 
                AND selve_varsel.unik_id NOT IN (${varselIder.joinToString { "?" }})
                """.trimIndent()
            },
            generasjonId,
            *varselIder.toTypedArray(),
        ).update()
    }

    private fun Row.toDto(): List<VarselDto> {
        val varsler = this.stringOrNull("varsler") ?: return emptyList()
        return objectMapper.readTree(varsler).map { varsel ->
            VarselDto(
                varsel["unik_id"].asUUID(),
                varsel["kode"].asText(),
                varsel["opprettet"].asLocalDateTime(),
                varsel["vedtaksperiode_id"].asUUID(),
                when (val status = varsel["status"].asText()) {
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

    override fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> =
        asSQL(
            """
            SELECT b.vedtaksperiode_id FROM behandling b 
            INNER JOIN vedtaksperiode v on b.vedtaksperiode_id = v.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to fødselsnummer,
        ).list {
            it.uuid("vedtaksperiode_id")
        }.toSet()

    override fun førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? =
        asSQL(
            """
            SELECT tilstand_endret_tidspunkt 
            FROM behandling 
            WHERE vedtaksperiode_id = :vedtaksperiodeId AND tilstand = 'VedtakFattet'
            ORDER BY tilstand_endret_tidspunkt
            LIMIT 1
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            it.localDateTimeOrNull("tilstand_endret_tidspunkt")
        }

    override fun førsteKjenteDag(fødselsnummer: String): LocalDate? =
        asSQL(
            """
            select min(b.fom) as foersteFom
            from behandling b
            join vedtaksperiode v on b.vedtaksperiode_id = v.vedtaksperiode_id
            join person p on p.id = v.person_ref
            where p.fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull {
            it.localDateOrNull("foersteFom")
        }
}
