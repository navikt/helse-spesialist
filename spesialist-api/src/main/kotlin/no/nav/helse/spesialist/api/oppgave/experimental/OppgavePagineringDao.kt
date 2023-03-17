package no.nav.helse.spesialist.api.oppgave.experimental

import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Sortering
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import org.intellij.lang.annotations.Language

class OppgavePagineringDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun getAntallOppgaver(tilganger: SaksbehandlerTilganger): Int =
        sessionOf(dataSource).use { session ->
            val eventuellEkskluderingAvRiskQA =
                if (tilganger.harTilgangTilRiskOppgaver()) "" else "AND o.type != 'RISK_QA'"
            val gyldigeAdressebeskyttelser =
                if (tilganger.harTilgangTilKode7()) "AND pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig')"
                else "AND pi.adressebeskyttelse = 'Ugradert'"
            val eventuellEkskluderingAvBeslutterOppgaver =
                if (tilganger.harTilgangTilBeslutterOppgaver()) "" else "AND o.er_beslutteroppgave = false"

            @Language("PostgreSQL")
            val query = """
            SELECT COUNT(o.id) as antall
            FROM oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                LEFT JOIN enhet e ON p.enhet_ref = e.id
                LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
                LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE status = 'AvventerSaksbehandler'::oppgavestatus
                $eventuellEkskluderingAvRiskQA
                $gyldigeAdressebeskyttelser
                $eventuellEkskluderingAvBeslutterOppgaver
            ;
            """
            session.run(queryOf(query).map { row -> row.int("antall") }.asSingle) ?: 0
        }

    fun finnOppgaver(
        tilganger: SaksbehandlerTilganger,
        antall: Int,
        side: Int,
        sortering: Sortering?,
    ): List<OppgaveForOversiktsvisning> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
            SELECT o.id as oppgave_id, o.type AS oppgavetype, o.opprettet, svg.opprettet_tidspunkt AS opprinneligSoknadsdato, o.er_beslutteroppgave, o.er_returoppgave, o.er_totrinnsoppgave, o.tidligere_saksbehandler_oid, o.sist_sendt, s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
                   pi.kjonn, pi.adressebeskyttelse, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, sot.inntektskilde, e.id AS enhet_id, e.navn AS enhet_navn, t.på_vent,
                   (SELECT COUNT(DISTINCT melding) from warning w where w.vedtak_ref = o.vedtak_ref and (w.inaktiv_fra is null or w.inaktiv_fra > now())) AS antall_varsler,
                   ttv.vedtaksperiode_id AS totrinnsvurdering_vedtaksperiode_id, ttv.saksbehandler, ttv.beslutter, ttv.er_retur
            FROM oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                INNER JOIN (
                    SELECT vedtaksperiode_id, min(opprettet_tidspunkt) AS opprettet_tidspunkt
                    FROM selve_vedtaksperiode_generasjon
                    GROUP BY vedtaksperiode_id
                ) svg ON svg.vedtaksperiode_id = v.vedtaksperiode_id
                LEFT JOIN enhet e ON p.enhet_ref = e.id
                LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
                LEFT JOIN saksbehandler s ON t.saksbehandler_ref = s.oid
                LEFT JOIN totrinnsvurdering ttv ON (ttv.vedtaksperiode_id = v.vedtaksperiode_id AND ttv.utbetaling_id_ref IS NULL)
            WHERE status = 'AvventerSaksbehandler'::oppgavestatus 
                AND CASE WHEN :harIkkeTilgangTilRisk THEN o.type <> 'RISK_QA' END
                AND CASE WHEN :harIkkeTilgangTilKode7 
                        THEN pi.adressebeskyttelse = 'Ugradert' 
                        ELSE pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig') END
                AND CASE WHEN :harIkkeTilgangTilBeslutter
                    THEN o.er_beslutteroppgave = false END
            ORDER BY
                CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'STIKKPRØVE' THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'RISK_QA' THEN 0 ELSE 1 END,
                opprettet ASC
            LIMIT :antall OFFSET :offset
            ;
            """
            val parameters = mapOf(
                "antall" to antall,
                "offset" to antall * (side - 1),
                "harIkkeTilgangTilRisk" to !tilganger.harTilgangTilRiskOppgaver(),
                "harIkkeTilgangTilKode7" to !tilganger.harTilgangTilKode7(),
                "harIkkeTilgangTilBeslutter" to !tilganger.harTilgangTilBeslutterOppgaver(),
            )
            session.run(
                queryOf(query, parameters)
                    .map(OppgaveApiDao::tilOppgaveForOversiktsvisning)
                    .asList
            )
        }
}
