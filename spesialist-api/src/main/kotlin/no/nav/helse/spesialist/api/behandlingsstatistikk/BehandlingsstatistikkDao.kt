package no.nav.helse.spesialist.api.behandlingsstatistikk

import java.time.LocalDate
import javax.sql.DataSource
import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language

class BehandlingsstatistikkDao(dataSource: DataSource) : HelseDao(dataSource) {

    fun getAntallTilgjengeligeBeslutteroppgaver(): Int {
        @Language("PostgreSQL")
        val query = """
            SELECT count(distinct o.id)
            FROM oppgave o
            WHERE o.status = 'AvventerSaksbehandler'
            AND o.er_beslutter_oppgave = true;
        """
        return query.single { it.int("count") } ?: 0
    }

    fun getAntallFullførteBeslutteroppgaver(fom: LocalDate): Int {
        @Language("PostgreSQL")
        val query = """
            SELECT count(distinct o.id)
            FROM oppgave o
            WHERE o.status = 'Ferdigstilt'
            AND o.er_beslutter_oppgave = true
            AND o.oppdatert >= :fom;
        """
        return query.single(mapOf("fom" to fom)) { it.int("count") } ?: 0
    }

    fun getAutomatiseringerPerInntektOgPeriodetype(fom: LocalDate): StatistikkPerInntektOgPeriodetype {
        @Language("PostgreSQL")
        val query = """
            SELECT s.type, s.inntektskilde, count(distinct a.id)
            FROM automatisering a
                     INNER JOIN saksbehandleroppgavetype s on s.vedtak_ref = a.vedtaksperiode_ref
            WHERE a.opprettet >= :fom
              AND a.automatisert = true
            GROUP BY s.type, s.inntektskilde;
        """.trimIndent()

        return getStatistikkPerInntektOgPeriodetype(query, mapOf("fom" to fom))
    }

    fun getTilgjengeligeOppgaverPerInntektOgPeriodetype(): StatistikkPerInntektOgPeriodetype {
        @Language("PostgreSQL")
        val query = """
            SELECT s.type, s.inntektskilde, count(distinct o.id)
            FROM oppgave o
                     INNER JOIN saksbehandleroppgavetype s on o.vedtak_ref = s.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type, s.inntektskilde;
        """.trimIndent()

        return getStatistikkPerInntektOgPeriodetype(query)
    }

    fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate): StatistikkPerInntektOgPeriodetype {
        @Language("PostgreSQL")
        val query = """
            SELECT s.type, s.inntektskilde, count(distinct o.id)
            FROM oppgave o
                     INNER JOIN saksbehandleroppgavetype s on o.vedtak_ref = s.vedtak_ref
            WHERE o.status = 'Ferdigstilt'
              AND o.oppdatert >= :fom
              AND o.type = 'SØKNAD'
            GROUP BY s.type, s.inntektskilde;
        """.trimIndent()

        return getStatistikkPerInntektOgPeriodetype(query, mapOf("fom" to fom))
    }

    fun getManueltUtførteOppgaverPerOppgavetype(fom: LocalDate): Map<Oppgavetype, Int> {
        @Language("PostgreSQL")
        val query = """
            SELECT o.type, count(distinct o.id)
            FROM oppgave o
            WHERE o.status = 'Ferdigstilt'
              AND o.oppdatert >= :fom
              AND o.type <> 'SØKNAD'
            GROUP BY o.type;
        """.trimIndent()

        return query
            .list(mapOf("fom" to fom)) { mapOf(Oppgavetype.valueOf(it.string("type")) to it.int("count")) }
            .reduce(Map<Oppgavetype, Int>::plus)
    }

    fun getTilgjengeligeOppgaverPerOppgavetype(): Map<Oppgavetype, Int> {
        @Language("PostgreSQL")
        val query = """
            SELECT o.type, count(distinct o.id)
            FROM oppgave o
            WHERE o.status = 'AvventerSaksbehandler'
              AND o.type <> 'SØKNAD'
            GROUP BY o.type;
        """.trimIndent()

        return query
            .list { mapOf(Oppgavetype.valueOf(it.string("type")) to it.int("count")) }
            .reduce(Map<Oppgavetype, Int>::plus)
    }

    private fun getStatistikkPerInntektOgPeriodetype(
        query: String,
        paramMap: Map<String, Any> = emptyMap()
    ): StatistikkPerInntektOgPeriodetype {
        val rader = query.list(paramMap) {
            InntektOgPeriodetyperad(
                inntekttype = Inntektskilde.valueOf(it.string("inntektskilde")),
                periodetype = Periodetype.valueOf(it.string("type")),
                antall = it.int("count")
            )
        }

        val perInntekttype = Inntektskilde.values().map { inntektskilde ->
            mapOf(inntektskilde to rader.filter { it.inntekttype == inntektskilde }.sumOf { it.antall })
        }.fold(emptyMap(), Map<Inntektskilde, Int>::plus)

        val perPeriodetype = Periodetype.values().map { periodetype ->
            mapOf(periodetype to rader.filter { it.periodetype == periodetype }.sumOf { it.antall })
        }.fold(emptyMap(), Map<Periodetype, Int>::plus)

        return StatistikkPerInntektOgPeriodetype(
            perInntekttype = perInntekttype,
            perPeriodetype = perPeriodetype,
        )
    }

    fun oppgavestatistikk(fom: LocalDate = LocalDate.now()): BehandlingsstatistikkDto {

        val godkjentManueltPerPeriodetype = godkjentManueltPerPeriodetype(fom)
        val tilGodkjenningPerPeriodetype = tilGodkjenningPerPeriodetype()
        val tildeltPerPeriodetype = tildeltPerPeriodetype()

        val godkjentManueltTotalt = godkjentManueltPerPeriodetype(fom).sumOf { (_, antall) -> antall }
        val annulleringerTotalt = antallAnnulleringer(fom)
        val godkjentAutomatiskTotalt = godkjentAutomatiskTotalt(fom)
        val oppgaverTilGodkjenningTotalt = tilGodkjenningPerPeriodetype.sumOf { (_, antall) -> antall }
        val tildelteOppgaverTotalt = tildeltPerPeriodetype.sumOf { (_, antall) -> antall }

        val behandletTotalt = annulleringerTotalt + godkjentManueltTotalt + godkjentAutomatiskTotalt

        return BehandlingsstatistikkDto(
            oppgaverTilGodkjenning = BehandlingsstatistikkDto.OppgavestatistikkDto(
                totalt = oppgaverTilGodkjenningTotalt,
                perPeriodetype = tilGodkjenningPerPeriodetype,
            ),
            tildelteOppgaver = BehandlingsstatistikkDto.OppgavestatistikkDto(
                totalt = tildelteOppgaverTotalt,
                perPeriodetype = tildeltPerPeriodetype
            ),
            fullførteBehandlinger = BehandlingsstatistikkDto.BehandlingerDto(
                annullert = annulleringerTotalt,
                manuelt = BehandlingsstatistikkDto.OppgavestatistikkDto(
                    totalt = godkjentManueltTotalt,
                    perPeriodetype = godkjentManueltPerPeriodetype
                ),
                automatisk = godkjentAutomatiskTotalt,
                totalt = behandletTotalt
            )
        )
    }

    private fun tilGodkjenningPerPeriodetype() =
        """ SELECT sot.type AS periodetype, o.type, COUNT(distinct o.id)
            FILTER (WHERE o.type = 'SØKNAD') AS antall,
            COUNT(distinct o.id) as antallAvOppgaveType
            FROM oppgave o
              INNER JOIN saksbehandleroppgavetype sot ON o.vedtak_ref = sot.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY sot.type, o.type
        """.list { perStatistikktype(it) }

    private fun tildeltPerPeriodetype() =
        """ SELECT s.type as periodetype, o.type,
            COUNT(distinct o.id) FILTER (WHERE o.type = 'SØKNAD') AS antall,
            COUNT(distinct o.id) as antallAvOppgaveType
            FROM oppgave o
              INNER JOIN vedtak v on o.vedtak_ref = v.id
              INNER JOIN saksbehandleroppgavetype s on v.id = s.vedtak_ref
              INNER JOIN tildeling t on o.id = t.oppgave_id_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type, o.type
        """.list { perStatistikktype(it) }

    private fun godkjentManueltPerPeriodetype(fom: LocalDate) =
        """ SELECT sot.type AS periodetype, o.type,
            COUNT(distinct o.id) FILTER (WHERE o.type = 'SØKNAD') AS antall,
            COUNT(distinct o.id) as antallAvOppgaveType
            FROM oppgave o
              INNER JOIN saksbehandleroppgavetype sot ON o.vedtak_ref = sot.vedtak_ref
            WHERE o.status = 'Ferdigstilt' AND o.oppdatert >= :fom
            GROUP BY sot.type, o.type
        """.list(mapOf("fom" to fom)) { perStatistikktype(it) }

    private fun godkjentAutomatiskTotalt(fom: LocalDate) = requireNotNull(
        """ SELECT COUNT(1) as antall
            FROM automatisering a
                INNER JOIN vedtak v on a.vedtaksperiode_ref = v.id
            WHERE a.automatisert = true 
            AND a.stikkprøve = false 
            AND a.opprettet >= :fom
            AND (a.inaktiv_fra IS NULL OR a.inaktiv_fra > now()) 
        """.single(mapOf("fom" to fom)) { it.int("antall") })

    private fun antallAnnulleringer(fom: LocalDate) = requireNotNull("""
            SELECT COUNT(1) as antall
            FROM annullert_av_saksbehandler
            WHERE annullert_tidspunkt >= :fom
        """.single(mapOf("fom" to fom)) { it.int("antall") })

    private fun perStatistikktype(row: Row): Pair<BehandlingsstatistikkType, Int> {
        val oppgavetype: Oppgavetype = Oppgavetype.valueOf(row.string("type"))

        return if (oppgavetype == Oppgavetype.SØKNAD) {
            BehandlingsstatistikkType.valueOf(row.string("periodetype")) to row.int("antall")
        } else {
            BehandlingsstatistikkType.valueOf(row.string("type")) to row.int("antallAvOppgaveType")
        }
    }
}

enum class BehandlingsstatistikkType {
    FØRSTEGANGSBEHANDLING,
    FORLENGELSE,
    INFOTRYGDFORLENGELSE,
    OVERGANG_FRA_IT,
    STIKKPRØVE,
    RISK_QA,
    REVURDERING,
    FORTROLIG_ADRESSE,
    UTBETALING_TIL_SYKMELDT,
    DELVIS_REFUSJON
}
