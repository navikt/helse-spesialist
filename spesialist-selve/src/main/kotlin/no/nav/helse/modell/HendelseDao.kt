package no.nav.helse.modell

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.HendelseDao.Hendelsetype.*
import no.nav.helse.modell.person.toFødselsnummer
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class HendelseDao(private val dataSource: DataSource) {
    internal fun opprett(hendelse: Hendelse) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run {
                    opprettHendelse(hendelse)
                    hendelse.vedtaksperiodeId()?.let { opprettKobling(it, hendelse.id) }
                }
            }
        }
    }

    internal fun finnFødselsnummer(hendelseId: UUID): String {
        return sessionOf(dataSource).use { session ->
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
        return requireNotNull(sessionOf(dataSource).use { session ->
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
            ON CONFLICT DO NOTHING
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
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT 1 FROM vedtaksperiode_hendelse WHERE vedtaksperiode_id=?", vedtaksperiodeId
                ).map { it.boolean(1) }.asSingle
            )
        } ?: false
    }

    internal fun finn(id: UUID, hendelsefabrikk: IHendelsefabrikk) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT type,data FROM hendelse WHERE id = ?", id).map { row ->
            fraHendelsetype(enumValueOf(row.string("type")), row.string("data"), hendelsefabrikk)
        }.asSingle)
    }

    private fun fraHendelsetype(
        hendelsetype: Hendelsetype,
        json: String,
        hendelsefabrikk: IHendelsefabrikk
    ): Hendelse =
        when (hendelsetype) {
            ADRESSEBESKYTTELSE_ENDRET -> hendelsefabrikk.adressebeskyttelseEndret(json)
            VEDTAKSPERIODE_ENDRET -> hendelsefabrikk.vedtaksperiodeEndret(json)
            VEDTAKSPERIODE_FORKASTET -> hendelsefabrikk.vedtaksperiodeForkastet(json)
            GODKJENNING -> hendelsefabrikk.godkjenning(json)
            OVERSTYRING -> hendelsefabrikk.overstyringTidslinje(json)
            OVERSTYRING_INNTEKT -> hendelsefabrikk.overstyringInntekt(json)
            OVERSTYRING_ARBEIDSFORHOLD -> hendelsefabrikk.overstyringArbeidsforhold(json)
            SAKSBEHANDLERLØSNING -> hendelsefabrikk.saksbehandlerløsning(json)
            UTBETALING_ANNULLERT -> hendelsefabrikk.utbetalingAnnullert(json)
            UTBETALING_ENDRET -> hendelsefabrikk.utbetalingEndret(json)
            OPPDATER_PERSONSNAPSHOT -> hendelsefabrikk.oppdaterPersonsnapshot(json)
            VEDTAKSPERIODE_REBEREGNET -> hendelsefabrikk.vedtaksperiodeReberegnet(json)
            REVURDERING_AVVIST -> hendelsefabrikk.revurderingAvvist(json)
            GOSYS_OPPGAVE_ENDRET -> hendelsefabrikk.gosysOppgaveEndret(json)
            INNHENT_SKJERMETINFO -> hendelsefabrikk.innhentSkjermetinfo(json)
        }

    private fun tilHendelsetype(hendelse: Hendelse) = when (hendelse) {
        is AdressebeskyttelseEndret -> ADRESSEBESKYTTELSE_ENDRET
        is VedtaksperiodeEndret -> VEDTAKSPERIODE_ENDRET
        is VedtaksperiodeForkastet -> VEDTAKSPERIODE_FORKASTET
        is Godkjenningsbehov -> GODKJENNING
        is OverstyringTidslinje -> OVERSTYRING
        is OverstyringInntekt -> OVERSTYRING_INNTEKT
        is OverstyringArbeidsforhold -> OVERSTYRING_ARBEIDSFORHOLD
        is Saksbehandlerløsning -> SAKSBEHANDLERLØSNING
        is UtbetalingAnnullert -> UTBETALING_ANNULLERT
        is OppdaterPersonsnapshot -> OPPDATER_PERSONSNAPSHOT
        is UtbetalingEndret -> UTBETALING_ENDRET
        is VedtaksperiodeReberegnet -> VEDTAKSPERIODE_REBEREGNET
        is RevurderingAvvist -> REVURDERING_AVVIST
        is GosysOppgaveEndret -> GOSYS_OPPGAVE_ENDRET
        is InnhentSkjermetinfo -> INNHENT_SKJERMETINFO
        else -> throw IllegalArgumentException("ukjent hendelsetype: ${hendelse::class.simpleName}")
    }

    private enum class Hendelsetype {
        ADRESSEBESKYTTELSE_ENDRET, VEDTAKSPERIODE_ENDRET, VEDTAKSPERIODE_FORKASTET, GODKJENNING, OVERSTYRING,
        SAKSBEHANDLERLØSNING, UTBETALING_ANNULLERT, OPPDATER_PERSONSNAPSHOT, UTBETALING_ENDRET,
        VEDTAKSPERIODE_REBEREGNET, OVERSTYRING_INNTEKT, OVERSTYRING_ARBEIDSFORHOLD, REVURDERING_AVVIST,
        GOSYS_OPPGAVE_ENDRET, INNHENT_SKJERMETINFO
    }
}
