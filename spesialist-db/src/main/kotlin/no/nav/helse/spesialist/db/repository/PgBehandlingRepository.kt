package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.time.LocalDate
import java.util.UUID

class PgBehandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    BehandlingRepository {
    override fun finn(id: SpleisBehandlingId): Behandling? =
        asSQL(
            """
            SELECT spleis_behandling_id, tags, p.fødselsnummer, array_agg(bs.søknad_id) as søknad_ider
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            LEFT JOIN behandling_soknad bs ON b.spleis_behandling_id = bs.behandling_id
            WHERE b.spleis_behandling_id = :spleis_behandling_id
            GROUP BY b.spleis_behandling_id, tags, p.fødselsnummer
        """,
            "spleis_behandling_id" to id.value,
        ).singleOrNull { row ->
            Behandling.fraLagring(
                id = SpleisBehandlingId(row.uuid("spleis_behandling_id")),
                tags = row.array<String>("tags").toSet(),
                fødselsnummer = row.string("fødselsnummer"),
                søknadIder = row.arrayOrNull<UUID?>("søknad_ider")?.filterNotNull()?.toSet() ?: emptySet(),
            )
        }

    override fun finnBehandlingerISykefraværstilfelle(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<Behandling> =
        asSQL(
            """
            SELECT spleis_behandling_id, tags, array_agg(bs.søknad_id) as søknad_ider
            FROM behandling b
            INNER JOIN vedtak v on v.vedtaksperiode_id = b.vedtaksperiode_id
            INNER JOIN person p on p.id = v.person_ref
            LEFT JOIN behandling_soknad bs ON b.spleis_behandling_id = bs.behandling_id
            WHERE fødselsnummer = :fodselsnummer
            AND skjæringstidspunkt = :skjaeringstidspunkt
            GROUP BY b.spleis_behandling_id, b.id, tags, p.fødselsnummer
            ORDER BY b.id
        """,
            "fodselsnummer" to fødselsnummer,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        ).list { row ->
            Behandling.fraLagring(
                id = SpleisBehandlingId(row.uuid("spleis_behandling_id")),
                tags = row.array<String>("tags").toSet(),
                fødselsnummer = fødselsnummer,
                søknadIder = row.arrayOrNull<UUID?>("søknad_ider")?.filterNotNull()?.toSet() ?: emptySet(),
            )
        }

    override fun lagre(behandling: Behandling) {
        behandling.søknadIder().forEach { søknadId ->
            asSQL(
                """
                INSERT INTO behandling_soknad (behandling_id, søknad_id) VALUES (:behandlingId, :soknadId) ON CONFLICT DO NOTHING
                """.trimIndent(),
                "behandlingId" to behandling.id.value,
                "soknadId" to søknadId,
            ).update()
        }
    }
}
