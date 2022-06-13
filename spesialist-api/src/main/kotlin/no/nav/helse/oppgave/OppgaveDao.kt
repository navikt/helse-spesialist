package no.nav.helse.oppgave

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.SaksbehandlerTilganger
import no.nav.helse.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.Kjønn
import no.nav.helse.person.PersoninfoApiDto
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.vedtaksperiode.EnhetDto
import no.nav.helse.vedtaksperiode.Inntektskilde
import no.nav.helse.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language

class OppgaveDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    fun finnOppgaver(saksbehandlerTilganger: SaksbehandlerTilganger) =
        sessionOf(dataSource).use { session ->
            val eventuellEkskluderingAvRiskQA =
                if (saksbehandlerTilganger.harTilgangTilRiskOppgaver()) "" else "AND o.type != 'RISK_QA'"
            val gyldigeAdressebeskyttelser =
                if (saksbehandlerTilganger.harTilgangTilKode7Oppgaver()) "AND pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig')"
                else "AND pi.adressebeskyttelse = 'Ugradert'"
            val eventuellEkskluderingAvBeslutterOppgaver = if(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver()) "" else "AND o.er_beslutter_oppgave = false"

            @Language("PostgreSQL")
            val query = """
            SELECT o.id as oppgave_id, o.type AS oppgavetype, o.opprettet, o.er_beslutter_oppgave, o.er_retur_oppgave, o.totrinnsvurdering, o.tidligere_saksbehandler_oid , s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
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
            ORDER BY
                CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'STIKKPRØVE' THEN 0 ELSE 1 END,
                CASE WHEN o.type = 'RISK_QA' THEN 0 ELSE 1 END,
                opprettet ASC
                ;
            """
            session.run(
                queryOf(query)
                    .map(::saksbehandleroppgaveDto)
                    .asList
            )
        }

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
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("totrinnsvurdering") } ?: false

    fun hentTidligereSaksbehandlerOid(vedtaksperiodeId: UUID): UUID? =
        """ SELECT tidligere_saksbehandler_oid FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.uuidOrNull("tidligere_saksbehandler_oid") }

    fun erBeslutteroppgave(vedtaksperiodeId: UUID): Boolean =
        """ SELECT er_beslutter_oppgave FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_beslutter_oppgave") } ?: false

    fun erBeslutteroppgave(oppgaveId: Long): Boolean =
        """ SELECT er_beslutter_oppgave FROM oppgave
            WHERE id=:oppgaveId
        """.single(mapOf("oppgaveId" to oppgaveId)) { it.boolean("er_beslutter_oppgave") } ?: false

    fun erReturOppgave(vedtaksperiodeId: UUID): Boolean =
        """ SELECT er_retur_oppgave FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
        """.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.boolean("er_retur_oppgave") } ?: false

    fun erReturoppgave(oppgaveId: Long): Boolean =
        """ SELECT er_retur_oppgave FROM oppgave
            WHERE id=:oppgaveId
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
        """SELECT hendelse_id FROM command_context WHERE context_id = (SELECT command_context_id FROM oppgave WHERE id = :oppgaveId)"""
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
        """ SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, uid.type AS utbetalingType, h.id AS hendelseId, h.data AS godkjenningbehovJson
            FROM vedtak v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN utbetaling_id uid ON uid.utbetaling_id = o.utbetaling_id
            INNER JOIN hendelse h ON h.id = (SELECT hendelse_id FROM command_context WHERE context_id = o.command_context_id LIMIT 1)
            WHERE o.id = :oppgaveId 
        """.single(mapOf("oppgaveId" to oppgaveId)) {
            GosysOppgaveEndretCommandData(
                vedtaksperiodeId = it.uuid("vedtaksperiode_id"),
                periodeFom = it.localDate("fom"),
                periodeTom = it.localDate("tom"),
                utbetalingId = it.uuid("utbetaling_id"),
                utbetalingType = it.string("utbetalingType"),
                hendelseId = it.uuid("hendelseId"),
                godkjenningsbehovJson = it.string("godkjenningbehovJson")
            )
        }

    fun finnVedtaksperiodeIdForPeriodeMedDager(fødselsnummer: String, organisasjonsnummer: String, overstyrteDager: List<OverstyringDagDto>): UUID? =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query =
                """ SELECT v.vedtaksperiode_id
                    FROM oppgave o
                    JOIN vedtak v on o.vedtak_ref = v.id
                    JOIN person p on p.id = v.person_ref
                    JOIN arbeidsgiver a on a.id = v.arbeidsgiver_ref
                    WHERE p.fodselsnummer = :fodselsnummer
                    AND a.orgnummer = :orgnummer
                    AND v.fom <= :dato::date
                    AND v.tom >= :dato::date
                    LIMIT 1
                """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnummer" to organisasjonsnummer.toLong(),
                        "dato" to overstyrteDager.first().dato
                    )
                ).map { it.uuid("vedtaksperiode_id") }.asSingle
            )
        }

    fun finnNyesteUtbetalteEllerAktiveVedtaksperiodeIdForSkjæringstidspunkt(fødselsnummer: String, organisasjonsnummer: String, skjæringstidspunkt: LocalDate): UUID? =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query =
                """ SELECT v.vedtaksperiode_id
                    FROM oppgave o
                    JOIN vedtak v on o.vedtak_ref = v.id
                    JOIN person p on p.id = v.person_ref
                    JOIN arbeidsgiver a on a.id = v.arbeidsgiver_ref
                    WHERE p.fodselsnummer = :fodselsnummer
                    AND a.orgnummer = :orgnummer
                    AND v.fom >= :skjaeringstidspunkt::date
                    AND (o.status = 'Ferdigstilt'::oppgavestatus OR o.status = 'AvventerSaksbehandler'::oppgavestatus)
                    ORDER BY 
                        o.status DESC,
                        v.fom DESC
                    LIMIT 1
                """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnummer" to organisasjonsnummer.toLong(),
                        "skjaeringstidspunkt" to skjæringstidspunkt
                    )
                ).map { it.uuid("vedtaksperiode_id") }.asSingle
            )
        }

    fun finnAktivVedtaksperiodeIdForSkjæringstidspunkt(fødselsnummer: String, skjæringstidspunkt: LocalDate): UUID? =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query =
                """ SELECT v.vedtaksperiode_id
                    FROM oppgave o
                    JOIN vedtak v on o.vedtak_ref = v.id
                    JOIN person p on p.id = v.person_ref
                    WHERE p.fodselsnummer = :fodselsnummer
                    AND v.fom >= :skjaeringstidspunkt::date
                    AND o.status = 'AvventerSaksbehandler'::oppgavestatus
                    LIMIT 1
                """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "skjaeringstidspunkt" to skjæringstidspunkt
                    )
                ).map { it.uuid("vedtaksperiode_id") }.asSingle
            )
        }

    fun setTrengerTotrinnsvurdering(vedtaksperiodeId: UUID): Long? =
        sessionOf(dataSource, returnGeneratedKey = true).use {
            @Language("PostgreSQL")
            val query =
                """  UPDATE oppgave o 
                     SET totrinnsvurdering=true 
                     FROM vedtak v
                     WHERE o.vedtak_ref = v.id 
                     AND v.vedtaksperiode_id = ?
            """
            it.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                ).asUpdateAndReturnGeneratedKey
            )
        }

    fun harOppgaveMedEndring(vedtaksperiodeId: UUID): Boolean =
        requireNotNull(sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                """ SELECT EXISTS (
                        SELECT o.id
                        FROM oppgave o
                        INNER JOIN vedtak v ON o.vedtak_ref = v.id
                        INNER JOIN reserver_person rp ON v.person_ref = rp.person_ref
                        WHERE v.vedtaksperiode_id = ?
                    )
                """
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                ).map { it.boolean(1) }.asSingle
            )
        })

    private fun saksbehandleroppgaveDto(it: Row) = OppgaveDto(
        oppgavereferanse = it.string("oppgave_id"),
        oppgavetype = it.string("oppgavetype"),
        opprettet = it.localDateTime("opprettet"),
        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
        personinfo = PersoninfoApiDto(
            it.string("fornavn"),
            it.stringOrNull("mellomnavn"),
            it.string("etternavn"),
            it.localDateOrNull("fodselsdato"),
            it.stringOrNull("kjonn")?.let(Kjønn::valueOf),
            it.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf)
        ),
        aktørId = it.long("aktor_id").toString(),
        fødselsnummer = it.long("fodselsnummer").toFødselsnummer(),
        antallVarsler = it.int("antall_varsler"),
        type = it.stringOrNull("saksbehandleroppgavetype")?.let(Periodetype::valueOf),
        inntektskilde = it.stringOrNull("inntektskilde")?.let(Inntektskilde::valueOf),
        boenhet = EnhetDto(it.string("enhet_id"), it.string("enhet_navn")),
        tildeling = it.stringOrNull("epost")?.let { epost ->
            TildelingApiDto(
                navn = it.string("saksbehandler_navn"),
                epost = epost,
                oid = UUID.fromString(it.string("oid")),
                påVent = it.boolean("på_vent")
            )
        },
        erBeslutterOppgave = it.boolean("er_beslutter_oppgave"),
        erReturOppgave = it.boolean("er_retur_oppgave"),
        trengerTotrinnsvurdering = it.boolean("totrinnsvurdering"),
        tidligereSaksbehandlerOid = it.uuidOrNull("tidligere_saksbehandler_oid")
    )

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
