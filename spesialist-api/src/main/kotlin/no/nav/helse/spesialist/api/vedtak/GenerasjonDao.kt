package no.nav.helse.spesialist.api.vedtak

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.varsel.Varsel

internal class GenerasjonDao(dataSource: DataSource) : HelseDao(dataSource) {

    internal fun gjeldendeGenerasjonFor(oppgaveId: Long): Vedtaksperiode = requireNotNull(
        queryize(
            """
                SELECT svg.vedtaksperiode_id, svg.fom, svg.tom, svg.skjæringstidspunkt
                FROM vedtak v 
                INNER JOIN selve_vedtaksperiode_generasjon svg on v.vedtaksperiode_id = svg.vedtaksperiode_id
                JOIN oppgave o ON v.id = o.vedtak_ref
                WHERE o.id = :oppgave_id
                ORDER BY svg.id DESC LIMIT 1;
            """
        ).single(mapOf("oppgave_id" to oppgaveId)) {
            Vedtaksperiode(
                it.uuid("vedtaksperiode_id"),
                it.localDate("fom"),
                it.localDate("tom"),
                it.localDate("skjæringstidspunkt"),
                emptySet()
            )
        })

    internal fun gjeldendeGenerasjonerForPerson(oppgaveId: Long): Set<Vedtaksperiode> = queryize(
        """
                SELECT DISTINCT ON (svg.vedtaksperiode_id) svg.vedtaksperiode_id, svg.fom, svg.tom, svg.skjæringstidspunkt 
                FROM vedtak v
                INNER JOIN selve_vedtaksperiode_generasjon svg on v.vedtaksperiode_id = svg.vedtaksperiode_id
                WHERE person_ref = 
                    (SELECT person_ref FROM vedtak v2
                    JOIN oppgave o on v2.id = o.vedtak_ref
                    WHERE o.id = :oppgave_id) AND v.forkastet = false
                ORDER BY svg.vedtaksperiode_id, svg.id DESC;
            """
    ).list(mapOf("oppgave_id" to oppgaveId)) {
        Vedtaksperiode(
            it.uuid("vedtaksperiode_id"),
            it.localDate("fom"),
            it.localDate("tom"),
            it.localDate("skjæringstidspunkt"),
            emptySet()
        )
    }.toSet()

    internal fun gjeldendeGenerasjonFor(oppgaveId: Long, varselGetter: (generasjonId: UUID) -> Set<Varsel>): Vedtaksperiode = requireNotNull(
        queryize(
            """
                SELECT svg.vedtaksperiode_id, svg.unik_id, svg.fom, svg.tom, svg.skjæringstidspunkt
                FROM vedtak v 
                INNER JOIN selve_vedtaksperiode_generasjon svg on v.vedtaksperiode_id = svg.vedtaksperiode_id
                JOIN oppgave o ON v.id = o.vedtak_ref
                WHERE o.id = :oppgave_id
                ORDER BY svg.id DESC LIMIT 1;
            """
        ).single(mapOf("oppgave_id" to oppgaveId)) {
            Vedtaksperiode(
                it.uuid("vedtaksperiode_id"),
                it.localDate("fom"),
                it.localDate("tom"),
                it.localDate("skjæringstidspunkt"),
                varselGetter(it.uuid("unik_id"))
            )
        })

    internal fun gjeldendeGenerasjonerForPerson(oppgaveId: Long, varselGetter: (generasjonId: UUID) -> Set<Varsel>): Set<Vedtaksperiode> = queryize(
        """
                SELECT DISTINCT ON (svg.vedtaksperiode_id) svg.vedtaksperiode_id, svg.unik_id, svg.fom, svg.tom, svg.skjæringstidspunkt 
                FROM vedtak v
                INNER JOIN selve_vedtaksperiode_generasjon svg on v.vedtaksperiode_id = svg.vedtaksperiode_id
                WHERE person_ref = 
                    (SELECT person_ref FROM vedtak v2
                    JOIN oppgave o on v2.id = o.vedtak_ref
                    WHERE o.id = :oppgave_id) AND v.forkastet = false
                ORDER BY svg.vedtaksperiode_id, svg.id DESC;
            """
    ).list(mapOf("oppgave_id" to oppgaveId)) {
        Vedtaksperiode(
            it.uuid("vedtaksperiode_id"),
            it.localDate("fom"),
            it.localDate("tom"),
            it.localDate("skjæringstidspunkt"),
            varselGetter(it.uuid("unik_id"))
        )
    }.toSet()
}