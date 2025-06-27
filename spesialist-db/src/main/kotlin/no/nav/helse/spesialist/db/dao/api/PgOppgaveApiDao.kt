package no.nav.helse.spesialist.db.dao.api

import kotliquery.Row
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.KommentarFraDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.db.PaVentInfoFraDatabase
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveApiDao internal constructor(dataSource: DataSource) :
    QueryRunner by MedDataSource(dataSource),
    OppgaveApiDao {
        override fun finnOppgaveId(fødselsnummer: String) =
            asSQL(
                """
                SELECT o.id as oppgaveId FROM oppgave o
                JOIN vedtak v ON v.id = o.vedtak_ref
                JOIN person p ON v.person_ref = p.id
                WHERE p.fødselsnummer = :fodselsnummer AND status = 'AvventerSaksbehandler'::oppgavestatus;
                """.trimIndent(),
                "fodselsnummer" to fødselsnummer,
            ).singleOrNull { it.long("oppgaveId") }

        override fun finnPeriodeoppgave(vedtaksperiodeId: UUID) =
            asSQL(
                """
                SELECT o.id, o.kan_avvises
                FROM oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                WHERE v.vedtaksperiode_id = :vedtaksperiodeId 
                    AND status = 'AvventerSaksbehandler'::oppgavestatus 
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
            ).singleOrNull { OppgaveForPeriodevisningDto(id = it.string("id"), kanAvvises = it.boolean("kan_avvises")) }

        override fun finnFødselsnummer(oppgaveId: Long) =
            asSQL(
                """
                SELECT fødselsnummer from person
                INNER JOIN vedtak v on person.id = v.person_ref
                INNER JOIN oppgave o on v.id = o.vedtak_ref
                WHERE o.id = :oppgaveId
                """.trimIndent(),
                "oppgaveId" to oppgaveId,
            ).single { it.string("fødselsnummer") }

        override fun finnOppgaverForVisning(
            ekskluderEgenskaper: List<String>,
            saksbehandlerOid: UUID,
            offset: Int,
            limit: Int,
            sortering: List<OppgavesorteringForDatabase>,
            egneSakerPåVent: Boolean,
            egneSaker: Boolean,
            tildelt: Boolean?,
            grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>>,
        ): List<OppgaveFraDatabaseForVisning> {
            val orderBy = if (sortering.isNotEmpty()) sortering.joinToString { it.nøkkelTilKolonne() } else "opprettet DESC"
            val ukategoriserteEgenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Ukategorisert]
            val oppgavetypeegenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Oppgavetype]
            val periodetypeegenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Periodetype]
            val mottakeregenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Mottaker]
            val antallArbeidsforholdEgenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Inntektskilde]
            val statusegenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Status]
            val inntektsforhold = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Inntektsforhold]

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
                AND (:inntektsforhold = '{}' OR egenskaper && :inntektsforhold::varchar[]) -- inkluder alle oppgaver som har minst en av de valgte inntektsforhold
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
                "ukategoriserte_egenskaper" to ukategoriserteEgenskaper.somDbArray(),
                "oppgavetypeegenskaper" to oppgavetypeegenskaper.somDbArray(),
                "periodetypeegenskaper" to periodetypeegenskaper.somDbArray(),
                "mottakeregenskaper" to mottakeregenskaper.somDbArray(),
                "antall_arbeidsforhold_egenskaper" to antallArbeidsforholdEgenskaper.somDbArray(),
                "statusegenskaper" to statusegenskaper.somDbArray(),
                "inntektsforhold" to inntektsforhold.somDbArray(),
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

        private fun OppgavesorteringForDatabase.nøkkelTilKolonne() =
            when (nøkkel) {
                SorteringsnøkkelForDatabase.TILDELT_TIL -> "navn".direction(stigende).nullsLast()
                SorteringsnøkkelForDatabase.OPPRETTET -> "opprettet".direction(stigende)
                SorteringsnøkkelForDatabase.TIDSFRIST -> "frist".direction(stigende).nullsLast()
                SorteringsnøkkelForDatabase.SØKNAD_MOTTATT -> "opprinnelig_soknadsdato".direction(stigende)
            }

        private fun String.direction(stigende: Boolean) = if (stigende) "$this ASC" else "$this DESC"

        private fun String.nullsLast() = "$this NULLS LAST"
    }
