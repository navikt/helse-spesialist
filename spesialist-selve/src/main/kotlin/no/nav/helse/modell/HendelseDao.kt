package no.nav.helse.modell

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.HendelseDao.Hendelsetype.ADRESSEBESKYTTELSE_ENDRET
import no.nav.helse.modell.HendelseDao.Hendelsetype.ENDRET_EGEN_ANSATT_STATUS
import no.nav.helse.modell.HendelseDao.Hendelsetype.GODKJENNING
import no.nav.helse.modell.HendelseDao.Hendelsetype.GODKJENT_TILBAKEDATERT_SYKMELDING
import no.nav.helse.modell.HendelseDao.Hendelsetype.GOSYS_OPPGAVE_ENDRET
import no.nav.helse.modell.HendelseDao.Hendelsetype.NYE_VARSLER
import no.nav.helse.modell.HendelseDao.Hendelsetype.OPPDATER_PERSONSNAPSHOT
import no.nav.helse.modell.HendelseDao.Hendelsetype.OVERSTYRING_IGANGSATT
import no.nav.helse.modell.HendelseDao.Hendelsetype.SAKSBEHANDLERLØSNING
import no.nav.helse.modell.HendelseDao.Hendelsetype.SYKEFRAVÆRSTILFELLER
import no.nav.helse.modell.HendelseDao.Hendelsetype.SØKNAD_SENDT
import no.nav.helse.modell.HendelseDao.Hendelsetype.UTBETALING_ANNULLERT
import no.nav.helse.modell.HendelseDao.Hendelsetype.UTBETALING_ENDRET
import no.nav.helse.modell.HendelseDao.Hendelsetype.VEDTAKSPERIODE_ENDRET
import no.nav.helse.modell.HendelseDao.Hendelsetype.VEDTAKSPERIODE_FORKASTET
import no.nav.helse.modell.HendelseDao.Hendelsetype.VEDTAKSPERIODE_NY_UTBETALING
import no.nav.helse.modell.HendelseDao.Hendelsetype.VEDTAKSPERIODE_OPPRETTET
import no.nav.helse.modell.HendelseDao.Hendelsetype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.modell.HendelseDao.Hendelsetype.VEDTAK_FATTET
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.TilbakedateringGodkjent
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.toFødselsnummer
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfeller
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOpprettet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.intellij.lang.annotations.Language

internal class HendelseDao(private val dataSource: DataSource) {
    internal fun opprett(hendelse: Personmelding) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run {
                    opprettHendelse(hendelse)
                    if (hendelse is Vedtaksperiodemelding)
                        opprettKobling(hendelse.vedtaksperiodeId(), hendelse.id)
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

    internal fun erAutomatisertKorrigertSøknadHåndtert(hendelseId: UUID): Boolean {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                SELECT count(1) AS antall
                FROM automatisering_korrigert_soknad aks
                WHERE hendelse_ref = :hendelseId
                """
            requireNotNull(session.run(queryOf(statement, mapOf("hendelseId" to hendelseId)).map {
                it.int("antall") > 0
            }.asSingle))
        }
    }

    internal fun opprettAutomatiseringKorrigertSøknad(vedtaksperiodeId: UUID, hendelseId: UUID) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO automatisering_korrigert_soknad (vedtaksperiode_id, hendelse_ref)
                VALUES (:vedtaksperiodeId, :hendelseId)
                """
            session.run(queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId, "hendelseId" to hendelseId)).asExecute)
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
                        hendelseId = data["@id"].asText(),
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
        val hendelseId: String,
        val berørtePerioder: List<BerørtPeriode>,
    )

    internal data class BerørtPeriode(
        val vedtaksperiodeId: UUID,
        val periodeFom: LocalDate,
        val orgnummer: String,
    )

    internal fun finnUtbetalingsgodkjenningbehovJson(hendelseId: UUID): String {
        return finnJson(hendelseId, GODKJENNING)
    }

    private fun finnJson(hendelseId: UUID, hendelsetype: Hendelsetype): String {
        return requireNotNull(sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """SELECT data FROM hendelse WHERE id = ? AND type = ?"""
            session.run(queryOf(statement, hendelseId, hendelsetype.name).map { it.string("data") }.asSingle)
        })
    }

    private fun TransactionalSession.opprettHendelse(hendelse: Personmelding) {
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

    internal fun finn(id: UUID, hendelsefabrikk: Hendelsefabrikk) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT type,data FROM hendelse WHERE id = ?", id).map { row ->
            fraHendelsetype(enumValueOf(row.string("type")), row.string("data"), hendelsefabrikk)
        }.asSingle)
    }

    // Denne funksjonen trenger bare å støtte de hendelsetypene som starter en kommandokjede som sender ut behov.
    // Kommandokjeder som ikke har noen suspenderende subcommands vil aldri kunne komme inn her.
    private fun fraHendelsetype(
        hendelsetype: Hendelsetype,
        json: String,
        hendelsefabrikk: Hendelsefabrikk,
    ): Personmelding {
        val jsonNode = objectMapper.readTree(json)
        return when (hendelsetype) {
            ADRESSEBESKYTTELSE_ENDRET -> AdressebeskyttelseEndret(jsonNode["@id"].asUUID(), jsonNode["fødselsnummer"].asText(), json)
            // VEDTAKSPERIODE_FORKASTET trengs pt. pga. KommandohendelseDaoTest.`lagrer og finner hendelser`
            VEDTAKSPERIODE_FORKASTET -> hendelsefabrikk.vedtaksperiodeForkastet(json)
            GODKJENNING -> godkjenning(json)
            OPPDATER_PERSONSNAPSHOT -> hendelsefabrikk.oppdaterPersonsnapshot(json)
            GOSYS_OPPGAVE_ENDRET -> hendelsefabrikk.gosysOppgaveEndret(json)
            else -> throw IllegalArgumentException(
                "Prøver å gjenoppta en kommando(kjede) etter mottak av hendelsetype " +
                        "$hendelsetype, men koden som trengs mangler!")
        }
    }

    private fun godkjenning(json: String): Godkjenningsbehov {
        val jsonNode = objectMapper.readTree(json)
        val periodetype = Periodetype.valueOf(jsonNode.path("Godkjenning").path("periodetype").asText())
        val førstegangsbehandling = jsonNode.path("Godkjenning").path("førstegangsbehandling")
            .asBoolean(periodetype == Periodetype.FØRSTEGANGSBEHANDLING) // bruker default-value enn så lenge for å kunne parse eldre godkjenningsbehov
        return Godkjenningsbehov(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            periodeFom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeFom").asText()),
            periodeTom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeTom").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
            skjæringstidspunkt = LocalDate.parse(jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText()),
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            utbetalingtype = Utbetalingtype.valueOf(jsonNode.path("Godkjenning").path("utbetalingtype").asText()),
            inntektskilde = Inntektskilde.valueOf(jsonNode.path("Godkjenning").path("inntektskilde").asText()),
            orgnummereMedRelevanteArbeidsforhold = jsonNode.path("Godkjenning")
                .path("orgnummereMedRelevanteArbeidsforhold")
                .takeUnless(JsonNode::isMissingOrNull)?.map { it.asText() } ?: emptyList(),
            kanAvvises = jsonNode.path("Godkjenning").path("kanAvvises").asBoolean(),
            json = json,
        )
    }

    private fun tilHendelsetype(hendelse: Personmelding) = when (hendelse) {
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
        is VedtaksperiodeOpprettet -> VEDTAKSPERIODE_OPPRETTET
        is SøknadSendt -> SØKNAD_SENDT
        is VedtaksperiodeNyUtbetaling -> VEDTAKSPERIODE_NY_UTBETALING
        is Sykefraværstilfeller -> SYKEFRAVÆRSTILFELLER
        is TilbakedateringGodkjent -> GODKJENT_TILBAKEDATERT_SYKMELDING
        else -> throw IllegalArgumentException("ukjent hendelsetype: ${hendelse::class.simpleName}")
    }

    private enum class Hendelsetype {
        ADRESSEBESKYTTELSE_ENDRET, VEDTAKSPERIODE_ENDRET, VEDTAKSPERIODE_FORKASTET, GODKJENNING,
        SAKSBEHANDLERLØSNING, UTBETALING_ANNULLERT, OPPDATER_PERSONSNAPSHOT, UTBETALING_ENDRET,
        VEDTAKSPERIODE_REBEREGNET,
        OVERSTYRING_IGANGSATT, GOSYS_OPPGAVE_ENDRET, ENDRET_EGEN_ANSATT_STATUS, VEDTAK_FATTET,
        NYE_VARSLER, VEDTAKSPERIODE_OPPRETTET, SØKNAD_SENDT, VEDTAKSPERIODE_NY_UTBETALING, SYKEFRAVÆRSTILFELLER,
        GODKJENT_TILBAKEDATERT_SYKMELDING
    }
}
