package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.HendelseDao.Hendelsetype.*
import no.nav.helse.modell.person.toFødselsnummer
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class HendelseDao(private val dataSource: DataSource) {
    internal fun opprett(hendelse: Hendelse) {
        using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run {
                    opprettHendelse(hendelse)
                    hendelse.vedtaksperiodeId()?.let { opprettKobling(it, hendelse.id) }
                }
            }
        }
    }

    internal fun finnFødselsnummer(hendelseId: UUID): String {
        return using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val statement = """SELECT fodselsnummer FROM hendelse WHERE id = ?"""
            requireNotNull(session.run(queryOf(statement, hendelseId).map {
                it.long("fodselsnummer").toFødselsnummer()
            }.asSingle))
        }
    }

    internal fun finnUtbetalingsgodkjenningbehov(hendelseId: UUID): UtbetalingsgodkjenningMessage {
        return UtbetalingsgodkjenningMessage(finnJson(hendelseId, GODKJENNING))
    }

    private fun finnJson(hendelseId: UUID, hendelsetype: Hendelsetype): String {
        return requireNotNull(using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val statement = """SELECT data FROM hendelse WHERE id = ? AND type = ?"""
            session.run(queryOf(statement, hendelseId, hendelsetype.name).map { it.string("data") }.asSingle)
        })
    }

    private fun TransactionalSession.opprettHendelse(hendelse: Hendelse) {
        @Language("PostgreSQL")
        val hendelseStatement = """
            INSERT INTO hendelse(id, fodselsnummer, data, type)
                VALUES(?, ?, CAST(? as json), ?)
            """
        run(
            queryOf(
                hendelseStatement,
                hendelse.id,
                hendelse.fødselsnummer().toLong(),
                hendelse.toJson(),
                tilHendelsetype(hendelse).name
            ).asUpdate
        )
    }

    private fun TransactionalSession.opprettKobling(vedtaksperiodeId: UUID, hendelseId: UUID) {
        @Language("PostgreSQL")
        val koblingStatement = "INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES(?,?)"
        run(
            queryOf(
                koblingStatement,
                vedtaksperiodeId,
                hendelseId
            ).asUpdate
        )
    }

    internal fun harKoblingTil(vedtaksperiodeId: UUID): Boolean {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT 1 FROM vedtaksperiode_hendelse WHERE vedtaksperiode_id=?", vedtaksperiodeId
                ).map { it.boolean(1) }.asSingle
            )
        } ?: false
    }

    internal fun finnSisteUkesAnnullerteOgForkastede(): List<String> {
        @Language("PostgreSQL")
        val statement = """
            SELECT DISTINCT fodselsnummer
            FROM hendelse h
            WHERE h.type IN ('VEDTAKSPERIODE_FORKASTET', 'UTBETALING_ANNULLERT')
            AND TO_DATE(h.data->>'@opprettet','YYYY-MM-DD') > CURRENT_DATE - INTERVAL '7 days'
            AND TO_DATE(h.data->>'@opprettet','YYYY-MM-DD') > (
                SELECT sist_endret
                FROM speil_snapshot
                    JOIN vedtak v ON speil_snapshot.id = v.speil_snapshot_ref
                    JOIN person p ON v.person_ref = p.id
                WHERE p.fodselsnummer = h.fodselsnummer
                ORDER BY v.id DESC
                LIMIT 1)
            """
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf(statement).map { it.long("fodselsnummer").toFødselsnummer() }.asList)
        }
    }

    internal fun finn(id: UUID, hendelsefabrikk: IHendelsefabrikk) = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT type,data FROM hendelse WHERE id = ?", id).map { row ->
            fraHendelsetype(enumValueOf(row.string("type")), row.string("data"), hendelsefabrikk)
        }.asSingle)
    }

    private fun fraHendelsetype(
        hendelsetype: Hendelsetype,
        json: String,
        hendelsefabrikk: IHendelsefabrikk
    ): Hendelse? =
        when (hendelsetype) {
            VEDTAKSPERIODE_ENDRET -> hendelsefabrikk.vedtaksperiodeEndret(json)
            VEDTAKSPERIODE_FORKASTET -> hendelsefabrikk.vedtaksperiodeForkastet(json)
            GODKJENNING -> hendelsefabrikk.godkjenning(json)
            OVERSTYRING -> hendelsefabrikk.overstyring(json)
            TILBAKERULLING -> hendelsefabrikk.tilbakerulling(json)
            SAKSBEHANDLERLØSNING -> hendelsefabrikk.saksbehandlerløsning(json)
            UTBETALING_ANNULLERT -> hendelsefabrikk.utbetalingAnnullert(json)
            UTBETALING_ENDRET -> hendelsefabrikk.utbetalingEndret(json)
            OPPDATER_PERSONSNAPSHOT -> hendelsefabrikk.oppdaterPersonsnapshot(json)
            OPPGAVE_MAKSTID_PÅMINNELSE -> hendelsefabrikk.oppgaveMakstidPåminnelse(json)
            AVBRYT_SAKSBEHANDLING -> hendelsefabrikk.avbrytSaksbehandling(json)
        }

    private fun tilHendelsetype(hendelse: Hendelse) = when (hendelse) {
        is VedtaksperiodeEndret -> VEDTAKSPERIODE_ENDRET
        is VedtaksperiodeForkastet -> VEDTAKSPERIODE_FORKASTET
        is Godkjenningsbehov -> GODKJENNING
        is Overstyring -> OVERSTYRING
        is Tilbakerulling -> TILBAKERULLING
        is Saksbehandlerløsning -> SAKSBEHANDLERLØSNING
        is UtbetalingAnnullert -> UTBETALING_ANNULLERT
        is OppdaterPersonsnapshot -> OPPDATER_PERSONSNAPSHOT
        is UtbetalingEndret -> UTBETALING_ENDRET
        is OppgaveMakstidPåminnelse -> OPPGAVE_MAKSTID_PÅMINNELSE
        is AvbrytSaksbehandling -> AVBRYT_SAKSBEHANDLING
        else -> throw IllegalArgumentException("ukjent hendelsetype: ${hendelse::class.simpleName}")
    }

    private enum class Hendelsetype {
        VEDTAKSPERIODE_ENDRET, VEDTAKSPERIODE_FORKASTET, GODKJENNING, OVERSTYRING, TILBAKERULLING,
        SAKSBEHANDLERLØSNING, UTBETALING_ANNULLERT, OPPDATER_PERSONSNAPSHOT, UTBETALING_ENDRET, OPPGAVE_MAKSTID_PÅMINNELSE,
        AVBRYT_SAKSBEHANDLING
    }
}
