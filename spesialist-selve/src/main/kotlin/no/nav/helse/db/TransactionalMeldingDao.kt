package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.ADRESSEBESKYTTELSE_ENDRET
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.AVSLUTTET_MED_VEDTAK
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.BEHANDLING_OPPRETTET
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.ENDRET_EGEN_ANSATT_STATUS
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.GODKJENNING
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.GODKJENT_TILBAKEDATERT_SYKMELDING
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.GOSYS_OPPGAVE_ENDRET
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.KLARGJØR_TILGANGSRELATERTE_DATA
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.NYE_VARSLER
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.OPPDATER_PERSONSNAPSHOT
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.OVERSTYRING_IGANGSATT
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.SAKSBEHANDLERLØSNING
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.SØKNAD_SENDT
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.UTBETALING_ENDRET
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.VEDTAKSPERIODE_FORKASTET
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.VEDTAKSPERIODE_NY_UTBETALING
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.db.TransactionalMeldingDao.Meldingtype.VEDTAK_FATTET
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetUtenVedtakMessage
import no.nav.helse.modell.MeldingDao.BerørtPeriode
import no.nav.helse.modell.MeldingDao.OverstyringIgangsattKorrigertSøknad
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteData
import no.nav.helse.modell.person.OppdaterPersondata
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.toFødselsnummer
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import org.intellij.lang.annotations.Language
import java.util.UUID

internal class TransactionalMeldingDao(private val transactionalSession: TransactionalSession) : MeldingRepository {
    override fun finnFødselsnummer(meldingId: UUID): String {
        @Language("PostgreSQL")
        val statement = """SELECT fodselsnummer FROM hendelse WHERE id = ?"""
        return requireNotNull(
            transactionalSession.run(
                queryOf(statement, meldingId).map {
                    it.long("fodselsnummer").toFødselsnummer()
                }.asSingle,
            ),
        )
    }

    override fun finn(id: UUID): Personmelding? {
        @Language("PostgreSQL")
        val statement = """SELECT type,data FROM hendelse WHERE id = ?"""
        return transactionalSession.run(
            queryOf(statement, id).map { row ->
                fraMeldingtype(enumValueOf(row.string("type")), row.string("data"))
            }.asSingle,
        )
    }

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov {
        val melding =
            finn(meldingId)
                ?: throw IllegalArgumentException("Forventer å finne godkjenningsbehov for meldingId=$meldingId")
        check(melding is Godkjenningsbehov) { "Forventer at melding funnet med meldingId=$meldingId er et godkjenningsbehov" }
        return melding
    }

    override fun lagre(melding: Personmelding) {
        transactionalSession.run {
            lagre(melding)
            if (melding is Vedtaksperiodemelding) {
                opprettKobling(melding.vedtaksperiodeId(), melding.id)
            }
        }
    }

    override fun sisteOverstyringIgangsattOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): OverstyringIgangsattKorrigertSøknad? {
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
        return transactionalSession.run(
            queryOf(
                statement,
                mapOf("fodselsnummer" to fødselsnummer.toLong(), "vedtaksperiodeId" to vedtaksperiodeId.toString()),
            ).map { row ->
                row.stringOrNull("data")?.let {
                    val data = objectMapper.readTree(it)
                    if (data["årsak"].asText() != "KORRIGERT_SØKNAD") return@let null

                    OverstyringIgangsattKorrigertSøknad(
                        periodeForEndringFom = data["periodeForEndringFom"].asLocalDate(),
                        meldingId = data["@id"].asText(),
                        berørtePerioder =
                            data["berørtePerioder"].map { berørtPeriode ->
                                BerørtPeriode(
                                    vedtaksperiodeId = UUID.fromString(berørtPeriode["vedtaksperiodeId"].asText()),
                                    periodeFom = berørtPeriode["periodeFom"].asLocalDate(),
                                    orgnummer = berørtPeriode["orgnummer"].asText(),
                                )
                            },
                    )
                }
            }.asSingle,
        )
    }

    override fun opprettAutomatiseringKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement =
            """
            INSERT INTO automatisering_korrigert_soknad (vedtaksperiode_id, hendelse_ref)
            VALUES (:vedtaksperiodeId, :hendelseId)
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                statement,
                mapOf("vedtaksperiodeId" to vedtaksperiodeId, "hendelseId" to meldingId),
            ).asExecute,
        )
    }

    override fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int {
        @Language("PostgreSQL")
        val statement = """
                SELECT count(1) AS antall
                FROM automatisering_korrigert_soknad aks
                WHERE vedtaksperiode_id = :vedtaksperiodeId
                """
        return requireNotNull(
            transactionalSession.run(
                queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map {
                    it.int("antall")
                }.asSingle,
            ),
        )
    }

    override fun erAutomatisertKorrigertSøknadHåndtert(meldingId: UUID): Boolean {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT count(1) AS antall
            FROM automatisering_korrigert_soknad aks
            WHERE hendelse_ref = :hendelseId
            """.trimIndent()
        return requireNotNull(
            transactionalSession.run(
                queryOf(statement, mapOf("hendelseId" to meldingId)).map {
                    it.int("antall") > 0
                }.asSingle,
            ),
        )
    }

    private fun TransactionalSession.opprettKobling(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        @Language("PostgreSQL")
        val koblingStatement = "INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES(?,?)"
        run(
            queryOf(
                koblingStatement,
                vedtaksperiodeId,
                meldingId,
            ).asUpdate,
        )
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
                tilMeldingtype(melding).name,
            ).asUpdate,
        )
    }

    private fun fraMeldingtype(
        meldingtype: Meldingtype,
        json: String,
    ): Personmelding {
        val jsonNode = objectMapper.readTree(json)
        return when (meldingtype) {
            ADRESSEBESKYTTELSE_ENDRET -> AdressebeskyttelseEndret(jsonNode)
            GODKJENNING -> Godkjenningsbehov(jsonNode)
            OPPDATER_PERSONSNAPSHOT -> OppdaterPersondata(jsonNode)
            GOSYS_OPPGAVE_ENDRET -> GosysOppgaveEndret(jsonNode)
            VEDTAKSPERIODE_FORKASTET -> VedtaksperiodeForkastet(jsonNode)
            GODKJENT_TILBAKEDATERT_SYKMELDING -> TilbakedateringBehandlet(jsonNode)
            OVERSTYRING_IGANGSATT -> OverstyringIgangsatt(jsonNode)
            SAKSBEHANDLERLØSNING -> Saksbehandlerløsning(jsonNode)
            UTBETALING_ENDRET -> UtbetalingEndret(jsonNode)
            VEDTAKSPERIODE_REBEREGNET -> VedtaksperiodeReberegnet(jsonNode)
            ENDRET_EGEN_ANSATT_STATUS -> EndretEgenAnsattStatus(jsonNode)
            VEDTAK_FATTET -> VedtakFattet(jsonNode)
            NYE_VARSLER -> NyeVarsler(jsonNode)
            SØKNAD_SENDT -> SøknadSendt(jsonNode)
            VEDTAKSPERIODE_NY_UTBETALING -> VedtaksperiodeNyUtbetaling(jsonNode)
            BEHANDLING_OPPRETTET -> BehandlingOpprettet(jsonNode)
            AVSLUTTET_UTEN_VEDTAK -> AvsluttetUtenVedtakMessage(jsonNode)
            KLARGJØR_TILGANGSRELATERTE_DATA -> KlargjørTilgangsrelaterteData(jsonNode)
            else -> throw IllegalArgumentException("ukjent meldingtype: $meldingtype")
        }
    }

    private fun tilMeldingtype(melding: Personmelding) =
        when (melding) {
            is AdressebeskyttelseEndret -> ADRESSEBESKYTTELSE_ENDRET
            is VedtaksperiodeForkastet -> VEDTAKSPERIODE_FORKASTET
            is Godkjenningsbehov -> GODKJENNING
            is OverstyringIgangsatt -> OVERSTYRING_IGANGSATT
            is Saksbehandlerløsning -> SAKSBEHANDLERLØSNING
            is OppdaterPersondata -> OPPDATER_PERSONSNAPSHOT
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
            is AvsluttetUtenVedtakMessage -> AVSLUTTET_UTEN_VEDTAK
            is AvsluttetMedVedtakMessage -> AVSLUTTET_MED_VEDTAK
            is KlargjørTilgangsrelaterteData -> KLARGJØR_TILGANGSRELATERTE_DATA
            else -> throw IllegalArgumentException("ukjent meldingtype: ${melding::class.simpleName}")
        }

    private enum class Meldingtype {
        ADRESSEBESKYTTELSE_ENDRET,
        VEDTAKSPERIODE_FORKASTET,
        GODKJENNING,
        SAKSBEHANDLERLØSNING,
        OPPDATER_PERSONSNAPSHOT,
        UTBETALING_ENDRET,
        VEDTAKSPERIODE_REBEREGNET,
        BEHANDLING_OPPRETTET,
        OVERSTYRING_IGANGSATT,
        GOSYS_OPPGAVE_ENDRET,
        ENDRET_EGEN_ANSATT_STATUS,
        VEDTAK_FATTET,
        NYE_VARSLER,
        SØKNAD_SENDT,
        VEDTAKSPERIODE_NY_UTBETALING,
        GODKJENT_TILBAKEDATERT_SYKMELDING,
        AVSLUTTET_UTEN_VEDTAK,
        AVSLUTTET_MED_VEDTAK,
        KLARGJØR_TILGANGSRELATERTE_DATA,
    }
}
