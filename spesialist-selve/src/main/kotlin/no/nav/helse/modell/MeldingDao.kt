package no.nav.helse.modell

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.MeldingDao.Meldingtype.ADRESSEBESKYTTELSE_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.BEHANDLING_OPPRETTET
import no.nav.helse.modell.MeldingDao.Meldingtype.ENDRET_EGEN_ANSATT_STATUS
import no.nav.helse.modell.MeldingDao.Meldingtype.GODKJENNING
import no.nav.helse.modell.MeldingDao.Meldingtype.GODKJENT_TILBAKEDATERT_SYKMELDING
import no.nav.helse.modell.MeldingDao.Meldingtype.GOSYS_OPPGAVE_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.NYE_VARSLER
import no.nav.helse.modell.MeldingDao.Meldingtype.OPPDATER_PERSONSNAPSHOT
import no.nav.helse.modell.MeldingDao.Meldingtype.OVERSTYRING_IGANGSATT
import no.nav.helse.modell.MeldingDao.Meldingtype.SAKSBEHANDLERLØSNING
import no.nav.helse.modell.MeldingDao.Meldingtype.SØKNAD_SENDT
import no.nav.helse.modell.MeldingDao.Meldingtype.UTBETALING_ANNULLERT
import no.nav.helse.modell.MeldingDao.Meldingtype.UTBETALING_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_FORKASTET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_NY_UTBETALING
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAK_FATTET
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.toFødselsnummer
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import org.intellij.lang.annotations.Language

internal class MeldingDao(private val dataSource: DataSource) {
    internal fun lagre(melding: Personmelding) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run {
                    lagre(melding)
                    if (melding is Vedtaksperiodemelding)
                        opprettKobling(melding.vedtaksperiodeId(), melding.id)
                }
            }
        }
    }

    internal fun finnFødselsnummer(meldingId: UUID): String {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """SELECT fodselsnummer FROM hendelse WHERE id = ?"""
            requireNotNull(session.run(queryOf(statement, meldingId).map {
                it.long("fodselsnummer").toFødselsnummer()
            }.asSingle))
        }
    }

    internal fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                SELECT count(1) AS antall
                FROM automatisering_korrigert_soknad aks
                WHERE vedtaksperiode_id = :vedtaksperiodeId
                """
            requireNotNull(session.run(queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map {
                it.int("antall")
            }.asSingle))
        }
    }

    internal fun erAutomatisertKorrigertSøknadHåndtert(meldingId: UUID): Boolean {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                SELECT count(1) AS antall
                FROM automatisering_korrigert_soknad aks
                WHERE hendelse_ref = :hendelseId
                """
            requireNotNull(session.run(queryOf(statement, mapOf("hendelseId" to meldingId)).map {
                it.int("antall") > 0
            }.asSingle))
        }
    }

    internal fun opprettAutomatiseringKorrigertSøknad(vedtaksperiodeId: UUID, meldingId: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO automatisering_korrigert_soknad (vedtaksperiode_id, hendelse_ref)
                VALUES (:vedtaksperiodeId, :hendelseId)
                """
            session.run(queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId, "hendelseId" to meldingId)).asExecute)
        }
    }

    internal fun sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer: String, vedtaksperiodeId: UUID): OverstyringIgangsattKorrigertSøknad? {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                SELECT h.data
                FROM hendelse h, json_array_elements(h.data -> 'berørtePerioder') AS bp
                WHERE fodselsnummer = :fodselsnummer
                AND h.type='OVERSTYRING_IGANGSATT'
                AND bp ->> 'vedtaksperiodeId' = :vedtaksperiodeId
                ORDER BY h.data ->> '@opprettet' DESC
                LIMIT 1
                """
            session.run(queryOf(statement, mapOf("fodselsnummer" to fødselsnummer.toLong(), "vedtaksperiodeId" to vedtaksperiodeId.toString())).map { row ->
                row.stringOrNull("data")?.let {
                    val data = objectMapper.readTree(it)
                    if (data["årsak"].asText() != "KORRIGERT_SØKNAD") return@let null

                    OverstyringIgangsattKorrigertSøknad(
                        periodeForEndringFom = data["periodeForEndringFom"].asLocalDate(),
                        meldingId = data["@id"].asText(),
                        berørtePerioder = data["berørtePerioder"].map { berørtPeriode ->
                            BerørtPeriode(
                                vedtaksperiodeId = UUID.fromString(berørtPeriode["vedtaksperiodeId"].asText()),
                                periodeFom = berørtPeriode["periodeFom"].asLocalDate(),
                                orgnummer = berørtPeriode["orgnummer"].asText()
                            )
                        }
                    )
                }
            }.asSingle)
        }
    }

    internal data class OverstyringIgangsattKorrigertSøknad(
        val periodeForEndringFom: LocalDate,
        val meldingId: String,
        val berørtePerioder: List<BerørtPeriode>,
    )

    internal data class BerørtPeriode(
        val vedtaksperiodeId: UUID,
        val periodeFom: LocalDate,
        val orgnummer: String,
    )

    internal fun finnUtbetalingsgodkjenningbehovJson(meldingId: UUID): String {
        return finnJson(meldingId, GODKJENNING)
    }

    private fun finnJson(meldingId: UUID, meldingtype: Meldingtype): String {
        return requireNotNull(sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """SELECT data FROM hendelse WHERE id = ? AND type = ?"""
            session.run(queryOf(statement, meldingId, meldingtype.name).map { it.string("data") }.asSingle)
        })
    }

    private fun TransactionalSession.lagre(melding: Personmelding) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO hendelse(id, fodselsnummer, data, type)
                VALUES(?, ?, CAST(? as json), ?)
            ON CONFLICT DO NOTHING
            """
        run(
            queryOf(
                query,
                melding.id,
                melding.fødselsnummer().toLong(),
                melding.toJson(),
                tilMeldingtype(melding).name
            ).asUpdate
        )
    }

    private fun TransactionalSession.opprettKobling(vedtaksperiodeId: UUID, meldingId: UUID) {
        @Language("PostgreSQL")
        val koblingStatement = "INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES(?,?)"
        run(
            queryOf(
                koblingStatement,
                vedtaksperiodeId,
                meldingId
            ).asUpdate
        )
    }

    internal fun finn(id: UUID) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT type,data FROM hendelse WHERE id = ?", id).map { row ->
            fraMeldingtype(enumValueOf(row.string("type")), row.string("data"))
        }.asSingle)
    }

    // Denne funksjonen trenger bare å støtte de meldingtypene som starter en kommandokjede som sender ut behov.
    // Kommandokjeder som ikke har noen suspenderende subcommands vil aldri kunne komme inn her.
    private fun fraMeldingtype(
        meldingtype: Meldingtype,
        json: String,
    ): Personmelding {
        val jsonNode = objectMapper.readTree(json)
        return when (meldingtype) {
            ADRESSEBESKYTTELSE_ENDRET -> AdressebeskyttelseEndret(jsonNode)
            GODKJENNING -> Godkjenningsbehov(jsonNode)
            OPPDATER_PERSONSNAPSHOT -> OppdaterPersonsnapshot(jsonNode)
            GOSYS_OPPGAVE_ENDRET -> GosysOppgaveEndret(jsonNode)
            VEDTAKSPERIODE_ENDRET -> VedtaksperiodeEndret(jsonNode)
            VEDTAKSPERIODE_FORKASTET -> VedtaksperiodeForkastet(jsonNode)
            UTBETALING_ANNULLERT -> UtbetalingAnnullert(jsonNode)
            else -> throw IllegalArgumentException(
                "Prøver å gjenoppta en kommando(kjede) etter mottak av hendelsetype " +
                        "$meldingtype, men koden som trengs mangler!")
        }
    }

    private fun tilMeldingtype(melding: Personmelding) = when (melding) {
        is AdressebeskyttelseEndret -> ADRESSEBESKYTTELSE_ENDRET
        is VedtaksperiodeEndret -> VEDTAKSPERIODE_ENDRET
        is VedtaksperiodeForkastet -> VEDTAKSPERIODE_FORKASTET
        is Godkjenningsbehov -> GODKJENNING
        is OverstyringIgangsatt -> OVERSTYRING_IGANGSATT
        is Saksbehandlerløsning -> SAKSBEHANDLERLØSNING
        is UtbetalingAnnullert -> UTBETALING_ANNULLERT
        is OppdaterPersonsnapshot -> OPPDATER_PERSONSNAPSHOT
        is UtbetalingEndret -> UTBETALING_ENDRET
        is VedtaksperiodeReberegnet -> VEDTAKSPERIODE_REBEREGNET
        is GosysOppgaveEndret -> GOSYS_OPPGAVE_ENDRET
        is EndretEgenAnsattStatus -> ENDRET_EGEN_ANSATT_STATUS
        is VedtakFattet -> VEDTAK_FATTET
        is NyeVarsler -> NYE_VARSLER
        is SøknadSendt -> SØKNAD_SENDT
        is VedtaksperiodeNyUtbetaling -> VEDTAKSPERIODE_NY_UTBETALING
        is TilbakedateringBehandlet -> GODKJENT_TILBAKEDATERT_SYKMELDING
        is BehandlingOpprettet -> BEHANDLING_OPPRETTET
        else -> throw IllegalArgumentException("ukjent meldingtype: ${melding::class.simpleName}")
    }

    private enum class Meldingtype {
        ADRESSEBESKYTTELSE_ENDRET, VEDTAKSPERIODE_ENDRET, VEDTAKSPERIODE_FORKASTET, GODKJENNING,
        SAKSBEHANDLERLØSNING, UTBETALING_ANNULLERT, OPPDATER_PERSONSNAPSHOT, UTBETALING_ENDRET,
        VEDTAKSPERIODE_REBEREGNET, BEHANDLING_OPPRETTET,
        OVERSTYRING_IGANGSATT, GOSYS_OPPGAVE_ENDRET, ENDRET_EGEN_ANSATT_STATUS, VEDTAK_FATTET,
        NYE_VARSLER, SØKNAD_SENDT, VEDTAKSPERIODE_NY_UTBETALING,
        GODKJENT_TILBAKEDATERT_SYKMELDING
    }
}
