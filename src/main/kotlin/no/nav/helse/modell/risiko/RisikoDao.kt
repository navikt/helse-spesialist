package no.nav.helse.modell.risiko

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

class RisikoDao(private val dataSource: DataSource) {
    fun persisterRisikovurdering(
        vedtaksperiodeId: UUID,
        samletScore: Int,
        begrunnelser: List<String>,
        ufullstendig: Boolean
    ) = using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
        session.transaction { transaction ->
            val risikovurderingRef = requireNotNull(
                transaction.run(
                    queryOf(
                        "INSERT INTO risikovurdering (vedtaksperiode_id, samlet_score, ufullstendig) VALUES (?, ?, ?);",
                        vedtaksperiodeId,
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
}
