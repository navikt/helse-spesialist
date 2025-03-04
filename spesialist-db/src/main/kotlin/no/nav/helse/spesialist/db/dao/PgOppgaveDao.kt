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
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.time.LocalDate
import java.time.LocalDateTime
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

    fun finnOppgave(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave? =
        asSQL(
            """
            SELECT o.egenskaper, o.status, v.vedtaksperiode_id, o.behandling_id, o.hendelse_id_godkjenningsbehov, o.ferdigstilt_av, o.ferdigstilt_av_oid, o.utbetaling_id, s.navn, s.epost, s.ident, s.oid, o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            LEFT JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE o.id = :oppgaveId
            ORDER BY o.id DESC LIMIT 1
            """,
            "oppgaveId" to id,
        ).singleOrNull { row ->
            val egenskaper: Set<Egenskap> =
                row
                    .array<String>("egenskaper")
                    .map { enumValueOf<Egenskap>(it) }
                    .toSet()
            Oppgave.fraLagring(
                id = id,
                egenskaper = egenskaper,
                tilstand = tilstand(row.string("status")),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                behandlingId = row.uuid("behandling_id"),
                utbetalingId = row.uuid("utbetaling_id"),
                godkjenningsbehovId = row.uuid("hendelse_id_godkjenningsbehov"),
                kanAvvises = row.boolean("kan_avvises"),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.uuidOrNull("ferdigstilt_av_oid"),
                tildeltTil =
                    row.uuidOrNull("oid")?.let {
                        LegacySaksbehandler(
                            epostadresse = row.string("epost"),
                            oid = it,
                            navn = row.string("navn"),
                            ident = row.string("ident"),
                            tilgangskontroll = tilgangskontroll,
                        )
                    },
            )
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

    override fun harGyldigOppgave(utbetalingId: UUID) =
        asSQL(
            """
            SELECT COUNT(1) AS oppgave_count FROM oppgave
            WHERE utbetaling_id = :utbetalingId AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
            """,
            "utbetalingId" to utbetalingId,
        ).single { it.int("oppgave_count") } > 0

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
        grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>>?,
    ): List<OppgaveFraDatabaseForVisning> {
        val orderBy = if (sortering.isNotEmpty()) sortering.joinToString { it.nøkkelTilKolonne() } else "opprettet DESC"
        val egenskaperSomSkalEkskluderes = ekskluderEgenskaper.joinToString { "'$it'" }
        val ukategoriserteEgenskaper = grupperteFiltrerteEgenskaper?.tilSqlString(Egenskap.Kategori.Ukategorisert)
        val oppgavetypeEgenskaper = grupperteFiltrerteEgenskaper?.tilSqlString(Egenskap.Kategori.Oppgavetype)
        val periodetypeEgenskaper = grupperteFiltrerteEgenskaper?.tilSqlString(Egenskap.Kategori.Periodetype)
        val mottakerEgenskaper = grupperteFiltrerteEgenskaper?.tilSqlString(Egenskap.Kategori.Mottaker)
        val antallArbeidsforholdEgenskaper = grupperteFiltrerteEgenskaper?.tilSqlString(Egenskap.Kategori.Inntektskilde)
        val statusEgenskaper = grupperteFiltrerteEgenskaper?.tilSqlString(Egenskap.Kategori.Status)

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
            LEFT JOIN totrinnsvurdering ttv ON (ttv.vedtaksperiode_id = v.vedtaksperiode_id AND ttv.utbetaling_id_ref IS NULL)
            LEFT JOIN saksbehandler s ON t.saksbehandler_ref = s.oid
            LEFT JOIN pa_vent pv ON v.vedtaksperiode_id = pv.vedtaksperiode_id
            LEFT JOIN saksbehandler sb ON pv.saksbehandler_ref = sb.oid
            WHERE o.status = 'AvventerSaksbehandler'
                AND (:ingen_ukategoriserte_egenskaper OR egenskaper @> ARRAY[$ukategoriserteEgenskaper]::varchar[]) -- egenskaper saksbehandler har filtrert på
                AND (:ingen_oppgavetype_egenskaper OR egenskaper && ARRAY[$oppgavetypeEgenskaper]::varchar[]) -- egenskaper saksbehandler har filtrert på
                AND (:ingen_periodetype_egenskaper OR egenskaper && ARRAY[$periodetypeEgenskaper]::varchar[]) -- egenskaper saksbehandler har filtrert på
                AND (:ingen_mottakertype_egenskaper OR egenskaper && ARRAY[$mottakerEgenskaper]::varchar[]) -- egenskaper saksbehandler har filtrert på
                AND (:ingen_antallarbeidsforholdtype_egenskaper OR egenskaper && ARRAY[$antallArbeidsforholdEgenskaper]::varchar[]) -- egenskaper saksbehandler har filtrert på
                AND (:ingen_statustype_egenskaper OR egenskaper && ARRAY[$statusEgenskaper]::varchar[]) -- egenskaper saksbehandler har filtrert på
                AND NOT (egenskaper && ARRAY[$egenskaperSomSkalEkskluderes]::varchar[]) -- egenskaper saksbehandler ikke har tilgang til
                AND NOT (egenskaper && ARRAY['BESLUTTER']::varchar[] AND ttv.saksbehandler = :oid) -- hvis oppgaven er sendt til beslutter og saksbehandler var den som sendte
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
            ORDER BY $orderBy NULLS LAST
            OFFSET :offset
            LIMIT :limit
            """,
            "oid" to saksbehandlerOid,
            "offset" to offset,
            "limit" to limit,
            "egne_saker_pa_vent" to egneSakerPåVent,
            "egne_saker" to egneSaker,
            "tildelt" to tildelt,
            "ingen_ukategoriserte_egenskaper" to (ukategoriserteEgenskaper == null),
            "ingen_oppgavetype_egenskaper" to (oppgavetypeEgenskaper == null),
            "ingen_periodetype_egenskaper" to (periodetypeEgenskaper == null),
            "ingen_mottakertype_egenskaper" to (mottakerEgenskaper == null),
            "ingen_antallarbeidsforholdtype_egenskaper" to (antallArbeidsforholdEgenskaper == null),
            "ingen_statustype_egenskaper" to (statusEgenskaper == null),
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

    private fun Map<Egenskap.Kategori, List<EgenskapForDatabase>>.tilSqlString(kategori: Egenskap.Kategori) =
        get(kategori)?.joinToString { "'${it.name}'" }

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase =
        asSQL(
            """
            SELECT
                count(*) FILTER ( WHERE NOT o.egenskaper @> ARRAY['PÅ_VENT']::varchar[] ) AS antall_mine_saker,
                count(*) FILTER ( WHERE o.egenskaper @> ARRAY['PÅ_VENT']::varchar[] ) AS antall_mine_saker_på_vent
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

    fun updateOppgave(oppgave: Oppgave): Int =
        asSQL(
            """
            UPDATE oppgave
            SET ferdigstilt_av = :ferdigstiltAv, ferdigstilt_av_oid = :oid, status = :oppgavestatus::oppgavestatus, egenskaper = :egenskaper::varchar[], oppdatert = :oppdatert
            WHERE id=:oppgaveId
            """,
            "ferdigstiltAv" to oppgave.ferdigstiltAvIdent,
            "oid" to oppgave.ferdigstiltAvOid,
            "oppgavestatus" to status(oppgave.tilstand),
            "egenskaper" to oppgave.egenskaper.joinToString(prefix = "{", postfix = "}"),
            "oppdatert" to LocalDateTime.now(),
            "oppgaveId" to oppgave.id,
        ).update()

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
                LEFT JOIN (SELECT DISTINCT ON (vedtaksperiode_id) utbetaling_id, beslutter.ident as beslutter, saksbehandler.ident as saksbehandler
                         FROM totrinnsvurdering tv
                         INNER JOIN utbetaling_id ui ON ui.id = utbetaling_id_ref
                         INNER JOIN saksbehandler beslutter on tv.beslutter = beslutter.oid
                         INNER JOIN saksbehandler saksbehandler on tv.saksbehandler = saksbehandler.oid
                         WHERE (saksbehandler = :oid OR beslutter = :oid) AND tv.oppdatert::date = :fom::date
                     ) ttv ON ttv.utbetaling_id = o.utbetaling_id
            WHERE (ttv.utbetaling_id IS NOT NULL OR o.ferdigstilt_av_oid = :oid)
                AND (o.status in ('Ferdigstilt', 'AvventerSystem'))
                AND o.oppdatert::date = :fom::date
            ORDER BY o.oppdatert
            OFFSET :offset
            LIMIT :limit;
            """,
            "oid" to behandletAvOid,
            "fom" to LocalDate.now(),
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

    fun opprettOppgave(oppgave: Oppgave) {
        val vedtakRef = vedtakRef(oppgave.vedtaksperiodeId)
        val personRef = personRef(oppgave.vedtaksperiodeId)

        val (arbeidsgiverBeløp, personBeløp) = finnArbeidsgiverbeløpOgPersonbeløp(oppgave.vedtaksperiodeId, oppgave.utbetalingId)
        val mottaker = finnMottaker(arbeidsgiverBeløp > 0, personBeløp > 0)

        asSQL(
            """
            INSERT INTO oppgave (id, oppdatert, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, generasjon_ref, behandling_id, hendelse_id_godkjenningsbehov, utbetaling_id, mottaker, egenskaper, kan_avvises) 
            SELECT
                :id,
                :oppdatert,
                CAST(:oppgavestatus as oppgavestatus),
                :ferdigstiltAv,
                :ferdigstiltAvOid,
                :vedtakRef,
                (
                    SELECT unik_id FROM behandling WHERE vedtaksperiode_id = (
                        SELECT vedtaksperiode_id FROM vedtak v WHERE v.id = :vedtakRef
                    ) ORDER BY id DESC LIMIT 1
                ),
                :behandlingId,
                :godkjenningsbehovId,
                :utbetalingId,
                CAST(:mottaker as mottakertype),
                CAST(:egenskaper as varchar[]),
                :kanAvvises
            WHERE
                NOT EXISTS(
                    SELECT 1 FROM oppgave o
                        LEFT JOIN vedtak v on v.id = o.vedtak_ref
                        WHERE o.status='AvventerSaksbehandler'::oppgavestatus
                            AND v.person_ref=:personRef
                )
            """,
            "id" to oppgave.id,
            "oppdatert" to LocalDateTime.now(),
            "oppgavestatus" to oppgave.tilstand,
            "ferdigstiltAv" to oppgave.ferdigstiltAvIdent,
            "ferdigstiltAvOid" to oppgave.ferdigstiltAvOid,
            "vedtakRef" to vedtakRef,
            "behandlingId" to oppgave.behandlingId,
            "godkjenningsbehovId" to oppgave.godkjenningsbehovId,
            "utbetalingId" to oppgave.utbetalingId,
            "mottaker" to mottaker?.name,
            "egenskaper" to oppgave.egenskaper.joinToString(prefix = "{", postfix = "}"),
            "kanAvvises" to oppgave.kanAvvises,
            "personRef" to personRef,
        ).updateAndReturnGeneratedKey()
    }

    private fun finnArbeidsgiverbeløpOgPersonbeløp(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Pair<Int, Int> =
        asSQL(
            """
            SELECT SUM(ABS(arbeidsgiverbeløp)) as sumArbeidsgiverbeløp, SUM(ABS(personbeløp)) as sumPersonbeløp
            FROM utbetaling_id
            WHERE person_ref=(SELECT person_ref FROM utbetaling_id WHERE utbetaling_id=:utbetalingId) AND
                utbetaling_id.utbetaling_id IN (
                    SELECT utbetaling_id
                    FROM behandling
                    WHERE skjæringstidspunkt=(
                        SELECT skjæringstidspunkt
                            FROM behandling
                            WHERE vedtaksperiode_id=:vedtaksperiodeId AND tilstand='VidereBehandlingAvklares'
                        ) AND tilstand='VidereBehandlingAvklares'
                    )
            """,
            "utbetalingId" to utbetalingId,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single { row ->
            Pair(row.intOrNull("sumArbeidsgiverbeløp") ?: 0, row.intOrNull("sumPersonbeløp") ?: 0)
        }

    private fun tilstand(oppgavestatus: String): Oppgave.Tilstand {
        return when (oppgavestatus) {
            "AvventerSaksbehandler" -> Oppgave.AvventerSaksbehandler
            "AvventerSystem" -> Oppgave.AvventerSystem
            "Ferdigstilt" -> Oppgave.Ferdigstilt
            "Invalidert" -> Oppgave.Invalidert
            else -> throw IllegalStateException("Oppgavestatus $oppgavestatus er ikke en gyldig status")
        }
    }

    private fun status(tilstand: Oppgave.Tilstand): String {
        return when (tilstand) {
            Oppgave.AvventerSaksbehandler -> "AvventerSaksbehandler"
            Oppgave.AvventerSystem -> "AvventerSystem"
            Oppgave.Ferdigstilt -> "Ferdigstilt"
            Oppgave.Invalidert -> "Invalidert"
        }
    }

    private fun finnMottaker(
        harArbeidsgiverbeløp: Boolean,
        harPersonbeløp: Boolean,
    ): Mottaker? =
        when {
            harArbeidsgiverbeløp && harPersonbeløp -> Mottaker.BEGGE
            harPersonbeløp -> Mottaker.SYKMELDT
            harArbeidsgiverbeløp -> Mottaker.ARBEIDSGIVER
            else -> null
        }

    private fun OppgavesorteringForDatabase.nøkkelTilKolonne() =
        when (this.nøkkel) {
            SorteringsnøkkelForDatabase.TILDELT_TIL -> "navn"
            SorteringsnøkkelForDatabase.OPPRETTET -> "opprettet"
            SorteringsnøkkelForDatabase.TIDSFRIST -> "frist"
            SorteringsnøkkelForDatabase.SØKNAD_MOTTATT -> "opprinnelig_soknadsdato"
        } + if (this.stigende) " ASC" else " DESC"

    private fun personRef(vedtaksperiodeId: UUID): Long =
        asSQL(
            """
            SELECT person_ref FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId;
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.long("person_ref")
        }

    private fun vedtakRef(vedtaksperiodeId: UUID): Long =
        asSQL(
            """
            SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId;
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.long("id")
        }

    private enum class Mottaker {
        SYKMELDT,
        ARBEIDSGIVER,
        BEGGE,
        INGEN,
    }
}
