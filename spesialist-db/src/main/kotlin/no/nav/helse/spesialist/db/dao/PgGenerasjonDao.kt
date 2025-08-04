package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.GenerasjonDao
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgGenerasjonDao private constructor(
    private val queryRunner: QueryRunner,
) : GenerasjonDao,
    QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    internal constructor(session: Session) : this(MedSession(session))

    override fun finnGenerasjoner(vedtaksperiodeId: UUID): List<BehandlingDto> =
        asSQL(
            """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, skjæringstidspunkt, fom, tom, tilstand, tags
            FROM behandling 
            WHERE vedtaksperiode_id = :vedtaksperiode_id ORDER BY id;
        """,
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list { row ->
            val generasjonRef = row.long("id")
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
                vedtakBegrunnelse = PgVedtakBegrunnelseDao(queryRunner).finnVedtakBegrunnelse(vedtaksperiodeId, generasjonRef),
                varsler = finnVarsler(generasjonRef),
            )
        }

    override fun lagreGenerasjon(behandlingDto: BehandlingDto) {
        lagre(behandlingDto)
        slettVarsler(behandlingDto.id, behandlingDto.varsler.map { it.id })
        behandlingDto.varsler.forEach { varselDto ->
            lagre(varselDto, behandlingDto.vedtaksperiodeId, behandlingDto.id)
        }
    }

    private fun lagre(behandlingDto: BehandlingDto) {
        asSQL(
            """
            INSERT INTO behandling (unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, opprettet_tidspunkt, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, fom, tom, skjæringstidspunkt, tilstand, tags) 
            VALUES (:unik_id, :vedtaksperiode_id, :utbetaling_id, :spleis_behandling_id, now(), gen_random_uuid(), now(), gen_random_uuid(), :fom, :tom, :skjaeringstidspunkt, :tilstand::generasjon_tilstand, :tags::varchar[])
            ON CONFLICT (unik_id) DO UPDATE SET utbetaling_id = excluded.utbetaling_id, spleis_behandling_id = excluded.spleis_behandling_id, fom = excluded.fom, tom = excluded.tom, skjæringstidspunkt = excluded.skjæringstidspunkt, tilstand = excluded.tilstand, tags = excluded.tags
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

    private fun finnVarsler(generasjonRef: Long): List<VarselDto> =
        asSQL(
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

    override fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> =
        asSQL(
            """
            SELECT b.vedtaksperiode_id FROM behandling b 
            INNER JOIN vedtak v on b.vedtaksperiode_id = v.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            WHERE fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to fødselsnummer,
        ).list {
            it.uuid("vedtaksperiode_id")
        }.toSet()

    override fun førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? =
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

    override fun førsteKjenteDag(fødselsnummer: String): LocalDate =
        asSQL(
            """
            select min(b.fom) as foersteFom
            from behandling b
            join vedtak v on b.vedtaksperiode_id = v.vedtaksperiode_id
            join person p on p.id = v.person_ref
            where p.fødselsnummer = :fodselsnummer
            """,
            "fodselsnummer" to fødselsnummer,
        ).single {
            it.localDate("foersteFom")
        }
}
