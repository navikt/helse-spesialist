package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.mediator.oppgave.OppgaveRepository.OppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.Side
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.dao.PgTildelingDao
import no.nav.helse.spesialist.domain.PersonId
import no.nav.helse.spesialist.domain.PåVentId
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveRepository private constructor(
    queryRunner: QueryRunner,
) : QueryRunner by queryRunner,
    OppgaveRepository {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    private val tildelingDao: PgTildelingDao = PgTildelingDao(queryRunner)

    override fun lagre(oppgave: Oppgave) {
        if (finnesAnnenAktivOppgavePåPerson(oppgave.id, oppgave.vedtaksperiodeId)) {
            error("Forventer ikke å finne annen aktiv oppgave for personen")
        }
        lagreOppgave(oppgave)
        lagreTildeling(oppgave)
    }

    override fun finn(id: Long): Oppgave? = finnOppgave(id)

    override fun finnSisteOppgaveForUtbetaling(utbetalingId: UUID): OppgaveRepository.OppgaveTilstandStatusOgGodkjenningsbehov? =
        asSQL(
            """
            SELECT id, status, hendelse_id_godkjenningsbehov, utbetaling_id FROM oppgave
            WHERE utbetaling_id = :utbetaling_id
            ORDER BY id DESC LIMIT 1
        """,
            "utbetaling_id" to utbetalingId,
        ).singleOrNull { row ->
            OppgaveRepository.OppgaveTilstandStatusOgGodkjenningsbehov(
                id = row.long("id"),
                tilstand = tilstand(row.string("status")),
                godkjenningsbehovId = row.uuid("hendelse_id_godkjenningsbehov"),
                utbetalingId = row.uuid("utbetaling_id"),
            )
        }

    private fun lagreOppgave(oppgave: Oppgave) {
        asSQL(
            """
            INSERT INTO oppgave (
                id,
                opprettet,
                første_opprettet,
                oppdatert,
                status, 
                ferdigstilt_av, 
                ferdigstilt_av_oid, 
                vedtak_ref, 
                generasjon_ref, 
                behandling_id, 
                hendelse_id_godkjenningsbehov, 
                utbetaling_id, 
                mottaker, 
                egenskaper, 
                kan_avvises
            ) 
            SELECT
                :id,
                :opprettet,
                :foerste_opprettet,
                :oppdatert,
                CAST(:oppgavestatus as oppgavestatus),
                :ferdigstilt_av,
                :ferdigstilt_av_oid,
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id),
                (SELECT unik_id FROM behandling WHERE spleis_behandling_id = :behandling_id),
                :behandling_id,
                :godkjenningsbehov_id,
                :utbetaling_id,
                null,
                CAST(:egenskaper as varchar[]),
                :kan_avvises
            ON CONFLICT(id) DO UPDATE SET 
                oppdatert = excluded.oppdatert,
                status = excluded.status,
                ferdigstilt_av = excluded.ferdigstilt_av,
                ferdigstilt_av_oid = excluded.ferdigstilt_av_oid,
                egenskaper = excluded.egenskaper,
                kan_avvises = excluded.kan_avvises,
                hendelse_id_godkjenningsbehov = excluded.hendelse_id_godkjenningsbehov
            """,
            "id" to oppgave.id,
            "opprettet" to oppgave.opprettet,
            "foerste_opprettet" to oppgave.førsteOpprettet,
            "oppdatert" to LocalDateTime.now(),
            "oppgavestatus" to status(oppgave.tilstand),
            "ferdigstilt_av" to oppgave.ferdigstiltAvIdent,
            "ferdigstilt_av_oid" to oppgave.ferdigstiltAvOid,
            "vedtaksperiode_id" to oppgave.vedtaksperiodeId,
            "behandling_id" to oppgave.behandlingId,
            "godkjenningsbehov_id" to oppgave.godkjenningsbehovId,
            "utbetaling_id" to oppgave.utbetalingId,
            "egenskaper" to oppgave.egenskaper.somDbArray { it.toDb() },
            "kan_avvises" to oppgave.kanAvvises,
        ).update()
    }

    override fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime? =
        asSQL(
            "SELECT min(opprettet) FROM oppgave WHERE behandling_id = :behandling_id",
            "behandling_id" to behandlingId,
        ).singleOrNull { it.localDateTimeOrNull(1) }

    override fun finnOppgaveProjeksjoner(
        minstEnAvEgenskapene: List<Set<Egenskap>>,
        ingenAvEgenskapene: Set<Egenskap>,
        erTildelt: Boolean?,
        tildeltTilOid: SaksbehandlerOid?,
        erPåVent: Boolean?,
        ikkeSendtTilBeslutterAvOid: SaksbehandlerOid?,
        sorterPå: SorteringsnøkkelForDatabase,
        sorteringsrekkefølge: Sorteringsrekkefølge,
        sidetall: Int,
        sidestørrelse: Int,
    ): Side<OppgaveProjeksjon> {
        val parameterMap = mutableMapOf<String, Any>()
        val sql =
            buildString {
                append(
                    """
                    SELECT
                        o.id as oppgave_id,
                        o.egenskaper,
                        o.første_opprettet,
                        v.person_ref,
                        os.soknad_mottatt AS opprinnelig_soknadsdato,
                        t.saksbehandler_ref as tildelt_til_oid,
                        pv.id AS på_vent_id,
                        count(1) OVER() AS filtered_count
                    FROM oppgave o
                    INNER JOIN vedtak v ON o.vedtak_ref = v.id
                    INNER JOIN opprinnelig_soknadsdato os ON os.vedtaksperiode_id = v.vedtaksperiode_id
                    LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
                    LEFT JOIN totrinnsvurdering ttv ON (ttv.person_ref = v.person_ref AND ttv.tilstand != 'GODKJENT')
                    LEFT JOIN pa_vent pv ON v.vedtaksperiode_id = pv.vedtaksperiode_id
                    WHERE o.status = 'AvventerSaksbehandler'
                    """.trimIndent(),
                )
                minstEnAvEgenskapene.filter { it.isNotEmpty() }.forEachIndexed { index, minstEnAvEgenskapeneGruppe ->
                    // inkluder alle oppgaver som har minst en av de valgte oppgavetype
                    val parameterName = "minstEnAvEgenskapene$index"
                    append("AND (egenskaper && :$parameterName::varchar[])\n")
                    parameterMap[parameterName] = minstEnAvEgenskapeneGruppe.tilDatabaseArray()
                }
                ingenAvEgenskapene.takeUnless { it.isEmpty() }.let {
                    val parameterName = "ingenAvEgenskapene"
                    append("AND NOT (egenskaper && :$parameterName::varchar[])\n")
                    parameterMap[parameterName] = ingenAvEgenskapene.tilDatabaseArray()
                }
                if (ikkeSendtTilBeslutterAvOid != null) {
                    val parameterName = "ikkeSendtTilBeslutterAvOid"
                    append("AND NOT ('BESLUTTER' = ANY(egenskaper) AND ttv.saksbehandler = :$parameterName)\n")
                    parameterMap[parameterName] = ikkeSendtTilBeslutterAvOid.value
                }
                if (erPåVent != null) {
                    if (erPåVent) {
                        append("AND 'PÅ_VENT' = ANY(o.egenskaper)\n")
                    } else {
                        append("AND NOT 'PÅ_VENT' = ANY(o.egenskaper)\n")
                    }
                }
                if (tildeltTilOid != null) {
                    val parameterName = "tildeltTilOid"
                    append("AND t.saksbehandler_ref = :$parameterName\n")
                    parameterMap[parameterName] = tildeltTilOid.value
                }
                if (erTildelt != null) {
                    if (erTildelt) {
                        append("AND t.saksbehandler_ref IS NOT NULL\n")
                    } else {
                        append("AND t.saksbehandler_ref IS NULL\n")
                    }
                }
                append("ORDER BY ${tilOrderBy(sorterPå, sorteringsrekkefølge)}\n")

                val offsetParameterName = "offset"
                append("OFFSET :$offsetParameterName\n")
                parameterMap[offsetParameterName] = (sidetall - 1) * sidestørrelse

                val limitParameterName = "limit"
                append("LIMIT :$limitParameterName\n")
                parameterMap[limitParameterName] = sidestørrelse
            }
        return queryOf(statement = sql, paramMap = parameterMap)
            .list { row ->
                row.long("filtered_count") to
                    OppgaveProjeksjon(
                        id = row.long("oppgave_id"),
                        personId = PersonId(row.int("person_ref")),
                        egenskaper =
                            row
                                .array<String>("egenskaper")
                                .map { enumValueOf<EgenskapForDatabase>(it) }
                                .tilModellversjoner(),
                        tildeltTilOid = row.uuidOrNull("tildelt_til_oid")?.let(::SaksbehandlerOid),
                        opprettetTidspunkt = row.instant("første_opprettet"),
                        opprinneligSøknadstidspunkt = row.instant("opprinnelig_soknadsdato"),
                        påVentId = row.intOrNull("på_vent_id")?.let(::PåVentId),
                    )
            }.let { listeMedTotaltAntallOgElement ->
                Side(
                    totaltAntall = listeMedTotaltAntallOgElement.firstOrNull()?.first ?: 0L,
                    sidetall = sidetall,
                    sidestørrelse = sidestørrelse,
                    elementer = listeMedTotaltAntallOgElement.map(Pair<Long, OppgaveProjeksjon>::second),
                )
            }
    }

    private fun Collection<Enum<*>>.tilDatabaseArray(): String = joinToString(prefix = "{", postfix = "}") { it.name }

    private fun tilOrderBy(
        sorterPå: SorteringsnøkkelForDatabase,
        sorteringsrekkefølge: Sorteringsrekkefølge,
    ): String =
        buildString {
            append("${sorterPå.tilOrderByKolonne()} ${sorteringsrekkefølge.tilAscEllerDesc()} NULLS LAST")
            if (sorterPå != SorteringsnøkkelForDatabase.OPPRETTET) {
                append(", ${SorteringsnøkkelForDatabase.OPPRETTET.tilOrderByKolonne()} DESC NULLS LAST")
            }
        }

    private fun Collection<EgenskapForDatabase>.tilModellversjoner() = mapNotNull { it.tilModellversjon() }.toSet()

    private fun EgenskapForDatabase.tilModellversjon(): Egenskap? =
        when (this) {
            EgenskapForDatabase.RISK_QA -> Egenskap.RISK_QA
            EgenskapForDatabase.FORTROLIG_ADRESSE -> Egenskap.FORTROLIG_ADRESSE
            EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE -> Egenskap.STRENGT_FORTROLIG_ADRESSE
            EgenskapForDatabase.EGEN_ANSATT -> Egenskap.EGEN_ANSATT
            EgenskapForDatabase.BESLUTTER -> Egenskap.BESLUTTER
            EgenskapForDatabase.SPESIALSAK -> Egenskap.SPESIALSAK
            EgenskapForDatabase.REVURDERING -> Egenskap.REVURDERING
            EgenskapForDatabase.SØKNAD -> Egenskap.SØKNAD
            EgenskapForDatabase.STIKKPRØVE -> Egenskap.STIKKPRØVE
            EgenskapForDatabase.UTBETALING_TIL_SYKMELDT -> Egenskap.UTBETALING_TIL_SYKMELDT
            EgenskapForDatabase.DELVIS_REFUSJON -> Egenskap.DELVIS_REFUSJON
            EgenskapForDatabase.UTBETALING_TIL_ARBEIDSGIVER -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
            EgenskapForDatabase.INGEN_UTBETALING -> Egenskap.INGEN_UTBETALING
            EgenskapForDatabase.HASTER -> Egenskap.HASTER
            EgenskapForDatabase.RETUR -> Egenskap.RETUR
            EgenskapForDatabase.VERGEMÅL -> Egenskap.VERGEMÅL
            EgenskapForDatabase.EN_ARBEIDSGIVER -> Egenskap.EN_ARBEIDSGIVER
            EgenskapForDatabase.FLERE_ARBEIDSGIVERE -> Egenskap.FLERE_ARBEIDSGIVERE
            EgenskapForDatabase.UTLAND -> Egenskap.UTLAND
            EgenskapForDatabase.FORLENGELSE -> Egenskap.FORLENGELSE
            EgenskapForDatabase.FORSTEGANGSBEHANDLING -> Egenskap.FORSTEGANGSBEHANDLING
            EgenskapForDatabase.INFOTRYGDFORLENGELSE -> Egenskap.INFOTRYGDFORLENGELSE
            EgenskapForDatabase.OVERGANG_FRA_IT -> Egenskap.OVERGANG_FRA_IT
            EgenskapForDatabase.SKJØNNSFASTSETTELSE -> Egenskap.SKJØNNSFASTSETTELSE
            EgenskapForDatabase.PÅ_VENT -> Egenskap.PÅ_VENT
            EgenskapForDatabase.TILBAKEDATERT -> Egenskap.TILBAKEDATERT
            EgenskapForDatabase.GOSYS -> Egenskap.GOSYS
            EgenskapForDatabase.MANGLER_IM -> Egenskap.MANGLER_IM
            EgenskapForDatabase.MEDLEMSKAP -> Egenskap.MEDLEMSKAP
            EgenskapForDatabase.GRUNNBELØPSREGULERING -> Egenskap.GRUNNBELØPSREGULERING
            EgenskapForDatabase.SELVSTENDIG_NÆRINGSDRIVENDE -> Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE
            EgenskapForDatabase.ARBEIDSTAKER -> Egenskap.ARBEIDSTAKER
            // Gammel egenskap fra tidligere iterasjon av tilkommen inntekt, skal overses
            EgenskapForDatabase.TILKOMMEN -> null
        }

    private fun Sorteringsrekkefølge.tilAscEllerDesc(): String =
        when (this) {
            Sorteringsrekkefølge.STIGENDE -> "ASC"
            Sorteringsrekkefølge.SYNKENDE -> "DESC"
        }

    private fun SorteringsnøkkelForDatabase.tilOrderByKolonne(): String =
        when (this) {
            SorteringsnøkkelForDatabase.TILDELT_TIL -> "navn"
            SorteringsnøkkelForDatabase.OPPRETTET -> "første_opprettet"
            SorteringsnøkkelForDatabase.TIDSFRIST -> "frist"
            SorteringsnøkkelForDatabase.SØKNAD_MOTTATT -> "opprinnelig_soknadsdato"
        }

    private fun lagreTildeling(oppgave: Oppgave) {
        val tildeltTil = oppgave.tildeltTil
        if (tildeltTil != null) {
            tildelingDao.tildel(oppgave.id, tildeltTil)
        } else {
            tildelingDao.avmeld(oppgave.id)
        }
    }

    private fun finnesAnnenAktivOppgavePåPerson(
        oppgaveId: Long,
        vedtaksperiodeId: UUID,
    ): Boolean =
        asSQL(
            """
            SELECT 1 FROM oppgave o 
                JOIN vedtak v ON o.vedtak_ref = v.id
                JOIN person p on v.person_ref = p.id
            WHERE p.id = (
                SELECT p.id FROM person p
                JOIN vedtak v ON v.person_ref = p.id
                WHERE v.vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.id != :oppgave_id 
            AND o.status = 'AvventerSaksbehandler'
            """.trimIndent(),
            "oppgave_id" to oppgaveId,
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).singleOrNull { it.boolean(1) } ?: false

    private fun finnOppgave(id: Long): Oppgave? =
        asSQL(
            """
            SELECT 
                o.egenskaper, 
                o.opprettet, 
                o.første_opprettet, 
                o.status, 
                v.vedtaksperiode_id, 
                o.behandling_id, 
                o.hendelse_id_godkjenningsbehov, 
                o.ferdigstilt_av, 
                o.ferdigstilt_av_oid, 
                o.utbetaling_id,
                t.saksbehandler_ref, 
                o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            WHERE o.id = :oppgaveId
            ORDER BY o.id DESC LIMIT 1
            """,
            "oppgaveId" to id,
        ).singleOrNull { row ->
            Oppgave.fraLagring(
                id = id,
                opprettet = row.localDateTime("opprettet"),
                førsteOpprettet = row.localDateTimeOrNull("første_opprettet"),
                egenskaper = row.array<String>("egenskaper").mapNotNull { it.fromDb() }.toSet(),
                tilstand = tilstand(row.string("status")),
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                behandlingId = row.uuid("behandling_id"),
                utbetalingId = row.uuid("utbetaling_id"),
                godkjenningsbehovId = row.uuid("hendelse_id_godkjenningsbehov"),
                kanAvvises = row.boolean("kan_avvises"),
                ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.uuidOrNull("ferdigstilt_av_oid"),
                tildeltTil = row.uuidOrNull("saksbehandler_ref")?.let(::SaksbehandlerOid),
            )
        }

    private fun tilstand(oppgavestatus: String): Oppgave.Tilstand =
        when (oppgavestatus) {
            "AvventerSaksbehandler" -> Oppgave.AvventerSaksbehandler
            "AvventerSystem" -> Oppgave.AvventerSystem
            "Ferdigstilt" -> Oppgave.Ferdigstilt
            "Invalidert" -> Oppgave.Invalidert
            else -> throw IllegalStateException("Oppgavestatus $oppgavestatus er ikke en gyldig status")
        }

    private fun status(tilstand: Oppgave.Tilstand): String =
        when (tilstand) {
            Oppgave.AvventerSaksbehandler -> "AvventerSaksbehandler"
            Oppgave.AvventerSystem -> "AvventerSystem"
            Oppgave.Ferdigstilt -> "Ferdigstilt"
            Oppgave.Invalidert -> "Invalidert"
        }

    private fun Egenskap.toDb() =
        when (this) {
            Egenskap.RISK_QA -> "RISK_QA"
            Egenskap.FORTROLIG_ADRESSE -> "FORTROLIG_ADRESSE"
            Egenskap.STRENGT_FORTROLIG_ADRESSE -> "STRENGT_FORTROLIG_ADRESSE"
            Egenskap.EGEN_ANSATT -> "EGEN_ANSATT"
            Egenskap.BESLUTTER -> "BESLUTTER"
            Egenskap.SPESIALSAK -> "SPESIALSAK"
            Egenskap.REVURDERING -> "REVURDERING"
            Egenskap.SØKNAD -> "SØKNAD"
            Egenskap.STIKKPRØVE -> "STIKKPRØVE"
            Egenskap.UTBETALING_TIL_SYKMELDT -> "UTBETALING_TIL_SYKMELDT"
            Egenskap.DELVIS_REFUSJON -> "DELVIS_REFUSJON"
            Egenskap.UTBETALING_TIL_ARBEIDSGIVER -> "UTBETALING_TIL_ARBEIDSGIVER"
            Egenskap.INGEN_UTBETALING -> "INGEN_UTBETALING"
            Egenskap.EN_ARBEIDSGIVER -> "EN_ARBEIDSGIVER"
            Egenskap.FLERE_ARBEIDSGIVERE -> "FLERE_ARBEIDSGIVERE"
            Egenskap.FORLENGELSE -> "FORLENGELSE"
            Egenskap.FORSTEGANGSBEHANDLING -> "FORSTEGANGSBEHANDLING"
            Egenskap.INFOTRYGDFORLENGELSE -> "INFOTRYGDFORLENGELSE"
            Egenskap.OVERGANG_FRA_IT -> "OVERGANG_FRA_IT"
            Egenskap.UTLAND -> "UTLAND"
            Egenskap.HASTER -> "HASTER"
            Egenskap.RETUR -> "RETUR"
            Egenskap.SKJØNNSFASTSETTELSE -> "SKJØNNSFASTSETTELSE"
            Egenskap.PÅ_VENT -> "PÅ_VENT"
            Egenskap.TILBAKEDATERT -> "TILBAKEDATERT"
            Egenskap.GOSYS -> "GOSYS"
            Egenskap.MANGLER_IM -> "MANGLER_IM"
            Egenskap.MEDLEMSKAP -> "MEDLEMSKAP"
            Egenskap.VERGEMÅL -> "VERGEMÅL"
            Egenskap.GRUNNBELØPSREGULERING -> "GRUNNBELØPSREGULERING"
            Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE -> "SELVSTENDIG_NÆRINGSDRIVENDE"
            Egenskap.ARBEIDSTAKER -> "ARBEIDSTAKER"
        }

    private fun String.fromDb(): Egenskap? =
        when (this) {
            "RISK_QA" -> Egenskap.RISK_QA
            "FORTROLIG_ADRESSE" -> Egenskap.FORTROLIG_ADRESSE
            "STRENGT_FORTROLIG_ADRESSE" -> Egenskap.STRENGT_FORTROLIG_ADRESSE
            "EGEN_ANSATT" -> Egenskap.EGEN_ANSATT
            "BESLUTTER" -> Egenskap.BESLUTTER
            "SPESIALSAK" -> Egenskap.SPESIALSAK
            "REVURDERING" -> Egenskap.REVURDERING
            "SØKNAD" -> Egenskap.SØKNAD
            "STIKKPRØVE" -> Egenskap.STIKKPRØVE
            "UTBETALING_TIL_SYKMELDT" -> Egenskap.UTBETALING_TIL_SYKMELDT
            "DELVIS_REFUSJON" -> Egenskap.DELVIS_REFUSJON
            "UTBETALING_TIL_ARBEIDSGIVER" -> Egenskap.UTBETALING_TIL_ARBEIDSGIVER
            "INGEN_UTBETALING" -> Egenskap.INGEN_UTBETALING
            "EN_ARBEIDSGIVER" -> Egenskap.EN_ARBEIDSGIVER
            "FLERE_ARBEIDSGIVERE" -> Egenskap.FLERE_ARBEIDSGIVERE
            "FORLENGELSE" -> Egenskap.FORLENGELSE
            "FORSTEGANGSBEHANDLING" -> Egenskap.FORSTEGANGSBEHANDLING
            "INFOTRYGDFORLENGELSE" -> Egenskap.INFOTRYGDFORLENGELSE
            "OVERGANG_FRA_IT" -> Egenskap.OVERGANG_FRA_IT
            "UTLAND" -> Egenskap.UTLAND
            "HASTER" -> Egenskap.HASTER
            "RETUR" -> Egenskap.RETUR
            "SKJØNNSFASTSETTELSE" -> Egenskap.SKJØNNSFASTSETTELSE
            "PÅ_VENT" -> Egenskap.PÅ_VENT
            "TILBAKEDATERT" -> Egenskap.TILBAKEDATERT
            "GOSYS" -> Egenskap.GOSYS
            "MANGLER_IM" -> Egenskap.MANGLER_IM
            "MEDLEMSKAP" -> Egenskap.MEDLEMSKAP
            "VERGEMÅL" -> Egenskap.VERGEMÅL
            "GRUNNBELØPSREGULERING" -> Egenskap.GRUNNBELØPSREGULERING
            "SELVSTENDIG_NÆRINGSDRIVENDE" -> Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE
            "ARBEIDSTAKER" -> Egenskap.ARBEIDSTAKER
            // Gammel egenskap fra tidligere iterasjon av tilkommen inntekt, skal overses
            "TILKOMMEN" -> null
            else -> error("Ukjent oppgaveegenskap")
        }
}
