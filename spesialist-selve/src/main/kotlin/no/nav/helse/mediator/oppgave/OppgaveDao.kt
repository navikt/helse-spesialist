package no.nav.helse.mediator.oppgave

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.db.OppgaveFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommandData
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSystem
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Invalidert
import no.nav.helse.spesialist.api.oppgave.Oppgavetype

interface OppgaveRepository {
    fun finnOppgave(id: Long): OppgaveFraDatabase?
    fun finnHendelseId(id: Long): UUID
}

class OppgaveDao(dataSource: DataSource) : HelseDao(dataSource), OppgaveRepository {

    fun reserverNesteId(): Long {
        return asSQL(
            """
               SELECT nextval(pg_get_serial_sequence('oppgave', 'id')) as neste_id; 
            """
        ).single { it.long("neste_id") } ?: throw IllegalStateException("Klarer ikke hente neste id i sekvens fra oppgave-tabellen")
    }

    override fun finnOppgave(id: Long): OppgaveFraDatabase? {
        return asSQL(
            """ 
            SELECT o.type, o.status, v.vedtaksperiode_id, o.ferdigstilt_av, o.ferdigstilt_av_oid, o.utbetaling_id, s.navn, s.epost, s.ident, s.oid, t.på_vent
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            LEFT JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE o.id = :oppgaveId
            ORDER BY o.id DESC LIMIT 1
        """, mapOf("oppgaveId" to id)
        ).single { row ->
            OppgaveFraDatabase(
                id = id,
                type = row.string("type"),
                status = row.string("status"),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                utbetalingId = row.uuid("utbetaling_id"),
                hendelseId = finnHendelseId(id),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                tildelt = row.uuidOrNull("oid")?.let {
                    SaksbehandlerFraDatabase(
                        epostadresse = row.string("epost"),
                        oid = it,
                        navn = row.string("navn"),
                        ident = row.string("ident")
                    )
                },
                påVent = row.boolean("på_vent"),
            )
        }
    }

    internal fun hentOppgavemelding(oppgaveId: Long): Oppgavemelder.Oppgavemelding? = asSQL(
        """
            SELECT DISTINCT hendelse_id, context_id, o.id as oppgave_id, status, type, beslutter, er_retur, ferdigstilt_av, ferdigstilt_av_oid, t.på_vent
            FROM oppgave o
            INNER JOIN command_context cc on cc.context_id = o.command_context_id
            INNER JOIN vedtaksperiode_utbetaling_id vui on o.utbetaling_id = vui.utbetaling_id
            LEFT JOIN totrinnsvurdering ttv on vui.vedtaksperiode_id = ttv.vedtaksperiode_id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            WHERE o.id = :oppgaveId
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """,
        mapOf("oppgaveId" to oppgaveId)
    ).single {
        Oppgavemelder.Oppgavemelding(
            hendelseId = it.uuid("hendelse_id"),
            oppgaveId = it.long("oppgave_id"),
            status = it.string("status"),
            type = it.string("type"),
            beslutter = it.uuidOrNull("beslutter"),
            erRetur = it.boolean("er_retur"),
            ferdigstiltAvIdent = it.stringOrNull("ferdigstilt_av"),
            ferdigstiltAvOid = it.uuidOrNull("ferdigstilt_av_oid"),
            påVent = it.boolean("på_vent")
        )
    }

    fun finnUtbetalingId(oppgaveId: Long) = asSQL(
        " SELECT utbetaling_id FROM oppgave WHERE id = :oppgaveId; ",
        mapOf("oppgaveId" to oppgaveId)
    ).single { it.uuid("utbetaling_id") }

    fun finnNyesteOppgaveId(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            ORDER BY opprettet DESC
            LIMIT 1
        """, mapOf("vedtaksperiodeId" to vedtaksperiodeId)
        ).single { it.long("id") }

    fun erRiskoppgave(oppgaveId: Long): Boolean =
        asSQL(
            """ SELECT 1 FROM oppgave
            WHERE id=:oppgaveId
            AND type = 'RISK_QA'
        """, mapOf("oppgaveId" to oppgaveId)
        ).single { true } ?: false

    fun finnOppgaveIdUansettStatus(fødselsnummer: String) =
        asSQL(
            """ SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            ORDER BY o.id DESC
            LIMIT 1;
        """, mapOf("fodselsnummer" to fødselsnummer.toLong())
        ).single { it.long("oppgaveId") }!!

    fun finnGodkjenningsbehov(fødselsnummer: String) =
        asSQL(
            """ SELECT c.hendelse_id as hendelse_id
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
                     JOIN command_context c ON o.command_context_id = c.context_id
            WHERE p.fodselsnummer = :fodselsnummer
            AND status = 'AvventerSaksbehandler'::oppgavestatus
            GROUP BY c.hendelse_id;
        """, mapOf("fodselsnummer" to fødselsnummer.toLong())
        ).single { it.uuid("hendelse_id") }!!

    fun finnVedtaksperiodeId(fødselsnummer: String) =
        asSQL(
            """ SELECT v.vedtaksperiode_id as vedtaksperiode_id
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            AND status = 'AvventerSaksbehandler'::oppgavestatus;
        """, mapOf("fodselsnummer" to fødselsnummer.toLong())
        ).single { it.uuid("vedtaksperiode_id") }!!

    fun finnOppgaveId(fødselsnummer: String) =
        asSQL(
            """ SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
              AND p.fodselsnummer = :fodselsnummer;
        """, mapOf("fodselsnummer" to fødselsnummer.toLong())
        ).single { it.long("oppgaveId") }

    fun finnOppgaveId(utbetalingId: UUID) =
        asSQL(
            """ SELECT o.id as oppgaveId
            FROM oppgave o WHERE o.utbetaling_id = :utbetaling_id
        """, mapOf("utbetaling_id" to utbetalingId)
        ).single { it.long("oppgaveId") }

    fun finn(oppgaveId: Long) =
        asSQL(
            """ 
            SELECT o.type, o.status, v.vedtaksperiode_id, o.ferdigstilt_av, o.ferdigstilt_av_oid, o.utbetaling_id, s.navn, s.epost, s.ident, s.oid, t.på_vent
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            LEFT JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE o.id = :oppgaveId
        """, mapOf("oppgaveId" to oppgaveId)
        ).single { row ->
            Oppgave(
                id = oppgaveId,
                type = enumValueOf(row.string("type")),
                tilstand = tilstand(enumValueOf(row.string("status"))),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                utbetalingId = row.uuid("utbetaling_id"),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                tildelt = row.uuidOrNull("oid")?.let {
                    Saksbehandler(
                        epostadresse = row.string("epost"),
                        oid = it,
                        navn = row.string("navn"),
                        ident = row.string("ident")
                    )
                },
                påVent = row.boolean("på_vent"),
                hendelseId = finnHendelseId(oppgaveId)
            )
        }

    private fun tilstand(oppgavestatus: Oppgavestatus): Oppgave.Tilstand {
        return when (oppgavestatus) {
            AvventerSaksbehandler -> Oppgave.AvventerSaksbehandler
            AvventerSystem -> Oppgave.AvventerSystem
            Ferdigstilt -> Oppgave.Ferdigstilt
            Invalidert -> Oppgave.Invalidert
        }
    }

    fun finnAktiv(vedtaksperiodeId: UUID) =
        asSQL(
            """ SELECT o.id, o.type, o.status, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)
        """, mapOf("vedtaksperiodeId" to vedtaksperiodeId)
        ).single { row ->
            val id = row.long("id")
            Oppgave(
                id = id,
                type = enumValueOf(row.string("type")),
                tilstand = tilstand(enumValueOf(row.string("status"))),
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = row.uuid("utbetaling_id"),
                hendelseId = finnHendelseId(id)
            )
        }

    fun finnVedtaksperiodeId(oppgaveId: Long) = requireNotNull(
        asSQL(
            """ SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """, mapOf("oppgaveId" to oppgaveId)
        ).single { row -> row.uuid("vedtaksperiode_id") })

    private fun finnArbeidsgiverbeløpOgPersonbeløp(vedtaksperiodeId: UUID, utbetalingId: UUID) = requireNotNull(
        asSQL(
            """ SELECT SUM(ABS(arbeidsgiverbeløp)) as sumArbeidsgiverbeløp, SUM(ABS(personbeløp)) as sumPersonbeløp
            FROM utbetaling_id 
            WHERE person_ref=(SELECT person_ref FROM utbetaling_id WHERE utbetaling_id=:utbetalingId) AND 
                utbetaling_id.utbetaling_id IN (
                    SELECT utbetaling_id 
                    FROM selve_vedtaksperiode_generasjon 
                    WHERE skjæringstidspunkt=(
                        SELECT skjæringstidspunkt 
                            FROM selve_vedtaksperiode_generasjon
                            WHERE vedtaksperiode_id=:vedtaksperiodeId AND tilstand='Ulåst'
                        ) AND tilstand='Ulåst'
                    )
        """, mapOf(
                "utbetalingId" to utbetalingId,
                "vedtaksperiodeId" to vedtaksperiodeId
            )
        ).single { row ->
            Pair(row.intOrNull("sumArbeidsgiverbeløp") ?: 0, row.intOrNull("sumPersonbeløp") ?: 0)
        })

    private fun finnMottaker(harArbeidsgiverbeløp: Boolean, harPersonbeløp: Boolean): Mottaker? {
        return when {
            harArbeidsgiverbeløp && harPersonbeløp -> Mottaker.BEGGE
            harPersonbeløp -> Mottaker.SYKMELDT
            harArbeidsgiverbeløp -> Mottaker.ARBEIDSGIVER
            else -> null
        }
    }

    fun opprettOppgave(id: Long, commandContextId: UUID, oppgavetype: Oppgavetype, vedtaksperiodeId: UUID, utbetalingId: UUID) =
        requireNotNull(run {
            val vedtakRef = vedtakRef(vedtaksperiodeId)

            val (arbeidsgiverBeløp, personBeløp) = finnArbeidsgiverbeløpOgPersonbeløp(vedtaksperiodeId, utbetalingId)
            val mottaker = finnMottaker(arbeidsgiverBeløp > 0, personBeløp > 0)

            asSQL(
                """
                    INSERT INTO oppgave(id, oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id, mottaker)
                    VALUES (:id, now(), CAST(:oppgavetype as oppgavetype), CAST(:oppgavestatus as oppgavestatus), :ferdigstiltAv, :ferdigstiltAvOid, :vedtakRef, :commandContextId, :utbetalingId, CAST(:mottaker as mottakertype));
                """, mapOf(
                    "id" to id,
                    "oppgavetype" to oppgavetype.name,
                    "oppgavestatus" to AvventerSaksbehandler.name,
                    "ferdigstiltAv" to null,
                    "ferdigstiltAvOid" to null,
                    "vedtakRef" to vedtakRef,
                    "commandContextId" to commandContextId,
                    "utbetalingId" to utbetalingId,
                    "mottaker" to mottaker?.name
                )
            ).updateAndReturnGeneratedKey()
        }) { "Kunne ikke opprette oppgave" }

    private fun vedtakRef(vedtaksperiodeId: UUID) = requireNotNull(asSQL(
        "SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId;",
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { it.long("id") }) { "Kunne ikke finne vedtak for vedtaksperiodeId $vedtaksperiodeId" }

    fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String? = null,
        oid: UUID? = null,
    ) = asSQL(
        """
            UPDATE oppgave
            SET ferdigstilt_av = :ferdigstiltAv, ferdigstilt_av_oid = :oid, status = :oppgavestatus::oppgavestatus
            WHERE id=:oppgaveId; 
        """, mapOf(
            "ferdigstiltAv" to ferdigstiltAv,
            "oid" to oid,
            "oppgavestatus" to oppgavestatus.name,
            "oppgaveId" to oppgaveId
        )
    ).update()

    override fun finnHendelseId(id: Long) = requireNotNull(
        asSQL(
            """
                SELECT DISTINCT hendelse_id 
                FROM command_context 
                WHERE context_id = (SELECT command_context_id FROM oppgave WHERE id = :oppgaveId);
            """,
            mapOf("oppgaveId" to id)
        )
            .single { row -> row.uuid("hendelse_id") })

    fun harGyldigOppgave(utbetalingId: UUID) = requireNotNull(
        asSQL(
            """ SELECT COUNT(1) AS oppgave_count FROM oppgave
            WHERE utbetaling_id = :utbetalingId AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
        """, mapOf("utbetalingId" to utbetalingId)
        ).single { it.int("oppgave_count") }) > 0

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) =
        requireNotNull(
            asSQL(
                """ SELECT COUNT(1) AS oppgave_count FROM oppgave o
                INNER JOIN vedtak v on o.vedtak_ref = v.id
                WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status = 'Ferdigstilt'::oppgavestatus
            """, mapOf("vedtaksperiodeId" to vedtaksperiodeId)
            ).single { it.int("oppgave_count") }) > 0

    fun venterPåSaksbehandler(oppgaveId: Long) = requireNotNull(asSQL(
        """ 
            SELECT EXISTS (
                SELECT 1 FROM oppgave WHERE id=:oppgaveId AND status IN('AvventerSaksbehandler'::oppgavestatus)
            )
        """, mapOf("oppgaveId" to oppgaveId)
    ).single { it.boolean(1) })

    fun finnFødselsnummer(oppgaveId: Long) = requireNotNull(asSQL(
        """ SELECT fodselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """, mapOf("oppgaveId" to oppgaveId)
    ).single { it.long("fodselsnummer").toFødselsnummer() })

    fun gosysOppgaveEndretCommandData(oppgaveId: Long): GosysOppgaveEndretCommandData? =
        asSQL(
            """ SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, h.id AS hendelseId, h.data AS godkjenningbehovJson, s.type as periodetype
            FROM vedtak v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN hendelse h ON h.id = (SELECT hendelse_id FROM command_context WHERE context_id = o.command_context_id LIMIT 1)
            INNER JOIN saksbehandleroppgavetype s ON s.vedtak_ref = v.id
            WHERE o.id = :oppgaveId 
        """, mapOf("oppgaveId" to oppgaveId)
        ).single {
            val json = objectMapper.readTree(it.string("godkjenningbehovJson"))
            val skjæringstidspunkt = json.path("Godkjenning").path("skjæringstidspunkt").asLocalDate()
            GosysOppgaveEndretCommandData(
                vedtaksperiodeId = it.uuid("vedtaksperiode_id"),
                periodeFom = it.localDate("fom"),
                periodeTom = it.localDate("tom"),
                skjæringstidspunkt = skjæringstidspunkt,
                utbetalingId = it.uuid("utbetaling_id"),
                hendelseId = it.uuid("hendelseId"),
                godkjenningsbehovJson = it.string("godkjenningbehovJson"),
                periodetype = enumValueOf(it.string("periodetype"))
            )
        }

    fun invaliderOppgaveFor(fødselsnummer: String) = asSQL(
        """
        UPDATE oppgave o
        SET status = 'Invalidert'
        FROM oppgave o2
        JOIN vedtak v on v.id = o2.vedtak_ref
        JOIN person p on v.person_ref = p.id
        WHERE p.fodselsnummer = :fodselsnummer
        and o.id = o2.id
        AND o.status = 'AvventerSaksbehandler'::oppgavestatus; 
    """, mapOf("fodselsnummer" to fødselsnummer.toLong())
    ).update()


    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
