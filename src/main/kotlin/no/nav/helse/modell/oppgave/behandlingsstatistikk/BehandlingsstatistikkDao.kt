package no.nav.helse.modell.oppgave.behandlingsstatistikk

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.Periodetype
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

class BehandlingsstatistikkDao(private val dataSource: DataSource) {

    internal fun oppgavestatistikk(fom: LocalDate = LocalDate.now()): BehandlingsstatistikkDto {
        val tilGodkjenningPerPeriodetype = tilGodkjenningPerPeriodetype()
        val tildeltPerPeriodetype = tildeltPerPeriodetype()
        val antallAnnulleringer = antallAnnulleringer(fom)
        val antallManuelleGodkjenninger = godkjentManueltTotalt(fom)
        val antallAutomatiskeGodkjenninger = godkjentAutomatiskTotalt(fom)
        val behandletTotalt = antallAnnulleringer + antallAutomatiskeGodkjenninger + antallManuelleGodkjenninger

        return BehandlingsstatistikkDto(
            oppgaverTilGodkjenning = BehandlingsstatistikkDto.OppgavestatistikkDto(
                totalt = tilGodkjenningPerPeriodetype.sumBy { (_, antall) -> antall },
                perPeriodetype = tilGodkjenningPerPeriodetype
            ),
            tildelteOppgaver = BehandlingsstatistikkDto.OppgavestatistikkDto(
                totalt = tildeltPerPeriodetype.sumBy { (_, antall) -> antall },
                perPeriodetype = tildeltPerPeriodetype
            ),
            fullførteBehandlinger = BehandlingsstatistikkDto.BehandlingerDto(
                annullert = antallAnnulleringer,
                manuelt = antallManuelleGodkjenninger,
                automatisk = antallAutomatiskeGodkjenninger,
                totalt = behandletTotalt
            )
        )
    }

    private fun tilGodkjenningPerPeriodetype() = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT s.type as periodetype, COUNT(1) as antall FROM oppgave o
                 INNER JOIN vedtak v on o.vedtak_ref = v.id
                 INNER JOIN saksbehandleroppgavetype s on v.id = s.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type
        """
        session.run(queryOf(query).map { perPeriodetype(it) }.asList)
    }

    private fun tildeltPerPeriodetype() = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT s.type as periodetype, COUNT(1) as antall FROM oppgave o
                 INNER JOIN vedtak v on o.vedtak_ref = v.id
                 INNER JOIN saksbehandleroppgavetype s on v.id = s.vedtak_ref
                 INNER JOIN tildeling t on o.id = t.oppgave_id_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type
        """
        session.run(queryOf(query).map { perPeriodetype(it) }.asList)
    }

    private fun godkjentManueltTotalt(fom: LocalDate) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT COUNT(1) as antall FROM oppgave o WHERE o.status = 'Ferdigstilt' AND o.oppdatert >= :fom
        """
        session.run(queryOf(query, mapOf("fom" to fom)).map { it.int("antall") }.asSingle)
    })

    private fun godkjentAutomatiskTotalt(fom: LocalDate) = requireNotNull(using(sessionOf(dataSource)) {session ->
        @Language("PostgreSQL")
        val query = """
            SELECT COUNT(1) as antall FROM automatisering a
                INNER JOIN vedtak v on a.vedtaksperiode_ref = v.id
            WHERE a.automatisert = true AND stikkprøve = false AND a.opprettet >= :fom
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

    private fun perPeriodetype(row: Row) =
        Periodetype.valueOf(row.string("periodetype")) to row.int("antall")
}
