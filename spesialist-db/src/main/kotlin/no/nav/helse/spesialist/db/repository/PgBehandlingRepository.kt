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
import java.util.UUID

class PgBehandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    BehandlingRepository {
    override fun finn(id: SpleisBehandlingId): Behandling? =
        asSQL(
            """
            SELECT b.unik_id, b.vedtaksperiode_id, b.utbetaling_id, b.spleis_behandling_id, b.tags, b.fom, b.tom, b.skjæringstidspunkt, b.tilstand, b.yrkesaktivitetstype
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            WHERE b.spleis_behandling_id = :spleis_behandling_id
        """,
            "spleis_behandling_id" to id.value,
        ).singleOrNull(::tilBehandling)

    override fun finn(id: BehandlingUnikId): Behandling? =
        asSQL(
            """
            SELECT unik_id, vedtaksperiode_id, utbetaling_id, spleis_behandling_id, tags, fom, tom, skjæringstidspunkt, tilstand, yrkesaktivitetstype
            FROM behandling
            WHERE unik_id = :unik_id
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
                     INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
                     INNER JOIN person p on p.id = v.person_ref
            WHERE fødselsnummer = :fodselsnummer
              AND skjæringstidspunkt = :skjaeringstidspunkt
            ORDER BY b.vedtaksperiode_id, b.id DESC
        """,
            "fodselsnummer" to fødselsnummer,
            "skjaeringstidspunkt" to behandling.skjæringstidspunkt,
        ).list(::tilBehandling)
            .filterNot { it.spleisBehandlingId == behandling.spleisBehandlingId }
            .toSet()

    override fun lagre(behandling: Behandling) {
        // TODO: OBS OBS, DENNE LAGRER IKKE FAKTISK BEHANDLINGEN I BEHANDLING-TABELLEN (MEN BØR GJØRE DET)
        val spleisBehandlingId = checkNotNull(behandling.spleisBehandlingId)
        behandling.søknadIder().forEach { søknadId ->
            asSQL(
                """
                INSERT INTO behandling_soknad (behandling_id, søknad_id) VALUES (:behandlingId, :soknadId) ON CONFLICT DO NOTHING
                """.trimIndent(),
                "behandlingId" to spleisBehandlingId.value,
                "soknadId" to søknadId,
            ).update()
        }
    }

    private fun hentSøkadIderForBehandling(behandlingId: SpleisBehandlingId): Set<UUID> =
        asSQL(
            "SELECT søknad_id FROM behandling_soknad bs WHERE bs.behandling_id = :spleisBehandlingId",
            "spleisBehandlingId" to behandlingId.value,
        ).list {
            it.uuid("søknad_id")
        }.toSet()

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
            søknadIder = spleisBehandlingId?.let { hentSøkadIderForBehandling(it) } ?: emptySet(),
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
