package no.nav.helse.spesialist.api.vedtak

import javax.sql.DataSource
import no.nav.helse.HelseDao

internal class ApiVedtakDao(dataSource: DataSource) : HelseDao(dataSource) {

    internal fun vedtakFor(oppgaveId: Long): ApiVedtak = requireNotNull(
        queryize(
            """
                SELECT v.id, vedtaksperiode_id, fom, tom 
                FROM vedtak v 
                JOIN oppgave o ON v.id = o.vedtak_ref
                WHERE o.id = :oppgave_id;
            """
        ).single(mapOf("oppgave_id" to oppgaveId)) {
            ApiVedtak(
                it.long("id"),
                it.uuid("vedtaksperiode_id"),
                it.localDate("fom"),
                it.localDate("tom"),
            )
        })

    internal fun alleVedtakForPerson(oppgaveId: Long): Set<ApiVedtak> = queryize(
        """
                SELECT v.id, vedtaksperiode_id, fom, tom 
                FROM vedtak v
                WHERE person_ref = 
                    (SELECT person_ref FROM vedtak v2
                    JOIN oppgave o on v2.id = o.vedtak_ref
                    WHERE o.id = :oppgave_id);
            """
    ).list(mapOf("oppgave_id" to oppgaveId)) {
        ApiVedtak(
            it.long("id"),
            it.uuid("vedtaksperiode_id"),
            it.localDate("fom"),
            it.localDate("tom"),
        )
    }.toSet()
}