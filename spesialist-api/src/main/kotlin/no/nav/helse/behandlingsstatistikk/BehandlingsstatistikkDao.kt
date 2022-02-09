package no.nav.helse.behandlingsstatistikk

import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.oppgave.Oppgavetype
import java.time.LocalDate
import javax.sql.DataSource

class BehandlingsstatistikkDao(dataSource: DataSource): HelseDao(dataSource) {

    fun oppgavestatistikk(fom: LocalDate = LocalDate.now()): BehandlingsstatistikkDto {
        val tilGodkjenningPerPeriodetype = tilGodkjenningPerPeriodetype()
        val tildeltPerPeriodetype = tildeltPerPeriodetype()
        val antallAnnulleringer = antallAnnulleringer(fom)
        val antallManuelleGodkjenninger = godkjentManueltTotalt(fom)
        val antallAutomatiskeGodkjenninger = godkjentAutomatiskTotalt(fom)
        val behandletTotalt = antallAnnulleringer + antallAutomatiskeGodkjenninger + antallManuelleGodkjenninger

        return BehandlingsstatistikkDto(
            oppgaverTilGodkjenning = BehandlingsstatistikkDto.OppgavestatistikkDto(
                totalt = tilGodkjenningPerPeriodetype.sumOf { (_, antall) -> antall },
                perPeriodetype = tilGodkjenningPerPeriodetype,
            ),
            tildelteOppgaver = BehandlingsstatistikkDto.OppgavestatistikkDto(
                totalt = tildeltPerPeriodetype.sumOf { (_, antall) -> antall },
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

    private fun tilGodkjenningPerPeriodetype() =
        """ SELECT sot.type AS periodetype, o.type, COUNT(distinct o.id)
            FILTER (WHERE o.type != 'UTBETALING_TIL_SYKMELDT' or o.type != 'DELVIS_REFUSJON') AS antall,
            COUNT(distinct o.type) as antallAvOppgaveType FROM oppgave o
              INNER JOIN saksbehandleroppgavetype sot ON o.vedtak_ref = sot.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY sot.type, o.type
        """.list { perStatistikktype(it) }

    private fun tildeltPerPeriodetype() =
        """ SELECT s.type as periodetype, o.type, COUNT(distinct s.type)
            FILTER (WHERE o.type != 'UTBETALING_TIL_SYKMELDT' or o.type != 'DELVIS_REFUSJON') AS antall,
            COUNT(distinct o.type) as antallAvOppgaveType FROM oppgave o
                 INNER JOIN vedtak v on o.vedtak_ref = v.id
                 INNER JOIN saksbehandleroppgavetype s on v.id = s.vedtak_ref
                 INNER JOIN tildeling t on o.id = t.oppgave_id_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type, o.type
        """.list { perStatistikktype(it) }

    private fun godkjentManueltTotalt(fom: LocalDate) = requireNotNull(
        """ SELECT COUNT(1) as antall FROM oppgave o WHERE o.status = 'Ferdigstilt' AND o.oppdatert >= :fom"""
            .single(mapOf("fom" to fom)) { it.int("antall") } )

    private fun godkjentAutomatiskTotalt(fom: LocalDate) = requireNotNull(
        """ SELECT COUNT(1) as antall FROM automatisering a
                INNER JOIN vedtak v on a.vedtaksperiode_ref = v.id
            WHERE a.automatisert = true AND stikkprøve = false AND a.opprettet >= :fom
        """.single(mapOf("fom" to fom)) { it.int("antall")})

    private fun antallAnnulleringer(fom: LocalDate) = requireNotNull("""
            SELECT COUNT(1) as antall FROM annullert_av_saksbehandler WHERE annullert_tidspunkt >= :fom
        """.single(mapOf("fom" to fom)) {it.int("antall")})

    private fun perStatistikktype(row: Row): Pair<BehandlingsstatistikkType, Int> {
        val oppgavetype: Oppgavetype = Oppgavetype.valueOf(row.string("type"))

        return if (oppgavetype != Oppgavetype.DELVIS_REFUSJON && oppgavetype != Oppgavetype.UTBETALING_TIL_SYKMELDT) {
            BehandlingsstatistikkType.valueOf(row.string("periodetype")) to row.int("antall")
        } else {  BehandlingsstatistikkType.valueOf(row.string("type")) to row.int("antallAvOppgaveType") }
    }
}

enum class BehandlingsstatistikkType {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON
}
