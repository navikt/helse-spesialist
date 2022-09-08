package no.nav.helse.spesialist.api.oppgave.experimental

import java.time.LocalDateTime
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao.Companion.saksbehandleroppgaveDto
import no.nav.helse.spesialist.api.oppgave.OppgaveForOversiktsvisningDto
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
                if (tilganger.harTilgangTilBeslutterOppgaver()) "" else "AND o.er_beslutter_oppgave = false"

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
        fra: LocalDateTime?,
        antall: Int
    ): List<PaginertOppgave> =
        sessionOf(dataSource).use { session ->
            val eventuellEkskluderingAvRiskQA =
                if (tilganger.harTilgangTilRiskOppgaver()) "" else "AND o.type != 'RISK_QA'"
            val gyldigeAdressebeskyttelser =
                if (tilganger.harTilgangTilKode7()) "AND pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig')"
                else "AND pi.adressebeskyttelse = 'Ugradert'"
            val eventuellEkskluderingAvBeslutterOppgaver =
                if (tilganger.harTilgangTilBeslutterOppgaver()) "" else "AND o.er_beslutter_oppgave = false"

            @Language("PostgreSQL")
            val query = """
            SELECT row_number() over (), o.id as oppgave_id, o.type AS oppgavetype, o.opprettet, o.er_beslutter_oppgave, o.er_retur_oppgave, o.totrinnsvurdering, o.tidligere_saksbehandler_oid , s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
                   pi.kjonn, pi.adressebeskyttelse, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, sot.inntektskilde, e.id AS enhet_id, e.navn AS enhet_navn, t.på_vent,
                   (SELECT COUNT(DISTINCT melding) from warning w where w.vedtak_ref = o.vedtak_ref and (w.inaktiv_fra is null or w.inaktiv_fra > now())) AS antall_varsler
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
                AND o.opprettet > :fra
            ORDER BY
                CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'STIKKPRØVE' THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'RISK_QA' THEN 0 ELSE 1 END,
                opprettet ASC
            LIMIT :antall
            ;
            """
            session.run(
                queryOf(query, mapOf("fra" to fra, "antall" to antall))
                    .map {
                        val oppgave = saksbehandleroppgaveDto(it)
                        val radnummer = it.int("row_number")
                        PaginertOppgave(oppgave, radnummer)
                    }
                    .asList
            )
        }
}

data class PaginertOppgave(
    val oppgave: OppgaveForOversiktsvisningDto,
    val radnummer: Int,
)
