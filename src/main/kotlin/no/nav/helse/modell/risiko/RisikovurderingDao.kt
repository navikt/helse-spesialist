package no.nav.helse.modell.risiko

import kotliquery.queryOf
import kotliquery.sessionOf
import java.util.*
import javax.sql.DataSource

internal class RisikovurderingDao(val dataSource: DataSource) {
    internal fun persisterRisikovurdering(risikovurdering: RisikovurderingDto) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { tx ->
                val id = requireNotNull(
                    tx.run(
                        queryOf(
                            "INSERT INTO risikovurdering (vedtaksperiode_id, opprettet, samlet_score, ufullstendig) VALUES (?, ?, ?, ?);",
                            risikovurdering.vedtaksperiodeId,
                            risikovurdering.opprettet,
                            risikovurdering.samletScore.toInt(),
                            risikovurdering.ufullstendig
                        ).asUpdateAndReturnGeneratedKey
                    )
                )
                risikovurdering.faresignaler.forEach { tekst ->
                    tx.run(
                        queryOf(
                            "INSERT INTO risikovurdering_faresignal (risikovurdering_ref, tekst) VALUES (?, ?);",
                            id,
                            tekst
                        ).asUpdate
                    )
                }
                risikovurdering.arbeidsuførhetvurdering.forEach { tekst ->
                    tx.run(
                        queryOf(
                            "INSERT INTO risikovurdering_arbeidsuforhetvurdering (risikovurdering_ref, tekst) VALUES (?, ?);",
                            id,
                            tekst
                        ).asUpdate
                    )
                }
            }
        }
    }

    internal fun hentRisikovurdering(vedtaksperiodeId: UUID): RisikovurderingDto? {
        return sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(
                queryOf("SELECT * FROM risikovurdering WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1", vedtaksperiodeId).map {
                    val id = it.long("id")
                    RisikovurderingDto(
                        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
                        opprettet = it.localDateTime("opprettet"),
                        samletScore = it.int("samlet_score").toDouble(),
                        ufullstendig = it.boolean("ufullstendig"),
                        faresignaler = session.run(
                            queryOf(
                                "SELECT DISTINCT tekst FROM risikovurdering_faresignal WHERE risikovurdering_ref = ?",
                                id
                            ).map { it.string("tekst") }.asList
                        ),
                        arbeidsuførhetvurdering = session.run(
                            queryOf(
                                "SELECT DISTINCT tekst FROM risikovurdering_arbeidsuforhetvurdering WHERE risikovurdering_ref = ?",
                                id
                            ).map { it.string("tekst") }.asList
                        )
                    )
                }.asSingle
            )
        }
    }
}
