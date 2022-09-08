package no.nav.helse.spesialist.api.oppgave

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import org.intellij.lang.annotations.Language

class OppgaveDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finnOppgaveId(vedtaksperiodeId: UUID) =
        """ SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("id") }

    fun trengerTotrinnsvurdering(vedtaksperiodeId: UUID): Boolean =
        """ SELECT totrinnsvurdering FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus   
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("totrinnsvurdering") } ?: false

    fun trengerTotrinnsvurdering(oppgaveId: Long): Boolean =
        """ SELECT totrinnsvurdering FROM oppgave
            WHERE id = :oppgaveId
            AND status = 'AvventerSaksbehandler'::oppgavestatus   
        """.single(mapOf("oppgaveId" to oppgaveId)) { it.boolean("totrinnsvurdering") } ?: false

    fun hentTidligereSaksbehandlerOid(vedtaksperiodeId: UUID): UUID? =
        """ SELECT tidligere_saksbehandler_oid FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
             AND status = 'AvventerSaksbehandler'::oppgavestatus   
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.uuidOrNull("tidligere_saksbehandler_oid") }

    fun erBeslutteroppgave(vedtaksperiodeId: UUID): Boolean =
        """ SELECT er_beslutter_oppgave FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus  
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_beslutter_oppgave") } ?: false

    fun erBeslutteroppgave(oppgaveId: Long): Boolean =
        """ SELECT er_beslutter_oppgave FROM oppgave
            WHERE id=:oppgaveId
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """.single(mapOf("oppgaveId" to oppgaveId)) { it.boolean("er_beslutter_oppgave") } ?: false

    fun erReturOppgave(vedtaksperiodeId: UUID): Boolean =
        """ SELECT er_retur_oppgave FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus    
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_retur_oppgave") } ?: false

    fun erReturoppgave(oppgaveId: Long): Boolean =
        """ SELECT er_retur_oppgave FROM oppgave
            WHERE id=:oppgaveId
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """.single(mapOf("oppgaveId" to oppgaveId)) { it.boolean("er_retur_oppgave") } ?: false

    fun finnOppgaveIdUansettStatus(fødselsnummer: String) =
        """ SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            ORDER BY o.id DESC
            LIMIT 1;
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it.long("oppgaveId") }!!

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
                vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
                utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString)
            )
        }

    fun finnAktive(vedtaksperiodeId: UUID) =
        """ SELECT o.id, o.type, o.status, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)
        """.list(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { row ->
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
                vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
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
        """.single(mapOf("oppgaveId" to oppgaveId)) { row -> UUID.fromString(row.string("vedtaksperiode_id")) })

    fun opprettOppgave(commandContextId: UUID, oppgavetype: Oppgavetype, vedtaksperiodeId: UUID, utbetalingId: UUID) =
        requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use {
            val vedtakRef = vedtakRef(vedtaksperiodeId)

            @Language("PostgreSQL")
            val query = """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id)
                VALUES (now(), CAST(? as oppgavetype), CAST(? as oppgavestatus), ?, ?, ?, ?, ?);
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
                    utbetalingId
                ).asUpdateAndReturnGeneratedKey
            )
        }) { "Kunne ikke opprette oppgave" }

    private fun vedtakRef(vedtaksperiodeId: UUID) = requireNotNull(
        """SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId"""
            .single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.long("id") })
    { "Kunne ikke finne vedtak for vedtaksperiodeId $vedtaksperiodeId" }

    fun setBeslutterOppgave(
        oppgaveId: Long,
        erBeslutterOppgave: Boolean,
        erReturOppgave: Boolean,
        totrinnsvurdering: Boolean,
        tidligereSaksbehandlerOID: UUID
    ) =
        """ UPDATE oppgave
            SET er_beslutter_oppgave=:er_beslutter_oppgave, er_retur_oppgave=:er_retur_oppgave, totrinnsvurdering=:totrinnsvurdering, tidligere_saksbehandler_oid=:tidligere_saksbehandler_oid
            WHERE id=:oppgave_id
        """.update(
            mapOf(
                "er_beslutter_oppgave" to erBeslutterOppgave,
                "er_retur_oppgave" to erReturOppgave,
                "totrinnsvurdering" to totrinnsvurdering,
                "tidligere_saksbehandler_oid" to tidligereSaksbehandlerOID,
                "oppgave_id" to oppgaveId
            )
        )

    fun finnTidligereSaksbehandler(oppgaveId: Long) = """
        SELECT tidligere_saksbehandler_oid FROM oppgave
        WHERE id=:oppgave_id
    """.single(mapOf("oppgave_id" to oppgaveId)) { it.uuidOrNull("tidligere_saksbehandler_oid") }

    fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String? = null,
        oid: UUID? = null
    ) =
        """UPDATE oppgave SET oppdatert=now(), ferdigstilt_av=:ferdigstiltAv, ferdigstilt_av_oid=:oid, status=:oppgavestatus::oppgavestatus WHERE id=:oppgaveId"""
            .update(
                mapOf(
                    "ferdigstiltAv" to ferdigstiltAv,
                    "oid" to oid,
                    "oppgavestatus" to oppgavestatus.name,
                    "oppgaveId" to oppgaveId
                )
            )

    fun finnContextId(oppgaveId: Long) = requireNotNull(
        """SELECT command_context_id FROM oppgave WHERE id = :oppgaveId""".single(mapOf("oppgaveId" to oppgaveId)) { row ->
            UUID.fromString(
                row.string("command_context_id")
            )
        })

    fun finnHendelseId(oppgaveId: Long) = requireNotNull(
        """SELECT DISTINCT hendelse_id FROM command_context WHERE context_id = (SELECT command_context_id FROM oppgave WHERE id = :oppgaveId)"""
            .single(mapOf("oppgaveId" to oppgaveId)) { row -> UUID.fromString(row.string("hendelse_id")) })

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
        """ SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, h.id AS hendelseId, h.data AS godkjenningbehovJson
            FROM vedtak v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN hendelse h ON h.id = (SELECT hendelse_id FROM command_context WHERE context_id = o.command_context_id LIMIT 1)
            WHERE o.id = :oppgaveId 
        """.single(mapOf("oppgaveId" to oppgaveId)) {
            GosysOppgaveEndretCommandData(
                vedtaksperiodeId = it.uuid("vedtaksperiode_id"),
                periodeFom = it.localDate("fom"),
                periodeTom = it.localDate("tom"),
                utbetalingId = it.uuid("utbetaling_id"),
                hendelseId = it.uuid("hendelseId"),
                godkjenningsbehovJson = it.string("godkjenningbehovJson")
            )
        }

    fun setTrengerTotrinnsvurdering(vedtaksperiodeId: UUID): Long? =
        sessionOf(dataSource, returnGeneratedKey = true).use {
            @Language("PostgreSQL")
            val query =
                """ UPDATE oppgave
                    SET totrinnsvurdering=true
                    WHERE vedtak_ref = 
                    (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)
                    AND status = 'AvventerSaksbehandler'::oppgavestatus
            """
            it.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                ).asUpdateAndReturnGeneratedKey
            )
        }

    fun setTrengerTotrinnsvurdering(oppgaveId: Long): Long? =
        sessionOf(dataSource, returnGeneratedKey = true).use {
            @Language("PostgreSQL")
            val query =
                """ UPDATE oppgave
                    SET totrinnsvurdering=true
                    WHERE id = ?
            """
            it.run(
                queryOf(
                    query,
                    oppgaveId,
                ).asUpdateAndReturnGeneratedKey
            )
        }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
