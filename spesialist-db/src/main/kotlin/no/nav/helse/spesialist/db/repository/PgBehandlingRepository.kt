package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.util.UUID

class PgBehandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    BehandlingRepository {
    override fun finn(id: SpleisBehandlingId): Behandling? =
        asSQL(
            """
            SELECT b.vedtaksperiode_id, spleis_behandling_id, tags, b.fom, b.tom, b.skjæringstidspunkt
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            WHERE b.spleis_behandling_id = :spleis_behandling_id
        """,
            "spleis_behandling_id" to id.value,
        ).singleOrNull(::tilBehandling)

    override fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String,
    ): Set<Behandling> =
        asSQL(
            """
            SELECT DISTINCT ON (b.vedtaksperiode_id) b.vedtaksperiode_id, spleis_behandling_id, tags, b.fom, b.tom, b.skjæringstidspunkt
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
        behandling.søknadIder().forEach { søknadId ->
            asSQL(
                """
                INSERT INTO behandling_soknad (behandling_id, søknad_id) VALUES (:behandlingId, :soknadId) ON CONFLICT DO NOTHING
                """.trimIndent(),
                "behandlingId" to behandling.spleisBehandlingId.value,
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
        val spleisBehandlingId = SpleisBehandlingId(row.uuid("spleis_behandling_id"))
        return Behandling.fraLagring(
            id = spleisBehandlingId,
            vedtaksperiodeId = VedtaksperiodeId(row.uuid("vedtaksperiode_id")),
            tags = row.array<String>("tags").toSet(),
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
            skjæringstidspunkt = row.localDate("skjæringstidspunkt"),
            søknadIder = hentSøkadIderForBehandling(spleisBehandlingId),
        )
    }
}
