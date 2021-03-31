package no.nav.helse.modell.saksbehandler.behandlingsstatistikk

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class BehandlingsstatistikkDao(private val dataSource: DataSource) {

    internal fun oppgavestatistikk(fom: LocalDate) = using(sessionOf(dataSource)) { session ->
        val tilGodkjenningPerPeriodetype = tilGodkjenningPerPeriodetype(fom)
        val totaltTilGodkjenning = tilGodkjenningPerPeriodetype.values.sumBy { it }
        BehandlingsstatistikkDto(
            BehandlingsstatistikkDto.OppgaverTilGodkjenningDto(
                totaltTilGodkjenning,
                tilGodkjenningPerPeriodetype(fom)
            ),
            tideltTotalt(fom),
            godkjentTotalt(fom),
            antallAnnulleringer(fom)
        )
    }

    private fun tilGodkjenningPerPeriodetype(fom: LocalDate) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT s.type as periodetype, COUNT(1) as antall FROM oppgave o
                 INNER JOIN vedtak v on o.vedtak_ref = v.id
                 INNER JOIN saksbehandleroppgavetype s on v.id = s.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler' AND o.oppdatert >= :fom
            GROUP BY s.type
        """
        session.run(queryOf(query, mapOf("fom" to fom)).map { tilGodkjenningForPeriodetypeDto(it) }.asList).toMap()
    }

    private fun tideltTotalt(fom: LocalDate) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT COUNT(1) as antall FROM oppgave o
                INNER JOIN tildeling t on o.id = t.oppgave_id_ref
            WHERE o.status = 'AvventerSaksbehandler' AND o.oppdatert >= :fom
        """
        session.run(queryOf(query, mapOf("fom" to fom)).map { it.int("antall") }.asSingle)
    })

    private fun godkjentTotalt(fom: LocalDate) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT COUNT(1) as antall FROM oppgave o WHERE o.status = 'Ferdigstilt' AND o.oppdatert >= :fom
        """
        session.run(queryOf(query, mapOf("fom" to fom)).map { it.int("antall") }.asSingle)
    })

    private fun antallAnnulleringer(fom: LocalDate) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT COUNT(1) as antall FROM annullert_av_saksbehandler WHERE annullert_tidspunkt >= :fom
        """
        session.run(queryOf(query, mapOf("fom" to fom)).map { it.int("antall") }.asSingle)
    })

    private fun tilGodkjenningForPeriodetypeDto(row: Row) =
        Saksbehandleroppgavetype.valueOf(row.string("periodetype")) to row.int("antall")
}
