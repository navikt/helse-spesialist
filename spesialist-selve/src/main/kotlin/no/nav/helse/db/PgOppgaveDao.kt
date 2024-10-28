package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveDao(queryRunner: QueryRunner) : OppgaveDao, QueryRunner by queryRunner {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun finnGenerasjonId(oppgaveId: Long): UUID {
        return asSQL("SELECT generasjon_ref FROM oppgave WHERE id = :oppgaveId", "oppgaveId" to oppgaveId)
            .single { it.uuid("generasjon_ref") }
    }

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
        return asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            ORDER BY o.id DESC
            LIMIT 1
            """,
            "fodselsnummer" to fødselsnummer.toLong(),
        )
            .single {
                it.long("oppgaveId")
            }
    }

    override fun finnOppgave(id: Long): OppgaveFraDatabase? {
        return asSQL(
            """
            SELECT o.egenskaper, o.status, v.vedtaksperiode_id, o.ferdigstilt_av, o.ferdigstilt_av_oid, o.utbetaling_id, s.navn, s.epost, s.ident, s.oid, o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            LEFT JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE o.id = :oppgaveId
            ORDER BY o.id DESC LIMIT 1            
            """,
            "oppgaveId" to id,
        ).singleOrNull { row ->
            val egenskaper: List<EgenskapForDatabase> =
                row.array<String>("egenskaper").toList().map { enumValueOf(it) }
            OppgaveFraDatabase(
                id = id,
                egenskaper = egenskaper,
                status = row.string("status"),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                utbetalingId = row.uuid("utbetaling_id"),
                hendelseId = finnHendelseId(id),
                kanAvvises = row.boolean("kan_avvises"),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                tildelt =
                    row.uuidOrNull("oid")?.let {
                        SaksbehandlerFraDatabase(
                            epostadresse = row.string("epost"),
                            oid = it,
                            navn = row.string("navn"),
                            ident = row.string("ident"),
                        )
                    },
            )
        }
    }

    override fun finnOppgaveId(fødselsnummer: String): Long? {
        return asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o
            JOIN vedtak v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
                AND p.fodselsnummer = :fodselsnummer;                
            """,
            "fodselsnummer" to fødselsnummer.toLong(),
        )
            .singleOrNull {
                it.long("oppgaveId")
            }
    }

    override fun finnOppgaveId(utbetalingId: UUID): Long? {
        return asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o WHERE o.utbetaling_id = :utbetaling_id
            AND o.status NOT IN ('Invalidert'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
            """,
            "utbetaling_id" to utbetalingId,
        )
            .singleOrNull {
                it.long("oppgaveId")
            }
    }

    override fun finnVedtaksperiodeId(fødselsnummer: String): UUID {
        return asSQL(
            """
             SELECT v.vedtaksperiode_id as vedtaksperiode_id
            FROM oppgave o
                     JOIN vedtak v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            AND status = 'AvventerSaksbehandler'::oppgavestatus;
            """,
            "fodselsnummer" to fødselsnummer.toLong(),
        ).single {
            it.uuid("vedtaksperiode_id")
        }
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
        ).single {
            it.int("oppgave_count")
        } > 0

    override fun finnHendelseId(id: Long): UUID {
        return asSQL(
            """
            SELECT DISTINCT hendelse_id 
            FROM command_context 
            WHERE context_id = (SELECT command_context_id FROM oppgave WHERE id = :oppgaveId);            
            """,
            "oppgaveId" to id,
        ).single {
            it.uuid("hendelse_id")
        }
    }

    override fun invaliderOppgaveFor(fødselsnummer: String) {
        asSQL(
            """
            UPDATE oppgave o
            SET status = 'Invalidert'
            FROM oppgave o2
            JOIN vedtak v on v.id = o2.vedtak_ref
            JOIN person p on v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            and o.id = o2.id
            AND o.status = 'AvventerSaksbehandler'::oppgavestatus;             
            """,
            "fodselsnummer" to fødselsnummer.toLong(),
        ).update()
    }

    override fun reserverNesteId(): Long {
        return asSQL(
            """
            SELECT nextval(pg_get_serial_sequence('oppgave', 'id')) as neste_id              
            """,
        )
            .single {
                it.long("neste_id")
            }
    }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean {
        return asSQL(
            """
            SELECT EXISTS (
                SELECT 1 FROM oppgave WHERE id=:oppgaveId AND status IN('AvventerSaksbehandler'::oppgavestatus)
            )              
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.boolean(1)
        }
    }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID {
        return asSQL(
            """
            SELECT spleis_behandling_id FROM oppgave o
            INNER JOIN selve_vedtaksperiode_generasjon svg ON svg.unik_id = o.generasjon_ref
            WHERE o.id = :oppgaveId;              
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.uuid("spleis_behandling_id")
        }
    }

    override fun oppgaveDataForAutomatisering(oppgaveId: Long): OppgaveDataForAutomatisering? {
        return asSQL(
            """
            SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, h.id AS hendelseId, h.data AS godkjenningbehovJson, s.type as periodetype
            FROM vedtak v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN hendelse h ON h.id = (SELECT hendelse_id FROM command_context WHERE context_id = o.command_context_id LIMIT 1)
            INNER JOIN saksbehandleroppgavetype s ON s.vedtak_ref = v.id
            WHERE o.id = :oppgaveId 
            """,
            "oppgaveId" to oppgaveId,
        )
            .singleOrNull { row ->
                val json = objectMapper.readTree(row.string("godkjenningbehovJson"))
                val skjæringstidspunkt = json.path("Godkjenning").path("skjæringstidspunkt").asLocalDate()
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
                p.aktor_id,
                v.vedtaksperiode_id, 
                pi.fornavn, pi.mellomnavn, pi.etternavn, 
                o.egenskaper,
                s.oid, s.ident, s.epost, s.navn,
                o.opprettet,
                os.soknad_mottatt AS opprinnelig_soknadsdato,
                o.kan_avvises,
                pv.frist,
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
                        WHEN :tildelt THEN t.saksbehandler_ref IS NOT NULL
                        WHEN :tildelt = false THEN t.saksbehandler_ref IS NULL
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
                aktørId = row.string("aktor_id"),
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
            )
        }
    }

    private fun Map<Egenskap.Kategori, List<EgenskapForDatabase>>.tilSqlString(kategori: Egenskap.Kategori) =
        get(kategori)?.joinToString { "'${it.name}'" }

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase {
        return asSQL(
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
        )
            .singleOrNull { row ->
                AntallOppgaverFraDatabase(
                    antallMineSaker = row.int("antall_mine_saker"),
                    antallMineSakerPåVent = row.int("antall_mine_saker_på_vent"),
                )
            } ?: AntallOppgaverFraDatabase(antallMineSaker = 0, antallMineSakerPåVent = 0)
    }

    override fun finnFødselsnummer(oppgaveId: Long): String {
        return asSQL(
            """
            SELECT fodselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.long("fodselsnummer").toFødselsnummer()
        }
    }

    override fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: String,
        ferdigstiltAv: String?,
        oid: UUID?,
        egenskaper: List<EgenskapForDatabase>,
    ): Int {
        return asSQL(
            """
            UPDATE oppgave
            SET ferdigstilt_av = :ferdigstiltAv, ferdigstilt_av_oid = :oid, status = :oppgavestatus::oppgavestatus, egenskaper = :egenskaper::varchar[]
            WHERE id=:oppgaveId
            """,
            "ferdigstiltAv" to ferdigstiltAv,
            "oid" to oid,
            "oppgavestatus" to oppgavestatus,
            "egenskaper" to egenskaper.joinToString(prefix = "{", postfix = "}"),
            "oppgaveId" to oppgaveId,
        ).update()
    }

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean {
        return asSQL(
            """
            SELECT COUNT(1) AS oppgave_count FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status = 'Ferdigstilt'::oppgavestatus
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.int("oppgave_count")
        } > 0
    }

    override fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int,
        limit: Int,
    ): List<BehandletOppgaveFraDatabaseForVisning> {
        return asSQL(
            """
            SELECT 
                o.id as oppgave_id,
                p.aktor_id,
                o.egenskaper,
                o.oppdatert as ferdigstilt_tidspunkt,
                o.ferdigstilt_av,
                pi.fornavn, pi.mellomnavn, pi.etternavn,
                count(1) OVER() AS filtered_count
            FROM oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                LEFT JOIN (SELECT DISTINCT ON (vedtaksperiode_id) vedtaksperiode_id, saksbehandler, utbetaling_id
                         FROM totrinnsvurdering
                         INNER JOIN utbetaling_id ui on ui.id = utbetaling_id_ref
                         WHERE utbetaling_id_ref IS NOT NULL
                         ORDER BY vedtaksperiode_id, totrinnsvurdering.id DESC
                     ) ttv ON ttv.vedtaksperiode_id = v.vedtaksperiode_id AND ttv.utbetaling_id = o.utbetaling_id
            WHERE (ttv.saksbehandler = :oid OR o.ferdigstilt_av_oid = :oid) AND (o.status in ('Ferdigstilt', 'AvventerSystem'))
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
                aktørId = row.string("aktor_id"),
                egenskaper =
                    row.array<String>("egenskaper").map { enumValueOf<EgenskapForDatabase>(it) }
                        .toSet(),
                ferdigstiltTidspunkt = row.localDateTime("ferdigstilt_tidspunkt"),
                ferdigstiltAv = row.stringOrNull("ferdigstilt_av"),
                navn =
                    PersonnavnFraDatabase(
                        row.string("fornavn"),
                        row.stringOrNull("mellomnavn"),
                        row.string("etternavn"),
                    ),
                filtrertAntall = row.int("filtered_count"),
            )
        }
    }

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? {
        return asSQL(
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
    }

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? {
        return asSQL(
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
    }

    override fun opprettOppgave(
        id: Long,
        commandContextId: UUID,
        egenskaper: List<EgenskapForDatabase>,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        kanAvvises: Boolean,
    ): Long {
        val vedtakRef = vedtakRef(vedtaksperiodeId)
        val personRef = personRef(vedtaksperiodeId)

        val (arbeidsgiverBeløp, personBeløp) = finnArbeidsgiverbeløpOgPersonbeløp(vedtaksperiodeId, utbetalingId)
        val mottaker = finnMottaker(arbeidsgiverBeløp > 0, personBeløp > 0)

        return asSQL(
            """
            INSERT INTO oppgave(id, oppdatert, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, generasjon_ref, command_context_id, utbetaling_id, mottaker, egenskaper, kan_avvises)      
            SELECT 
                :id, 
                now(), 
                CAST(:oppgavestatus as oppgavestatus), 
                :ferdigstiltAv, 
                :ferdigstiltAvOid, 
                :vedtakRef,
                (
                    SELECT unik_id FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = (
                        SELECT vedtaksperiode_id FROM vedtak v WHERE v.id = :vedtakRef
                    ) ORDER BY id DESC LIMIT 1
                ),
                :commandContextId, 
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
            "id" to id,
            "oppgavestatus" to "AvventerSaksbehandler",
            "ferdigstiltAv" to null,
            "ferdigstiltAvOid" to null,
            "vedtakRef" to vedtakRef,
            "commandContextId" to commandContextId,
            "utbetalingId" to utbetalingId,
            "mottaker" to mottaker?.name,
            "egenskaper" to egenskaper.joinToString(prefix = "{", postfix = "}"),
            "kanAvvises" to kanAvvises,
            "personRef" to personRef,
        ).updateAndReturnGeneratedKey()
    }

    private fun finnArbeidsgiverbeløpOgPersonbeløp(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Pair<Int, Int> {
        return asSQL(
            """
            SELECT SUM(ABS(arbeidsgiverbeløp)) as sumArbeidsgiverbeløp, SUM(ABS(personbeløp)) as sumPersonbeløp
            FROM utbetaling_id 
            WHERE person_ref=(SELECT person_ref FROM utbetaling_id WHERE utbetaling_id=:utbetalingId) AND 
                utbetaling_id.utbetaling_id IN (
                    SELECT utbetaling_id 
                    FROM selve_vedtaksperiode_generasjon 
                    WHERE skjæringstidspunkt=(
                        SELECT skjæringstidspunkt 
                            FROM selve_vedtaksperiode_generasjon
                            WHERE vedtaksperiode_id=:vedtaksperiodeId AND tilstand='VidereBehandlingAvklares'
                        ) AND tilstand='VidereBehandlingAvklares'
                    )
            """,
            "utbetalingId" to utbetalingId,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single { row ->
            Pair(row.intOrNull("sumArbeidsgiverbeløp") ?: 0, row.intOrNull("sumPersonbeløp") ?: 0)
        }
    }

    private fun finnMottaker(
        harArbeidsgiverbeløp: Boolean,
        harPersonbeløp: Boolean,
    ): Mottaker? {
        return when {
            harArbeidsgiverbeløp && harPersonbeløp -> Mottaker.BEGGE
            harPersonbeløp -> Mottaker.SYKMELDT
            harArbeidsgiverbeløp -> Mottaker.ARBEIDSGIVER
            else -> null
        }
    }

    private fun OppgavesorteringForDatabase.nøkkelTilKolonne() =
        when (this.nøkkel) {
            SorteringsnøkkelForDatabase.TILDELT_TIL -> "navn"
            SorteringsnøkkelForDatabase.OPPRETTET -> "opprettet"
            SorteringsnøkkelForDatabase.TIDSFRIST -> "frist"
            SorteringsnøkkelForDatabase.SØKNAD_MOTTATT -> "opprinnelig_soknadsdato"
        } + if (this.stigende) " ASC" else " DESC"

    private fun personRef(vedtaksperiodeId: UUID): Long {
        return asSQL(
            """
            SELECT person_ref FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId;
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.long("person_ref")
        }
    }

    private fun vedtakRef(vedtaksperiodeId: UUID): Long {
        return asSQL(
            """
            SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId;  
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.long("id")
        }
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
