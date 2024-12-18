package no.nav.helse.spesialist.api.vedtak

import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.varsel.Varsel
import java.util.UUID
import javax.sql.DataSource

internal class GenerasjonDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    internal fun gjeldendeGenerasjonFor(oppgaveId: Long): Vedtaksperiode =
        asSQL(
            """
            SELECT b.vedtaksperiode_id, b.fom, b.tom, b.skjæringstidspunkt
            FROM vedtak v 
            INNER JOIN behandling b on v.vedtaksperiode_id = b.vedtaksperiode_id
            JOIN oppgave o ON v.id = o.vedtak_ref
            WHERE o.id = :oppgave_id
            ORDER BY b.id DESC LIMIT 1;
            """.trimIndent(),
            "oppgave_id" to oppgaveId,
        ).single { it.tilVedtaksperiode() }

    internal fun gjeldendeGenerasjonerForPerson(oppgaveId: Long): Set<Vedtaksperiode> =
        asSQL(
            """
            SELECT DISTINCT ON (b.vedtaksperiode_id) b.vedtaksperiode_id, b.fom, b.tom, b.skjæringstidspunkt 
            FROM vedtak v
            INNER JOIN behandling b on v.vedtaksperiode_id = b.vedtaksperiode_id
            WHERE person_ref = 
                (SELECT person_ref FROM vedtak v2
                JOIN oppgave o on v2.id = o.vedtak_ref
                WHERE o.id = :oppgave_id) AND v.forkastet = false
            ORDER BY b.vedtaksperiode_id, b.id DESC;
            """.trimIndent(),
            "oppgave_id" to oppgaveId,
        ).list { it.tilVedtaksperiode() }.toSet()

    internal fun gjeldendeGenerasjonFor(
        oppgaveId: Long,
        varselGetter: (generasjonId: UUID) -> Set<Varsel>,
    ): Vedtaksperiode =
        asSQL(
            """
            SELECT b.vedtaksperiode_id, b.unik_id, b.fom, b.tom, b.skjæringstidspunkt
            FROM vedtak v 
            INNER JOIN behandling b on v.vedtaksperiode_id = b.vedtaksperiode_id
            JOIN oppgave o ON v.id = o.vedtak_ref
            WHERE o.id = :oppgave_id
            ORDER BY b.id DESC LIMIT 1;
            """.trimIndent(),
            "oppgave_id" to oppgaveId,
        ).single { it.tilVedtaksperiode(varselGetter) }

    internal fun gjeldendeGenerasjonerForPerson(
        oppgaveId: Long,
        varselGetter: (generasjonId: UUID) -> Set<Varsel>,
    ): Set<Vedtaksperiode> =
        asSQL(
            """
            SELECT DISTINCT ON (b.vedtaksperiode_id) b.vedtaksperiode_id, b.unik_id, b.fom, b.tom, b.skjæringstidspunkt 
            FROM vedtak v
            INNER JOIN behandling b on v.vedtaksperiode_id = b.vedtaksperiode_id
            WHERE person_ref = 
                (SELECT person_ref FROM vedtak v2
                JOIN oppgave o on v2.id = o.vedtak_ref
                WHERE o.id = :oppgave_id) AND v.forkastet = false
            ORDER BY b.vedtaksperiode_id, b.id DESC;
            """.trimIndent(),
            "oppgave_id" to oppgaveId,
        ).list { it.tilVedtaksperiode(varselGetter) }.toSet()

    private fun Row.tilVedtaksperiode(varselGetter: (generasjonId: UUID) -> Set<Varsel>) = tilVedtaksperiode(varselGetter(uuid("unik_id")))

    private fun Row.tilVedtaksperiode(varsler: Set<Varsel> = emptySet()) =
        Vedtaksperiode(
            uuid("vedtaksperiode_id"),
            localDate("fom"),
            localDate("tom"),
            localDate("skjæringstidspunkt"),
            varsler,
        )
}
