package no.nav.helse.modell.risiko

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.objectMapper
import java.util.*

internal fun Session.persisterRisikovurdering(risikovurdering: RisikovurderingDto) {
    val risikovurderingRef = requireNotNull(
        run(
            queryOf(
                "INSERT INTO risikovurdering (vedtaksperiode_id, opprettet, samlet_score, ufullstendig) VALUES (?, ?, ?, ?);",
                risikovurdering.vedtaksperiodeId,
                risikovurdering.opprettet,
                risikovurdering.samletScore,
                risikovurdering.ufullstendig
            ).asUpdateAndReturnGeneratedKey
        )
    )

    risikovurdering.faresignaler.forEach { tekst ->
        run(
            queryOf(
                "INSERT INTO risikovurdering_faresignal (risikovurdering_ref, tekst) VALUES (?, ?);",
                risikovurderingRef,
                tekst
            ).asUpdate
        )
    }


    risikovurdering.arbeidsuførhetvurdering.forEach { tekst ->
        run(
            queryOf(
                "INSERT INTO risikovurdering_arbeidsuforhetvurdering (risikovurdering_ref, tekst) VALUES (?, ?);",
                risikovurderingRef,
                tekst
            ).asUpdate
        )
    }
}

internal fun Session.hentRisikovurderingForVedtaksperiode(vedtaksperiodeId: UUID): RisikovurderingDto? =
    run(
        queryOf(
            """
                SELECT r.id,
                       r.opprettet,
                       r.vedtaksperiode_id,
                       r.samlet_score,
                       json_agg(DISTINCT rb.tekst) as faresignaler,
                       json_agg(DISTINCT ra.tekst) as arbeidsuførhetvurderinger,
                       r.ufullstendig
                FROM risikovurdering r
                         LEFT JOIN risikovurdering_faresignal rb on r.id = rb.risikovurdering_ref
                         LEFT JOIN risikovurdering_arbeidsuforhetvurdering ra on r.id = ra.risikovurdering_ref
                WHERE r.vedtaksperiode_id=?
                  AND (r.vedtaksperiode_id, r.id) IN (
                    SELECT rr.vedtaksperiode_id, max(rr.id)
                    FROM risikovurdering rr
                    GROUP BY rr.vedtaksperiode_id
                )
                GROUP BY r.id;
            """
            , vedtaksperiodeId
        )
            .map(::tilRisikovurderingDto)
            .asSingle
    )


private fun tilRisikovurderingDto(row: Row) = RisikovurderingDto(
    vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
    opprettet = row.localDateTime("opprettet"),
    samletScore = row.int("samlet_score"),
    faresignaler = objectMapper.readValue(row.string("faresignaler")),
    arbeidsuførhetvurdering = objectMapper.readValue(row.string("arbeidsuførhetvurderinger")),
    ufullstendig = row.boolean("ufullstendig")
)
