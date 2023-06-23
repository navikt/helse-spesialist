package no.nav.helse.modell.oppgave

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommandData
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.intellij.lang.annotations.Language

class OppgaveDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finnOppgaveId(vedtaksperiodeId: UUID) =
        """ SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("id") }

    fun finnUtbetalingId(oppgaveId: Long) =
        queryize(""" SELECT utbetaling_id FROM oppgave WHERE id = :oppgaveId""")
            .single(mapOf("oppgaveId" to oppgaveId)) { it.uuid("utbetaling_id") }

    fun finnNyesteOppgaveId(vedtaksperiodeId: UUID) =
        """
            SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            ORDER BY opprettet DESC
            LIMIT 1
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("id") }

    fun erRiskoppgave(oppgaveId: Long): Boolean =
        """ SELECT 1 FROM oppgave
            WHERE id=:oppgaveId
            AND type = 'RISK_QA'
        """.single(mapOf("oppgaveId" to oppgaveId)) { true } ?: false

    fun finnOppgaveIdUansettStatus(fødselsnummer: String) =
        """ SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            ORDER BY o.id DESC
            LIMIT 1;
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it.long("oppgaveId") }!!

    fun finnGodkjenningsbehov(fødselsnummer: String) =
        """ SELECT c.hendelse_id as hendelse_id
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
                     JOIN command_context c ON o.command_context_id = c.context_id
            WHERE p.fodselsnummer = :fodselsnummer
            AND status = 'AvventerSaksbehandler'::oppgavestatus
            GROUP BY c.hendelse_id;
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it.uuid("hendelse_id") }!!

    fun finnVedtaksperiodeId(fødselsnummer: String) =
        """ SELECT v.vedtaksperiode_id as vedtaksperiode_id
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            AND status = 'AvventerSaksbehandler'::oppgavestatus;
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it.uuid("vedtaksperiode_id") }!!

    fun finnOppgaveId(fødselsnummer: String) =
        """ SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
              AND p.fodselsnummer = :fodselsnummer;
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it.long("oppgaveId") }

    fun finn(oppgaveId: Long) =
        """ SELECT o.type, o.status, v.vedtaksperiode_id, o.ferdigstilt_av, o.ferdigstilt_av_oid, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE o.id = :oppgaveId
        """.single(mapOf("oppgaveId" to oppgaveId)) { row ->
            Oppgave(
                id = oppgaveId,
                type = enumValueOf(row.string("type")),
                status = enumValueOf(row.string("status")),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString)
            )
        }

    fun finnAktiv(vedtaksperiodeId: UUID) =
        """ SELECT o.id, o.type, o.status, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { row ->
            Oppgave(
                id = row.long("id"),
                type = enumValueOf(row.string("type")),
                status = enumValueOf(row.string("status")),
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
            )
        }

    fun finn(utbetalingId: UUID) =
        """ SELECT o.id, o.type, o.status, v.vedtaksperiode_id, o.utbetaling_id, o.ferdigstilt_av, o.ferdigstilt_av_oid
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE utbetaling_id = :utbetalingId AND o.status NOT IN ('Invalidert'::oppgavestatus)
        """.single(mapOf("utbetalingId" to utbetalingId)) { row ->
            Oppgave(
                id = row.long("id"),
                type = enumValueOf(row.string("type")),
                status = enumValueOf(row.string("status")),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString)
            )
        }

    fun finnVedtaksperiodeId(oppgaveId: Long) = requireNotNull(
        """ SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """.single(mapOf("oppgaveId" to oppgaveId)) { row -> row.uuid("vedtaksperiode_id") })

    private fun finnArbeidsgiverbeløpOgPersonbeløp(vedtaksperiodeId: UUID, utbetalingId: UUID) = requireNotNull(
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
        """.trimIndent().single(mapOf(
            "utbetalingId" to utbetalingId,
            "vedtaksperiodeId" to vedtaksperiodeId
        )) {row ->
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

    fun opprettOppgave(commandContextId: UUID, oppgavetype: Oppgavetype, vedtaksperiodeId: UUID, utbetalingId: UUID) =
        requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use {
            val vedtakRef = vedtakRef(vedtaksperiodeId)

            val (arbeidsgiverBeløp, personBeløp) = finnArbeidsgiverbeløpOgPersonbeløp(vedtaksperiodeId, utbetalingId)
            val mottaker = finnMottaker(arbeidsgiverBeløp > 0, personBeløp > 0)

            @Language("PostgreSQL")
            val query = """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id, mottaker)
                VALUES (now(), CAST(? as oppgavetype), CAST(? as oppgavestatus), ?, ?, ?, ?, ?, CAST(? as mottakertype));
            """
            it.run(
                queryOf(
                    query,
                    oppgavetype.name,
                    AvventerSaksbehandler.name,
                    null,
                    null,
                    vedtakRef,
                    commandContextId,
                    utbetalingId,
                    mottaker?.name
                ).asUpdateAndReturnGeneratedKey
            )
        }) { "Kunne ikke opprette oppgave" }

    private fun vedtakRef(vedtaksperiodeId: UUID) = requireNotNull(
        """SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId"""
            .single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("id") })
    { "Kunne ikke finne vedtak for vedtaksperiodeId $vedtaksperiodeId" }

    fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String? = null,
        oid: UUID? = null,
    ) =
        """UPDATE oppgave SET ferdigstilt_av=:ferdigstiltAv, ferdigstilt_av_oid=:oid, status=:oppgavestatus::oppgavestatus WHERE id=:oppgaveId"""
            .update(
                mapOf(
                    "ferdigstiltAv" to ferdigstiltAv,
                    "oid" to oid,
                    "oppgavestatus" to oppgavestatus.name,
                    "oppgaveId" to oppgaveId
                )
            )

    fun finnContextId(oppgaveId: Long) = requireNotNull(
        """SELECT command_context_id FROM oppgave WHERE id = :oppgaveId""".trimMargin().single(mapOf("oppgaveId" to oppgaveId)) { row ->
            UUID.fromString(
                row.string("command_context_id")
            )
        })

    fun finnHendelseId(oppgaveId: Long) = requireNotNull(
        """SELECT DISTINCT hendelse_id 
            |FROM command_context 
            |WHERE context_id = (SELECT command_context_id FROM oppgave WHERE id = :oppgaveId)""".trimMargin()
            .single(mapOf("oppgaveId" to oppgaveId)) { row -> row.uuid("hendelse_id") })

    fun harGyldigOppgave(utbetalingId: UUID) = requireNotNull(
        """ SELECT COUNT(1) AS oppgave_count FROM oppgave
            WHERE utbetaling_id = :utbetalingId AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
        """.single(mapOf("utbetalingId" to utbetalingId)) { it.int("oppgave_count") }) > 0

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) =
        requireNotNull(
            """ SELECT COUNT(1) AS oppgave_count FROM oppgave o
                INNER JOIN vedtak v on o.vedtak_ref = v.id
                WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status = 'Ferdigstilt'::oppgavestatus
            """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.int("oppgave_count") }) > 0

    fun venterPåSaksbehandler(oppgaveId: Long) = requireNotNull(
        """ SELECT EXISTS ( SELECT 1 FROM oppgave WHERE id=:oppgaveId AND status IN('AvventerSaksbehandler'::oppgavestatus) )"""
            .single(mapOf("oppgaveId" to oppgaveId)) { it.boolean(1) })

    fun finnFødselsnummer(oppgaveId: Long) = requireNotNull(
        """ SELECT fodselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """.single(mapOf("oppgaveId" to oppgaveId)) { it.long("fodselsnummer").toFødselsnummer() })

    fun gosysOppgaveEndretCommandData(oppgaveId: Long): GosysOppgaveEndretCommandData? =
        """ SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, h.id AS hendelseId, h.data AS godkjenningbehovJson, s.type as periodetype
            FROM vedtak v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN hendelse h ON h.id = (SELECT hendelse_id FROM command_context WHERE context_id = o.command_context_id LIMIT 1)
            INNER JOIN saksbehandleroppgavetype s ON s.vedtak_ref = v.id
            WHERE o.id = :oppgaveId 
        """.single(mapOf("oppgaveId" to oppgaveId)) {
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

    fun invaliderOppgaveFor(fødselsnummer: String) = queryize("""
        UPDATE oppgave o
        SET status = 'Invalidert'
        FROM oppgave o2
        JOIN vedtak v on v.id = o2.vedtak_ref
        JOIN person p on v.person_ref = p.id
        WHERE p.fodselsnummer = :fodselsnummer
        and o.id = o2.id
        AND o.status = 'AvventerSaksbehandler'::oppgavestatus; 
    """).update(mapOf("fodselsnummer" to fødselsnummer.toLong()))


    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
