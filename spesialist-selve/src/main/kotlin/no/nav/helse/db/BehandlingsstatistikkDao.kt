package no.nav.helse.db

import java.time.LocalDate
import javax.sql.DataSource
import kotliquery.Query
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.behandlingsstatistikk.InntektOgPeriodetyperad
import no.nav.helse.spesialist.api.behandlingsstatistikk.StatistikkPerInntektOgPeriodetype
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Mottakertype
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

class BehandlingsstatistikkDao(dataSource: DataSource) : HelseDao(dataSource) {

    fun getAntallTilgjengeligeBeslutteroppgaver() = asSQL(
        """
            SELECT count(1)
            FROM totrinnsvurdering t
            INNER JOIN vedtak v ON v.vedtaksperiode_id = t.vedtaksperiode_id
            INNER JOIN oppgave o ON v.id = o.vedtak_ref
            WHERE o.status='AvventerSaksbehandler'::oppgavestatus
              AND v.forkastet = false 
              AND o.egenskaper @> ARRAY['BESLUTTER']::VARCHAR[]
        """
    ).single { it.int("count") } ?: 0

    fun getAntallTilgjengeligeEgenAnsattOppgaver() = asSQL(
        """
            SELECT count(1)
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE o.status='AvventerSaksbehandler'::oppgavestatus
              AND v.forkastet = false 
              AND o.egenskaper @> ARRAY['EGEN_ANSATT']::VARCHAR[]
        """
    ).single { it.int("count") } ?: 0

    fun getAntallManueltFullførteEgenAnsattOppgaver(fom: LocalDate) = asSQL(
        """
            SELECT count(1)
            FROM oppgave
            WHERE status='Ferdigstilt'::oppgavestatus
              AND oppdatert >= :fom
              AND egenskaper @> ARRAY['EGEN_ANSATT']::VARCHAR[]
        """, mapOf("fom" to fom)
    ).single { it.int("count") } ?: 0

    fun getAntallFullførteBeslutteroppgaver(fom: LocalDate) = asSQL(
        """
            SELECT count(1)
            FROM totrinnsvurdering
            WHERE utbetaling_id_ref IS NOT NULL
            AND oppdatert >= :fom
        """, mapOf("fom" to fom)
    ).single { it.int("count") } ?: 0

    fun getAutomatiseringerPerInntektOgPeriodetype(fom: LocalDate): StatistikkPerInntektOgPeriodetype {
        val query = asSQL("""
            SELECT s.type,
                s.inntektskilde,
                CASE WHEN ui.arbeidsgiverbeløp > 0 AND ui.personbeløp > 0 THEN '${Mottakertype.BEGGE}'
                    WHEN ui.personbeløp > 0 THEN '${Mottakertype.SYKMELDT}'
                    ELSE '${Mottakertype.ARBEIDSGIVER}'
                END AS mottakertype,
                count(distinct a.id)
            FROM automatisering a
                     INNER JOIN saksbehandleroppgavetype s on s.vedtak_ref = a.vedtaksperiode_ref
                     INNER JOIN vedtak v ON v.id = a.vedtaksperiode_ref
                     INNER JOIN vedtaksperiode_utbetaling_id vui on vui.vedtaksperiode_id = v.vedtaksperiode_id 
                     INNER JOIN utbetaling_id ui on ui.utbetaling_id = vui.utbetaling_id
            WHERE a.opprettet >= :fom
              AND a.automatisert = true
            GROUP BY s.type, s.inntektskilde, mottakertype;
        """, mapOf("fom" to fom))

        return getStatistikkPerInntektOgPeriodetype(query, inkluderMottakertype = true)
    }

    fun getTilgjengeligeOppgaverPerInntektOgPeriodetype(): StatistikkPerInntektOgPeriodetype {
        val query = asSQL("""
            SELECT s.type, s.inntektskilde, count(distinct o.id)
            FROM oppgave o
                     INNER JOIN saksbehandleroppgavetype s on o.vedtak_ref = s.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type, s.inntektskilde;
        """)

        return getStatistikkPerInntektOgPeriodetype(query)
    }

    fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate): StatistikkPerInntektOgPeriodetype {
        val query = asSQL("""
            SELECT s.type, s.inntektskilde, count(distinct o.id)
            FROM oppgave o
                     INNER JOIN saksbehandleroppgavetype s on o.vedtak_ref = s.vedtak_ref
            WHERE o.status = 'Ferdigstilt'
              AND o.oppdatert >= :fom
            GROUP BY s.type, s.inntektskilde;
        """, mapOf("fom" to fom))

        return getStatistikkPerInntektOgPeriodetype(query)
    }

    fun antallTilgjengeligeOppgaverFor(egenskap: EgenskapForDatabase): Int {
        return asSQL("""
            SELECT count(distinct o.id) FROM oppgave o 
            WHERE o.status = 'AvventerSaksbehandler'
            AND o.egenskaper @> ARRAY[:egenskap]::varchar[]
            """.trimIndent(),
           mapOf(
               "egenskap" to egenskap.name,
           )
        ).single { it.int(1) } ?: 0
    }

    fun antallFerdigstilteOppgaverFor(egenskap: EgenskapForDatabase, fom: LocalDate): Int {
        return asSQL("""
            SELECT count(distinct o.id) FROM oppgave o 
            WHERE o.status = 'Ferdigstilt'
            AND o.oppdatert >= :fom
            AND o.egenskaper @> ARRAY[:egenskap]::varchar[]
            """.trimIndent(),
           mapOf(
               "egenskap" to egenskap.name,
               "fom" to fom
           )
        ).single { it.int(1) } ?: 0
    }

    private fun getStatistikkPerInntektOgPeriodetype(
        query: Query,
        inkluderMottakertype: Boolean = false
    ): StatistikkPerInntektOgPeriodetype {
        val rader = query.list {
            InntektOgPeriodetyperad(
                inntekttype = Inntektskilde.valueOf(it.string("inntektskilde")),
                periodetype = Periodetype.valueOf(it.string("type")),
                mottakertype = if (inkluderMottakertype) Mottakertype.valueOf(it.string("mottakertype")) else null,
                antall = it.int("count")
            )
        }

        val perInntekttype = Inntektskilde.entries.map { inntektskilde ->
            mapOf(inntektskilde to rader.filter { it.inntekttype == inntektskilde }.sumOf { it.antall })
        }.fold(emptyMap(), Map<Inntektskilde, Int>::plus)

        val perPeriodetype = Periodetype.entries.map { periodetype ->
            mapOf(periodetype to rader.filter { it.periodetype == periodetype }.sumOf { it.antall })
        }.fold(emptyMap(), Map<Periodetype, Int>::plus)

        val perMottakertype = if (inkluderMottakertype) {
            Mottakertype.entries.map { mottakertype ->
                mapOf(mottakertype to rader.filter { it.mottakertype == mottakertype }.sumOf { it.antall })
            }.fold(emptyMap(), Map<Mottakertype, Int>::plus)
        } else emptyMap()

        return StatistikkPerInntektOgPeriodetype(
            perInntekttype = perInntekttype,
            perPeriodetype = perPeriodetype,
            perMottakertype = perMottakertype,
        )
    }

    fun getAntallAnnulleringer(fom: LocalDate) = asSQL(
        """
            SELECT count(distinct u.id) as annulleringer
            FROM utbetaling u
            WHERE u.status = 'ANNULLERT'
              AND u.opprettet >= :fom;
        """, mapOf("fom" to fom)
    ).single { it.int("annulleringer") } ?: 0
}

