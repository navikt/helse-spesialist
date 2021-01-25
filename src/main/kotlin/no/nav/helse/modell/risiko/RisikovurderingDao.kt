package no.nav.helse.modell.risiko

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

internal class RisikovurderingDao(val dataSource: DataSource) {
    internal fun persisterRisikovurdering(risikovurdering: RisikovurderingDto) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, krever_supersaksbehandler, data, opprettet) VALUES (?, ?, ?, CAST (? AS JSON), ?);",
                    risikovurdering.vedtaksperiodeId,
                    risikovurdering.kanGodkjennesAutomatisk,
                    risikovurdering.kreverSupersaksbehandler,
                    objectMapper.writeValueAsString(risikovurdering.data),
                    risikovurdering.opprettet,
                ).asUpdate
            )
        }
    }

    internal fun hentRisikovurderingDto(vedtaksperiodeId: UUID): RisikovurderingDto? {
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT * FROM risikovurdering_2021 WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1", vedtaksperiodeId).map {
                    RisikovurderingDto(
                        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
                        opprettet = it.localDateTime("opprettet"),
                        kanGodkjennesAutomatisk = it.boolean("kan_godkjennes_automatisk"),
                        kreverSupersaksbehandler = it.boolean("krever_supersaksbehandler"),
                        data = objectMapper.readTree(it.string("data")),
                    )
                }.asSingle
            )
        }
    }


    internal fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering? {
        return hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) }
    }
}
