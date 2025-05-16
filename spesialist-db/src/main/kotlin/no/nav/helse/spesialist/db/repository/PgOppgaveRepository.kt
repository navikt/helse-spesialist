package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.dao.PgTildelingDao
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveRepository private constructor(queryRunner: QueryRunner) : QueryRunner by queryRunner, OppgaveRepository {
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

    override fun finn(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave? {
        return finnOppgave(id, tilgangskontroll)
    }

    override fun finnSisteOppgaveTilstandForUtbetaling(utbetalingId: UUID): Oppgave.Tilstand? =
        asSQL(
            """
            SELECT status FROM oppgave
            WHERE utbetaling_id = :utbetaling_id
            ORDER BY id DESC LIMIT 1
        """,
            "utbetaling_id" to utbetalingId,
        ).singleOrNull { row ->
            tilstand(row.string("status"))
        }

    private fun lagreOppgave(oppgave: Oppgave) {
        asSQL(
            """
            INSERT INTO oppgave (
                id,
                opprettet,
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
            "opprettet" to LocalDateTime.now(),
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

    private fun lagreTildeling(oppgave: Oppgave) {
        val tildeltTil = oppgave.tildeltTil
        if (tildeltTil != null) {
            tildelingDao.tildel(oppgave.id, tildeltTil.oid)
        } else {
            tildelingDao.avmeld(oppgave.id)
        }
    }

    private fun finnesAnnenAktivOppgavePåPerson(
        oppgaveId: Long,
        vedtaksperiodeId: UUID,
    ): Boolean {
        return asSQL(
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
            "oppgave_id" to oppgaveId, "vedtaksperiode_id" to vedtaksperiodeId,
        ).singleOrNull { it.boolean(1) } ?: false
    }

    private fun finnOppgave(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave? {
        return asSQL(
            """
            SELECT 
                o.egenskaper, 
                o.status, 
                v.vedtaksperiode_id, 
                o.behandling_id, 
                o.hendelse_id_godkjenningsbehov, 
                o.ferdigstilt_av, 
                o.ferdigstilt_av_oid, 
                o.utbetaling_id, 
                s.navn, 
                s.epost, 
                s.ident, 
                s.oid, 
                o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            LEFT JOIN tildeling t on o.id = t.oppgave_id_ref
            LEFT JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE o.id = :oppgaveId
            ORDER BY o.id DESC LIMIT 1
            """,
            "oppgaveId" to id,
        ).singleOrNull { row ->
            val egenskaper = row.array<String>("egenskaper").map { it.fromDb() }.toSet()
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
            Egenskap.TILKOMMEN -> "TILKOMMEN"
            Egenskap.GRUNNBELØPSREGULERING -> "GRUNNBELØPSREGULERING"
        }

    private fun String.fromDb() =
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
            "TILKOMMEN" -> Egenskap.TILKOMMEN
            "GRUNNBELØPSREGULERING" -> Egenskap.GRUNNBELØPSREGULERING
            else -> error("Ukjent oppgaveegenskap")
        }
}
