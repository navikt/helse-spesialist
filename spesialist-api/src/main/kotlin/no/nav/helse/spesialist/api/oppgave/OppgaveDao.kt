package no.nav.helse.spesialist.api.oppgave

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import no.nav.helse.spesialist.api.person.PersoninfoApiDto
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language

class OppgaveDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto? {
        @Language("PostgreSQL")
        val query = """
            SELECT id, er_beslutter_oppgave, er_retur_oppgave, totrinnsvurdering, tidligere_saksbehandler_oid
            FROM oppgave
            WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                AND status = 'AvventerSaksbehandler'::oppgavestatus
        """.trimIndent()
        return query.single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) {
            OppgaveForPeriodevisningDto(
                id = it.string("id"),
                erBeslutter = it.boolean("er_beslutter_oppgave"),
                erRetur = it.boolean("er_retur_oppgave"),
                trengerTotrinnsvurdering = it.boolean("totrinnsvurdering"),
                tidligereSaksbehandler = it.stringOrNull("tidligere_saksbehandler_oid"),
            )
        }
    }

    fun finnOppgaver(saksbehandlerTilganger: SaksbehandlerTilganger) =
        sessionOf(dataSource).use { session ->
            val eventuellEkskluderingAvRiskQA =
                if (saksbehandlerTilganger.harTilgangTilRiskOppgaver()) "" else "AND o.type != 'RISK_QA'"
            val gyldigeAdressebeskyttelser =
                if (saksbehandlerTilganger.harTilgangTilKode7()) "AND pi.adressebeskyttelse IN ('Ugradert', 'Fortrolig')"
                else "AND pi.adressebeskyttelse = 'Ugradert'"
            val eventuellEkskluderingAvBeslutterOppgaver =
                if (saksbehandlerTilganger.harTilgangTilBeslutterOppgaver()) "" else "AND o.er_beslutter_oppgave = false"
            // bruk av const direkte i @Language-annotert sql fører til snodige fantom-compile-feil i IntelliJ
            val beslutterOppgaveHackyWorkaround = BESLUTTEROPPGAVE_PREFIX

            @Language("PostgreSQL")
            val query = """
            SELECT o.id as oppgave_id, o.type AS oppgavetype, o.opprettet, o.er_beslutter_oppgave, o.er_retur_oppgave, o.totrinnsvurdering, o.tidligere_saksbehandler_oid , s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
                   pi.kjonn, pi.adressebeskyttelse, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, sot.inntektskilde, e.id AS enhet_id, e.navn AS enhet_navn, t.på_vent,
                   (SELECT COUNT(DISTINCT melding) from warning w where w.melding not like '$beslutterOppgaveHackyWorkaround%' and w.vedtak_ref = o.vedtak_ref and (w.inaktiv_fra is null or w.inaktiv_fra > now())) AS antall_varsler
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

    fun hentFerdigstilteOppgaver(behandletAvIdent: String, fom: LocalDate?): List<FerdigstiltOppgaveDto> = queryize(
        """
        SELECT o.id                                                     as oppgave_id,
               o.type                                                   as oppgavetype,
               o.oppdatert                                              as ferdigstilt_tidspunkt,
               pi.fornavn                                               as soker_fornavn,
               pi.mellomnavn                                            as soker_mellomnavn,
               pi.etternavn                                             as soker_etternavn,
               p.aktor_id                                               as soker_aktor_id,
               sot.type                                                 as periodetype,
               sot.inntektskilde                                        as inntektstype,
               e.navn                                                   as bosted,
               (SELECT count(distinct melding)
                FROM warning w
                WHERE w.vedtak_ref = o.vedtak_ref
                  AND (w.inaktiv_fra is null OR w.inaktiv_fra > now())) as antall_varsler
        FROM oppgave o
                 INNER JOIN vedtak v ON o.vedtak_ref = v.id
                 INNER JOIN person p ON v.person_ref = p.id
                 INNER JOIN person_info pi ON p.info_ref = pi.id
                 LEFT JOIN enhet e ON p.enhet_ref = e.id
                 LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                 LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
                 LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
        WHERE (o.status = 'Ferdigstilt' OR o.status = 'AvventerSystem')
          AND o.oppdatert >= :fom
          AND s.ident = :ident
        ORDER BY o.oppdatert;
    """.trimIndent()
    ).list(mapOf("ident" to behandletAvIdent, "fom" to (fom ?: LocalDate.now()))) {
        FerdigstiltOppgaveDto(
            id = it.string("oppgave_id"),
            type = Oppgavetype.valueOf(it.string("oppgavetype")),
            ferdigstiltTidspunkt = it.localDateTime("ferdigstilt_tidspunkt"),
            personinfo = Personnavn(
                fornavn = it.string("soker_fornavn"),
                mellomnavn = it.stringOrNull("soker_mellomnavn"),
                etternavn = it.string("soker_etternavn"),
            ),
            aktørId = it.string("soker_aktor_id"),
            antallVarsler = it.int("antall_varsler"),
            periodetype = Periodetype.valueOf(it.string("periodetype")),
            inntektskilde = Inntektskilde.valueOf(it.string("inntektstype")),
            bosted = it.string("bosted"),
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

    private fun saksbehandleroppgaveDto(it: Row) = OppgaveForOversiktsvisningDto(
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

const val BESLUTTEROPPGAVE_PREFIX = "Beslutteroppgave:"