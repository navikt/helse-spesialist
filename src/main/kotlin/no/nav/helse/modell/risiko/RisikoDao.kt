package no.nav.helse.modell.risiko

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class RisikoDao(private val dataSource: DataSource) {
    fun persisterRisikovurdering(
        vedtaksperiodeId: UUID,
        opprettet: LocalDateTime,
        samletScore: Int,
        begrunnelser: List<String>,
        ufullstendig: Boolean
    ) = using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
        session.transaction { transaction ->
            val risikovurderingRef = requireNotNull(
                transaction.run(
                    queryOf(
                        "INSERT INTO risikovurdering (vedtaksperiode_id, opprettet, samlet_score, ufullstendig) VALUES (?, ?, ?, ?);",
                        vedtaksperiodeId,
                        opprettet,
                        samletScore,
                        ufullstendig
                    ).asUpdateAndReturnGeneratedKey
                )
            )

            begrunnelser.forEach { begrunnelse ->
                transaction.run(
                    queryOf(
                        "INSERT INTO risikovurdering_begrunnelse (risikovurdering_ref, begrunnelse) VALUES (?, ?);",
                        risikovurderingRef,
                        begrunnelse
                    ).asUpdate
                )
            }
        }
    }

    fun hentRisikovurderingerForArbeidsgiver(arbeidsgiverRef: Int) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """
                    SELECT r.id, r.opprettet, r.vedtaksperiode_id, r.samlet_score, rb.begrunnelse, r.ufullstendig
                    FROM risikovurdering r
                             INNER JOIN vedtak v on r.vedtaksperiode_id = v.vedtaksperiode_id
                             INNER JOIN risikovurdering_begrunnelse rb on r.id = rb.risikovurdering_ref
                    WHERE v.arbeidsgiver_ref=?
                      AND (r.id, r.vedtaksperiode_id) IN (
                        SELECT max(rr.id), rr.vedtaksperiode_id
                        FROM risikovurdering rr
                        GROUP BY rr.vedtaksperiode_id
                    )
                """
                    , arbeidsgiverRef
                )
                    .map(::tilRisikovurderingDto)
                    .asList
            )
        }
            .groupBy { it.vedtaksperiodeId }
            .map {
                it.value.reduce { champion, challenger ->
                    champion.copy(begrunnelser = champion.begrunnelser + challenger.begrunnelser)
                }
            }


    private fun tilRisikovurderingDto(row: Row) = RisikovurderingDto(
        vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
        opprettet = row.localDateTime("opprettet"),
        samletScore = row.int("samlet_score"),
        begrunnelser = listOf(row.string("begrunnelse")),
        ufullstendig = row.boolean("ufullstendig")
    )
}
