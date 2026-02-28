package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime

class PgBehandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    BehandlingRepository {
    override fun finn(id: SpleisBehandlingId): Behandling? =
        asSQL(
            """
            SELECT b.unik_id, b.vedtaksperiode_id, b.utbetaling_id, b.spleis_behandling_id, b.tags, b.fom, b.tom, b.skjæringstidspunkt, b.tilstand, b.yrkesaktivitetstype
            FROM behandling b
            INNER JOIN vedtaksperiode v on v.vedtaksperiode_id = b.vedtaksperiode_id
            WHERE b.spleis_behandling_id = :spleis_behandling_id AND v.forkastet = false
        """,
            "spleis_behandling_id" to id.value,
        ).singleOrNull(::tilBehandling)

    override fun finn(id: BehandlingUnikId): Behandling? =
        asSQL(
            """
            SELECT unik_id, b.vedtaksperiode_id, utbetaling_id, spleis_behandling_id, tags, b.fom, b.tom, skjæringstidspunkt, tilstand, yrkesaktivitetstype
            FROM behandling b
            INNER JOIN vedtaksperiode v on v.vedtaksperiode_id = b.vedtaksperiode_id
            WHERE unik_id = :unik_id AND v.forkastet = false
        """,
            "unik_id" to id.value,
        ).singleOrNull(::tilBehandling)

    override fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String,
    ): Set<Behandling> =
        asSQL(
            """
            SELECT DISTINCT ON (b.vedtaksperiode_id) b.vedtaksperiode_id, b.utbetaling_id, b.unik_id, b.spleis_behandling_id, b.tags, b.fom, b.tom, b.skjæringstidspunkt, b.tilstand, b.yrkesaktivitetstype
            FROM behandling b
                     INNER JOIN vedtaksperiode v on v.vedtaksperiode_id = b.vedtaksperiode_id
                     INNER JOIN person p on p.id = v.person_ref
            WHERE fødselsnummer = :fodselsnummer
              AND skjæringstidspunkt = :skjaeringstidspunkt
              AND v.forkastet = false
            ORDER BY b.vedtaksperiode_id, b.id DESC
        """,
            "fodselsnummer" to fødselsnummer,
            "skjaeringstidspunkt" to behandling.skjæringstidspunkt,
        ).list(::tilBehandling)
            .filterNot { it.spleisBehandlingId == behandling.spleisBehandlingId }
            .toSet()

    override fun finnNyesteForVedtaksperiode(vedtaksperiodeId: VedtaksperiodeId): Behandling? =
        asSQL(
            """
        SELECT unik_id, b.vedtaksperiode_id, utbetaling_id, spleis_behandling_id, tags, b.fom, b.tom, skjæringstidspunkt, opprettet_tidspunkt, tilstand, yrkesaktivitetstype
        FROM behandling b
            INNER JOIN vedtaksperiode v on v.vedtaksperiode_id = b.vedtaksperiode_id
        WHERE b.vedtaksperiode_id = :vedtaksperiode_id
          AND v.forkastet = false
        ORDER BY b.id DESC LIMIT 1
    """,
            "vedtaksperiode_id" to vedtaksperiodeId.value,
        ).singleOrNull(::tilBehandling)

    override fun lagre(behandling: Behandling) {
        asSQL(
            """
            INSERT INTO behandling(
                unik_id, spleis_behandling_id, vedtaksperiode_id, utbetaling_id, 
                fom, tom, skjæringstidspunkt, tilstand, tags, yrkesaktivitetstype,
                opprettet_tidspunkt, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse 
            ) 
            VALUES (
                :unik_id, :spleis_behandling_id, :vedtaksperiode_id, :utbetaling_id, 
                :fom, :tom, :skjaeringstidspunkt, :tilstand::generasjon_tilstand, :tags, :yrkesaktivitetstype,
                :opprettet_tidspunkt, :placeholder_uuid::uuid, :tilstand_endret_tidspunkt, :placeholder_uuid::uuid 
            )
            ON CONFLICT (unik_id) DO UPDATE SET 
                utbetaling_id = excluded.utbetaling_id,
                fom = excluded.fom,
                tom = excluded.tom,
                skjæringstidspunkt = excluded.skjæringstidspunkt,
                tilstand = excluded.tilstand,
                tags = excluded.tags,
                yrkesaktivitetstype = excluded.yrkesaktivitetstype,
                tilstand_endret_tidspunkt = excluded.tilstand_endret_tidspunkt
            """.trimIndent(),
            "unik_id" to behandling.id.value,
            "vedtaksperiode_id" to behandling.vedtaksperiodeId.value,
            "spleis_behandling_id" to behandling.spleisBehandlingId?.value,
            "utbetaling_id" to behandling.utbetalingId?.value,
            "tags" to behandling.tags.toTypedArray(),
            "tilstand" to behandling.tilstand.name,
            "opprettet_tidspunkt" to LocalDateTime.now(),
            "fom" to behandling.fom,
            "tom" to behandling.tom,
            "skjaeringstidspunkt" to behandling.skjæringstidspunkt,
            "yrkesaktivitetstype" to behandling.yrkesaktivitetstype.name,
            "tilstand_endret_tidspunkt" to LocalDateTime.now(),
            "placeholder_uuid" to "00000000-0000-0000-0000-000000000000",
        ).update()
    }

    override fun lagreAlle(behandlinger: Collection<Behandling>) {
        // Kan optimaliseres om nødvendig (batch insert / update)
        behandlinger.forEach(::lagre)
    }

    private fun tilBehandling(row: Row): Behandling {
        val spleisBehandlingId = row.uuidOrNull("spleis_behandling_id")?.let { SpleisBehandlingId(it) }
        return Behandling.fraLagring(
            id = BehandlingUnikId(row.uuid("unik_id")),
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = VedtaksperiodeId(row.uuid("vedtaksperiode_id")),
            utbetalingId = row.uuidOrNull("utbetaling_id")?.let(::UtbetalingId),
            tags = row.array<String>("tags").toSet(),
            tilstand =
                when (enumValueOf<DBTilstand>(row.string("tilstand"))) {
                    DBTilstand.VedtakFattet -> Behandling.Tilstand.VedtakFattet
                    DBTilstand.VidereBehandlingAvklares -> Behandling.Tilstand.VidereBehandlingAvklares
                    DBTilstand.AvsluttetUtenVedtak -> Behandling.Tilstand.AvsluttetUtenVedtak
                    DBTilstand.AvsluttetUtenVedtakMedVarsler -> Behandling.Tilstand.AvsluttetUtenVedtakMedVarsler
                    DBTilstand.KlarTilBehandling -> Behandling.Tilstand.KlarTilBehandling
                },
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
            skjæringstidspunkt = row.localDateOrNull("skjæringstidspunkt") ?: row.localDate("fom"),
            yrkesaktivitetstype =
                row
                    .stringOrNull("yrkesaktivitetstype")
                    ?.let { enumValueOf<DBYrkesaktivitetstype>(it) }
                    .let {
                        when (it) {
                            DBYrkesaktivitetstype.ARBEIDSTAKER -> Yrkesaktivitetstype.ARBEIDSTAKER
                            DBYrkesaktivitetstype.FRILANS -> Yrkesaktivitetstype.FRILANS
                            DBYrkesaktivitetstype.ARBEIDSLEDIG -> Yrkesaktivitetstype.ARBEIDSLEDIG
                            DBYrkesaktivitetstype.SELVSTENDIG -> Yrkesaktivitetstype.SELVSTENDIG
                            null -> Yrkesaktivitetstype.ARBEIDSTAKER // Alle gamle behandlinger gjaldt arbeidstaker
                        }
                    },
        )
    }

    // For å støtte et potensielt annet sett med verdier i databasen, må vi definere og mappe nesten like enums
    private enum class DBTilstand {
        VedtakFattet,
        VidereBehandlingAvklares,
        AvsluttetUtenVedtak,
        AvsluttetUtenVedtakMedVarsler,
        KlarTilBehandling,
    }

    private enum class DBYrkesaktivitetstype {
        ARBEIDSTAKER,
        FRILANS,
        ARBEIDSLEDIG,
        SELVSTENDIG,
    }
}
