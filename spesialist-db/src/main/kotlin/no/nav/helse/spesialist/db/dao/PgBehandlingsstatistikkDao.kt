package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.BehandlingsstatistikkDao
import no.nav.helse.db.BehandlingsstatistikkDao.StatistikkPerKombinasjon
import no.nav.helse.db.BehandlingsstatistikkDao.StatistikkPerKombinasjon.Mottakertype
import no.nav.helse.db.BehandlingsstatistikkDao.StatistikkPerKombinasjon.Utbetalingtype
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.HelseDao
import java.time.LocalDate
import javax.sql.DataSource

class PgBehandlingsstatistikkDao internal constructor(dataSource: DataSource) :
    HelseDao(dataSource),
    BehandlingsstatistikkDao {
        override fun getAntallTilgjengeligeBeslutteroppgaver() =
            asSQL(
                """
            SELECT count(1)
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE o.status='AvventerSaksbehandler'::oppgavestatus
              AND v.forkastet = false 
              AND o.egenskaper @> ARRAY['BESLUTTER']::VARCHAR[]
        """,
            ).single { it.int("count") } ?: 0

        override fun getAntallTilgjengeligeEgenAnsattOppgaver() =
            asSQL(
                """
            SELECT count(1)
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE o.status='AvventerSaksbehandler'::oppgavestatus
              AND v.forkastet = false 
              AND o.egenskaper @> ARRAY['EGEN_ANSATT']::VARCHAR[]
        """,
            ).single { it.int("count") } ?: 0

        override fun getAntallManueltFullførteEgenAnsattOppgaver(fom: LocalDate) =
            asSQL(
                """
                SELECT count(1)
                FROM oppgave
                WHERE status='Ferdigstilt'::oppgavestatus
                  AND oppdatert >= :fom
                  AND egenskaper @> ARRAY['EGEN_ANSATT']::VARCHAR[]
                """.trimIndent(),
                "fom" to fom,
            ).single { it.int("count") } ?: 0

        override fun getAntallFullførteBeslutteroppgaver(fom: LocalDate) =
            asSQL(
                """
                SELECT count(1)
                FROM totrinnsvurdering
                WHERE utbetaling_id_ref IS NOT NULL
                AND oppdatert >= :fom
                """.trimIndent(),
                "fom" to fom,
            ).single { it.int("count") } ?: 0

        override fun getAutomatiseringPerKombinasjon(fom: LocalDate): StatistikkPerKombinasjon {
            val rader =
                asSQL(
                    """
                    SELECT s.type,
                        s.inntektskilde,
                        CASE WHEN ui.arbeidsgiverbeløp > 0 AND ui.personbeløp > 0 THEN '${Mottakertype.BEGGE}'
                            WHEN ui.personbeløp > 0 THEN '${Mottakertype.SYKMELDT}'
                            ELSE '${Mottakertype.ARBEIDSGIVER}'
                        END AS mottakertype,
                        ui.type AS utbetaling_type,
                        count(distinct a.id)
                    FROM automatisering a
                             INNER JOIN saksbehandleroppgavetype s on s.vedtak_ref = a.vedtaksperiode_ref
                             INNER JOIN vedtak v ON v.id = a.vedtaksperiode_ref
                             INNER JOIN vedtaksperiode_utbetaling_id vui on vui.vedtaksperiode_id = v.vedtaksperiode_id 
                             INNER JOIN utbetaling_id ui on ui.utbetaling_id = vui.utbetaling_id
                    WHERE a.opprettet >= :fom
                      AND a.automatisert = true
                    GROUP BY s.type, s.inntektskilde, mottakertype, utbetaling_type;
                    """.trimIndent(),
                    "fom" to fom,
                )
                    .list {
                        AntallPerKombinasjonRad(
                            inntekttype = Inntektskilde.valueOf(it.string("inntektskilde")),
                            periodetype = Periodetype.valueOf(it.string("type")),
                            mottakertype = Mottakertype.valueOf(it.string("mottakertype")),
                            utbetalingtype = Utbetalingtype.valueOf(it.string("utbetaling_type")),
                            antall = it.int("count"),
                        )
                    }

            return getStatistikkPerInntektOgPeriodetype(rader)
        }

        override fun getTilgjengeligeOppgaverPerInntektOgPeriodetype(): StatistikkPerKombinasjon {
            val rader =
                asSQL(
                    """
            SELECT s.type, s.inntektskilde, count(distinct o.id)
            FROM oppgave o
                     INNER JOIN saksbehandleroppgavetype s on o.vedtak_ref = s.vedtak_ref
            WHERE o.status = 'AvventerSaksbehandler'
            GROUP BY s.type, s.inntektskilde;
        """,
                )
                    .list {
                        AntallPerKombinasjonRad(
                            inntekttype = Inntektskilde.valueOf(it.string("inntektskilde")),
                            periodetype = Periodetype.valueOf(it.string("type")),
                            mottakertype = null,
                            utbetalingtype = null,
                            antall = it.int("count"),
                        )
                    }

            return getStatistikkPerInntektOgPeriodetype(rader)
        }

        override fun getManueltUtførteOppgaverPerInntektOgPeriodetype(fom: LocalDate): StatistikkPerKombinasjon {
            val rader =
                asSQL(
                    """
                    SELECT s.type, s.inntektskilde, count(distinct o.id)
                    FROM oppgave o
                             INNER JOIN saksbehandleroppgavetype s on o.vedtak_ref = s.vedtak_ref
                    WHERE o.status = 'Ferdigstilt'
                      AND o.oppdatert >= :fom
                    GROUP BY s.type, s.inntektskilde;
                    """.trimIndent(),
                    "fom" to fom,
                )
                    .list {
                        AntallPerKombinasjonRad(
                            inntekttype = Inntektskilde.valueOf(it.string("inntektskilde")),
                            periodetype = Periodetype.valueOf(it.string("type")),
                            mottakertype = null,
                            utbetalingtype = null,
                            antall = it.int("count"),
                        )
                    }

            return getStatistikkPerInntektOgPeriodetype(rader)
        }

        override fun antallTilgjengeligeOppgaverFor(egenskap: EgenskapForDatabase): Int {
            return asSQL(
                """
                SELECT count(distinct o.id) FROM oppgave o 
                WHERE o.status = 'AvventerSaksbehandler'
                AND o.egenskaper @> ARRAY[:egenskap]::varchar[]
                """.trimIndent(),
                "egenskap" to egenskap.name,
            ).single { it.int(1) } ?: 0
        }

        override fun antallFerdigstilteOppgaverFor(
            egenskap: EgenskapForDatabase,
            fom: LocalDate,
        ): Int {
            return asSQL(
                """
                SELECT count(distinct o.id) FROM oppgave o 
                WHERE o.status = 'Ferdigstilt'
                AND o.oppdatert >= :fom
                AND o.egenskaper @> ARRAY[:egenskap]::varchar[]
                """.trimIndent(),
                "egenskap" to egenskap.name,
                "fom" to fom,
            ).single { it.int(1) } ?: 0
        }

        private fun getStatistikkPerInntektOgPeriodetype(rader: List<AntallPerKombinasjonRad>): StatistikkPerKombinasjon {
            val perInntekttype =
                Inntektskilde.entries.associateWith { inntekttype ->
                    rader.filter { it.inntekttype == inntekttype }.sumOf { it.antall }
                }

            val perPeriodetype =
                Periodetype.entries.associateWith { periodetype ->
                    rader.filter { it.periodetype == periodetype }.sumOf { it.antall }
                }

            val perMottakertype =
                if (rader.none { it.mottakertype == null }) {
                    Mottakertype.entries.associateWith { mottakertype ->
                        rader.filter { it.mottakertype == mottakertype }.sumOf { it.antall }
                    }
                } else {
                    emptyMap()
                }

            val perUtbetalingtype =
                if (rader.none { it.utbetalingtype == null }) {
                    Utbetalingtype.entries.associateWith { utbetalingtype ->
                        rader.filter { it.utbetalingtype == utbetalingtype }.sumOf { it.antall }
                    }
                } else {
                    emptyMap()
                }

            return StatistikkPerKombinasjon(
                perInntekttype = perInntekttype,
                perPeriodetype = perPeriodetype,
                perMottakertype = perMottakertype,
                perUtbetalingtype = perUtbetalingtype,
            )
        }

        override fun getAntallAnnulleringer(fom: LocalDate) =
            asSQL(
                """
                SELECT count(distinct u.id) as annulleringer
                FROM utbetaling u
                WHERE u.status = 'ANNULLERT'
                  AND u.opprettet >= :fom;
                """.trimIndent(),
                "fom" to fom,
            ).single { it.int("annulleringer") } ?: 0

        override fun getAntallAvvisninger(fom: LocalDate) =
            asSQL(
                """
                SELECT count(*) as avvisninger
                FROM vedtak v
                WHERE id IN (
                    SELECT vedtak_ref
                    FROM oppgave
                    WHERE status = 'Ferdigstilt' AND oppdatert::DATE = :fom)
                AND forkastet = True;
                """.trimIndent(),
                "fom" to fom,
            ).single { it.int("avvisninger") } ?: 0

        private data class AntallPerKombinasjonRad(
            val inntekttype: Inntektskilde,
            val periodetype: Periodetype,
            val mottakertype: Mottakertype?,
            val utbetalingtype: Utbetalingtype?,
            val antall: Int,
        )
    }
