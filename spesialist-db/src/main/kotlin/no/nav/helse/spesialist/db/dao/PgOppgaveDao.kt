package no.nav.helse.spesialist.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.KommentarFraDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.PaVentInfoFraDatabase
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveDao internal constructor(
    queryRunner: QueryRunner,
) : OppgaveDao,
    QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    internal constructor(session: Session) : this(MedSession(session))

    override fun finnGenerasjonId(oppgaveId: Long): UUID =
        asSQL("SELECT generasjon_ref FROM oppgave WHERE id = :oppgaveId", "oppgaveId" to oppgaveId)
            .single { it.uuid("generasjon_ref") }

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long =
        asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fødselsnummer = :fodselsnummer
            ORDER BY o.id DESC
            LIMIT 1
            """,
            "fodselsnummer" to fødselsnummer,
        ).single {
            it.long("oppgaveId")
        }

    override fun finnOppgaveId(fødselsnummer: String): Long? =
        asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o
            JOIN vedtak v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
                AND p.fødselsnummer = :fodselsnummer;
            """,
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull {
            it.long("oppgaveId")
        }

    override fun finnOppgaveId(utbetalingId: UUID): Long? =
        asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o WHERE o.utbetaling_id = :utbetaling_id
            AND o.status NOT IN ('Invalidert'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
            ORDER BY o.id DESC
            LIMIT 1
            """,
            "utbetaling_id" to utbetalingId,
        ).singleOrNull {
            it.long("oppgaveId")
        }

    override fun finnVedtaksperiodeId(fødselsnummer: String): UUID =
        asSQL(
            """
            SELECT v.vedtaksperiode_id
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fødselsnummer = :fodselsnummer
            AND status = 'AvventerSaksbehandler'::oppgavestatus;
            """,
            "fodselsnummer" to fødselsnummer,
        ).single {
            it.uuid("vedtaksperiode_id")
        }

    override fun finnVedtaksperiodeId(oppgaveId: Long) =
        asSQL(
            """
            SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).single { row -> row.uuid("vedtaksperiode_id") }

    override fun finnHendelseId(id: Long): UUID =
        asSQL(
            "SELECT hendelse_id_godkjenningsbehov FROM oppgave WHERE id = :oppgaveId",
            "oppgaveId" to id,
        ).single {
            it.uuid("hendelse_id_godkjenningsbehov")
        }

    override fun invaliderOppgaveFor(fødselsnummer: String) {
        asSQL(
            """
            UPDATE oppgave o
            SET status = 'Invalidert'
            FROM oppgave o2
            JOIN vedtak v on v.id = o2.vedtak_ref
            JOIN person p on v.person_ref = p.id
            WHERE p.fødselsnummer = :fodselsnummer
            and o.id = o2.id
            AND o.status = 'AvventerSaksbehandler'::oppgavestatus; 
            """,
            "fodselsnummer" to fødselsnummer,
        ).update()
    }

    override fun reserverNesteId(): Long =
        asSQL(
            """
            SELECT nextval(pg_get_serial_sequence('oppgave', 'id')) as neste_id 
            """,
        ).single {
            it.long("neste_id")
        }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean =
        asSQL(
            """
            SELECT EXISTS (
                SELECT 1 FROM oppgave WHERE id=:oppgaveId AND status IN('AvventerSaksbehandler'::oppgavestatus)
            ) 
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.boolean(1)
        }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID =
        asSQL(
            """
            SELECT spleis_behandling_id FROM oppgave o
            INNER JOIN behandling b ON b.unik_id = o.generasjon_ref
            WHERE o.id = :oppgaveId; 
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.uuid("spleis_behandling_id")
        }

    override fun oppgaveDataForAutomatisering(oppgaveId: Long): OppgaveDataForAutomatisering? =
        asSQL(
            """
            SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, h.id AS hendelseId, h.data AS godkjenningbehovJson, s.type as periodetype
            FROM vedtak v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN hendelse h ON h.id = o.hendelse_id_godkjenningsbehov
            INNER JOIN saksbehandleroppgavetype s ON s.vedtak_ref = v.id
            WHERE o.id = :oppgaveId
            """,
            "oppgaveId" to oppgaveId,
        ).singleOrNull { row ->
            val json = objectMapper.readTree(row.string("godkjenningbehovJson"))
            val skjæringstidspunkt =
                json.path("Godkjenning").path("skjæringstidspunkt").asText().let(LocalDate::parse)
            OppgaveDataForAutomatisering(
                oppgaveId = oppgaveId,
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                periodeFom = row.localDate("fom"),
                periodeTom = row.localDate("tom"),
                skjæringstidspunkt = skjæringstidspunkt,
                utbetalingId = row.uuid("utbetaling_id"),
                hendelseId = row.uuid("hendelseId"),
                godkjenningsbehovJson = row.string("godkjenningbehovJson"),
                periodetype = enumValueOf(row.string("periodetype")),
            )
        }

    override fun finnOppgaverForVisning(
        ekskluderEgenskaper: List<String>,
        saksbehandlerOid: UUID,
        offset: Int,
        limit: Int,
        sortering: List<OppgavesorteringForDatabase>,
        egneSakerPåVent: Boolean,
        egneSaker: Boolean,
        tildelt: Boolean?,
        filtreringer: OppgaveDao.Filtreringer,
    ): List<OppgaveFraDatabaseForVisning> {
        val orderBy = if (sortering.isNotEmpty()) sortering.joinToString { it.nøkkelTilKolonne() } else "opprettet DESC"

        return asSQL(
            """
            SELECT
                o.id as oppgave_id,
                p.aktør_id,
                v.vedtaksperiode_id,
                pi.fornavn, pi.mellomnavn, pi.etternavn,
                o.egenskaper,
                s.oid, s.ident, s.epost, s.navn,
                o.opprettet,
                os.soknad_mottatt AS opprinnelig_soknadsdato,
                o.kan_avvises,
                pv.frist,
                pv.opprettet AS på_vent_opprettet,
                pv.årsaker,
                pv.notattekst,
                sb.ident AS på_vent_saksbehandler,
                pv.dialog_ref,
                count(1) OVER() AS filtered_count
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            INNER JOIN person p ON v.person_ref = p.id
            INNER JOIN person_info pi ON p.info_ref = pi.id
            INNER JOIN opprinnelig_soknadsdato os ON os.vedtaksperiode_id = v.vedtaksperiode_id
            LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
            LEFT JOIN totrinnsvurdering ttv ON (ttv.person_ref = v.person_ref AND ttv.tilstand != 'GODKJENT')
            LEFT JOIN saksbehandler s ON t.saksbehandler_ref = s.oid
            LEFT JOIN pa_vent pv ON v.vedtaksperiode_id = pv.vedtaksperiode_id
            LEFT JOIN saksbehandler sb ON pv.saksbehandler_ref = sb.oid
            WHERE o.status = 'AvventerSaksbehandler'
                AND (:ukategoriserte_egenskaper = '{}' OR egenskaper @> :ukategoriserte_egenskaper::varchar[]) -- ukategoriserte egenskaper, inkluder oppgaver som inneholder alle saksbehandler har valgt
                AND (:oppgavetypeegenskaper = '{}' OR egenskaper && :oppgavetypeegenskaper::varchar[]) -- inkluder alle oppgaver som har minst en av de valgte oppgavetype
                AND (:periodetypeegenskaper = '{}' OR egenskaper && :periodetypeegenskaper::varchar[]) -- inkluder alle oppgaver som har minst en av de valgte periodetypene
                AND (:mottakeregenskaper = '{}' OR egenskaper && :mottakeregenskaper::varchar[]) -- inkluder alle oppgaver som har minst en av de valgte mottakertypene
                AND (:antall_arbeidsforhold_egenskaper = '{}' OR egenskaper && :antall_arbeidsforhold_egenskaper::varchar[]) -- inkluder alle oppgaver som har minst en av de valgte
                AND (:statusegenskaper = '{}' OR egenskaper && :statusegenskaper::varchar[]) -- inkluder alle oppgaver som har minst en av de valgte statusene
                AND NOT (egenskaper && :egenskaper_som_skal_ekskluderes::varchar[]) -- egenskaper saksbehandler ikke har tilgang til
                AND NOT ('BESLUTTER' = ANY(egenskaper) AND ttv.saksbehandler = :oid) -- hvis oppgaven er sendt til beslutter og saksbehandler var den som sendte
                AND
                    CASE
                        WHEN :egne_saker_pa_vent THEN t.saksbehandler_ref = :oid AND ('PÅ_VENT' = ANY(o.egenskaper))
                        WHEN :egne_saker THEN t.saksbehandler_ref = :oid AND NOT ('PÅ_VENT' = ANY(o.egenskaper))
                        ELSE true
                    END
                AND
                    CASE
                        WHEN :tildelt THEN t.oppgave_id_ref IS NOT NULL
                        WHEN :tildelt = false THEN t.oppgave_id_ref IS NULL
                        ELSE true
                    END
            ORDER BY $orderBy
            OFFSET :offset
            LIMIT :limit
            """,
            "oid" to saksbehandlerOid,
            "offset" to offset,
            "limit" to limit,
            "egne_saker_pa_vent" to egneSakerPåVent,
            "egne_saker" to egneSaker,
            "tildelt" to tildelt,
            "ukategoriserte_egenskaper" to filtreringer.ukategoriserteEgenskaper.somDbArray(),
            "oppgavetypeegenskaper" to filtreringer.oppgavetypeegenskaper.somDbArray(),
            "periodetypeegenskaper" to filtreringer.periodetypeegenskaper.somDbArray(),
            "mottakeregenskaper" to filtreringer.mottakeregenskaper.somDbArray(),
            "antall_arbeidsforhold_egenskaper" to filtreringer.antallArbeidsforholdEgenskaper.somDbArray(),
            "statusegenskaper" to filtreringer.statusegenskaper.somDbArray(),
            "egenskaper_som_skal_ekskluderes" to ekskluderEgenskaper.somDbArray(),
        ).list { row ->
            val egenskaper =
                row.array<String>("egenskaper").map { enumValueOf<EgenskapForDatabase>(it) }.toSet()
            OppgaveFraDatabaseForVisning(
                id = row.long("oppgave_id"),
                aktørId = row.string("aktør_id"),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                navn =
                    PersonnavnFraDatabase(
                        row.string("fornavn"),
                        row.stringOrNull("mellomnavn"),
                        row.string("etternavn"),
                    ),
                egenskaper = egenskaper,
                tildelt =
                    row.uuidOrNull("oid")?.let {
                        SaksbehandlerFraDatabase(
                            epostadresse = row.string("epost"),
                            it,
                            row.string("navn"),
                            row.string("ident"),
                        )
                    },
                påVent = egenskaper.contains(EgenskapForDatabase.PÅ_VENT),
                opprettet = row.localDateTime("opprettet"),
                opprinneligSøknadsdato = row.localDateTime("opprinnelig_soknadsdato"),
                tidsfrist = row.localDateOrNull("frist"),
                filtrertAntall = row.int("filtered_count"),
                paVentInfo =
                    row.localDateTimeOrNull("på_vent_opprettet")?.let {
                        PaVentInfoFraDatabase(
                            årsaker = row.array<String>("årsaker").toList(),
                            tekst = row.stringOrNull("notattekst"),
                            dialogRef = row.long("dialog_ref"),
                            saksbehandler = row.string("på_vent_saksbehandler"),
                            opprettet = it,
                            tidsfrist = row.localDate("frist"),
                            kommentarer = finnKommentarerMedDialogRef(row.long("dialog_ref").toInt()),
                        )
                    },
            )
        }
    }

    private fun finnKommentarerMedDialogRef(dialogRef: Int): List<KommentarFraDatabase> =
        asSQL(
            """
            select id, tekst, feilregistrert_tidspunkt, opprettet, saksbehandlerident
            from kommentarer k
            where dialog_ref = :dialogRef
            """.trimIndent(),
            "dialogRef" to dialogRef,
        ).list { mapKommentarFraDatabase(it) }

    private fun mapKommentarFraDatabase(it: Row): KommentarFraDatabase =
        KommentarFraDatabase(
            id = it.int("id"),
            tekst = it.string("tekst"),
            opprettet = it.localDateTime("opprettet"),
            saksbehandlerident = it.string("saksbehandlerident"),
        )

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase =
        asSQL(
            """
            SELECT
                count(*) FILTER ( WHERE NOT 'PÅ_VENT' = ANY (o.egenskaper) ) AS antall_mine_saker,
                count(*) FILTER ( WHERE 'PÅ_VENT' = ANY (o.egenskaper) ) AS antall_mine_saker_på_vent
            from oppgave o
                LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
            WHERE o.status = 'AvventerSaksbehandler'
                AND t.saksbehandler_ref = :oid 
            """,
            "oid" to saksbehandlerOid,
        ).singleOrNull { row ->
            AntallOppgaverFraDatabase(
                antallMineSaker = row.int("antall_mine_saker"),
                antallMineSakerPåVent = row.int("antall_mine_saker_på_vent"),
            )
        } ?: AntallOppgaverFraDatabase(antallMineSaker = 0, antallMineSakerPåVent = 0)

    override fun finnFødselsnummer(oppgaveId: Long): String =
        asSQL(
            """
            SELECT fødselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.string("fødselsnummer")
        }

    override fun oppdaterPekerTilGodkjenningsbehov(
        godkjenningsbehovId: UUID,
        utbetalingId: UUID,
    ) {
        asSQL(
            """
            update oppgave
            set hendelse_id_godkjenningsbehov = :godkjenningsbehovId
            where utbetaling_id = :utbetalingId
            """.trimIndent(),
            "godkjenningsbehovId" to godkjenningsbehovId,
            "utbetalingId" to utbetalingId,
        ).update()
    }

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean =
        asSQL(
            """
            SELECT COUNT(1) AS oppgave_count FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status = 'Ferdigstilt'::oppgavestatus
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.int("oppgave_count")
        } > 0

    override fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandletOppgaveFraDatabaseForVisning> =
        asSQL(
            """
            SELECT
                o.id as oppgave_id,
                p.aktør_id,
                o.egenskaper,
                o.oppdatert as ferdigstilt_tidspunkt,
                o.ferdigstilt_av,
                ttv.beslutter,
                ttv.saksbehandler,
                pi.fornavn, pi.mellomnavn, pi.etternavn,
                count(1) OVER() AS filtered_count
            FROM oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                LEFT JOIN (SELECT utbetaling_id, beslutter.ident as beslutter, saksbehandler.ident as saksbehandler
                         FROM totrinnsvurdering tv
                         INNER JOIN utbetaling_id ui ON ui.id = utbetaling_id_ref
                         INNER JOIN saksbehandler beslutter on tv.beslutter = beslutter.oid
                         INNER JOIN saksbehandler saksbehandler on tv.saksbehandler = saksbehandler.oid
                         WHERE (saksbehandler = :oid OR beslutter = :oid) AND (tv.oppdatert::date >= :fom::date AND tv.oppdatert::date <= :tom::date)
                     ) ttv ON ttv.utbetaling_id = o.utbetaling_id
            WHERE (ttv.utbetaling_id IS NOT NULL OR o.ferdigstilt_av_oid = :oid)
                AND (o.status in ('Ferdigstilt', 'AvventerSystem'))
                AND (o.oppdatert::date >= :fom::date AND o.oppdatert::date <= :tom::date)
            ORDER BY o.oppdatert
            OFFSET :offset
            LIMIT :limit;
            """,
            "oid" to behandletAvOid,
            "fom" to fom,
            "tom" to tom,
            "offset" to offset,
            "limit" to limit,
        ).list { row ->
            BehandletOppgaveFraDatabaseForVisning(
                id = row.long("oppgave_id"),
                aktørId = row.string("aktør_id"),
                egenskaper =
                    row
                        .array<String>("egenskaper")
                        .map { enumValueOf<EgenskapForDatabase>(it) }
                        .toSet(),
                ferdigstiltTidspunkt = row.localDateTime("ferdigstilt_tidspunkt"),
                ferdigstiltAv = row.stringOrNull("ferdigstilt_av"),
                saksbehandler = row.stringOrNull("saksbehandler") ?: row.stringOrNull("ferdigstilt_av"),
                beslutter = row.stringOrNull("beslutter"),
                navn =
                    PersonnavnFraDatabase(
                        row.string("fornavn"),
                        row.stringOrNull("mellomnavn"),
                        row.string("etternavn"),
                    ),
                filtrertAntall = row.int("filtered_count"),
            )
        }

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? =
        asSQL(
            """
            SELECT o.egenskaper FROM oppgave o 
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
            AND o.utbetaling_id = :utbetalingId
            ORDER BY o.opprettet DESC
            LIMIT 1 
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "utbetalingId" to utbetalingId,
        ).singleOrNull { row ->
            row.array<String>("egenskaper").map { enumValueOf<EgenskapForDatabase>(it) }.toSet()
        }

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? =
        asSQL(
            """
            SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
                    AND status not in ('Ferdigstilt'::oppgavestatus, 'Invalidert'::oppgavestatus)
            ORDER BY opprettet DESC
            LIMIT 1
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            it.long("id")
        }

    private fun OppgavesorteringForDatabase.nøkkelTilKolonne() =
        when (this.nøkkel) {
            SorteringsnøkkelForDatabase.TILDELT_TIL -> "navn".direction(this.stigende).nullsLast()
            SorteringsnøkkelForDatabase.OPPRETTET -> "opprettet".direction(this.stigende)
            SorteringsnøkkelForDatabase.TIDSFRIST -> "frist".direction(this.stigende).nullsLast()
            SorteringsnøkkelForDatabase.SØKNAD_MOTTATT -> "opprinnelig_soknadsdato".direction(this.stigende)
        }

    private fun String.direction(stigende: Boolean) = if (stigende) "$this ASC" else "$this DESC"

    private fun String.nullsLast() = "$this NULLS LAST"
}
