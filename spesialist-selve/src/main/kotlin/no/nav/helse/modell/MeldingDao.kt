package no.nav.helse.modell

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.MeldingRepository
import no.nav.helse.db.QueryRunner
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetUtenVedtakMessage
import no.nav.helse.mediator.meldinger.hendelser.AvvikVurdertMessage
import no.nav.helse.modell.MeldingDao.Meldingtype.ADRESSEBESKYTTELSE_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.AVSLUTTET_MED_VEDTAK
import no.nav.helse.modell.MeldingDao.Meldingtype.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.modell.MeldingDao.Meldingtype.AVVIK_VURDERT
import no.nav.helse.modell.MeldingDao.Meldingtype.BEHANDLING_OPPRETTET
import no.nav.helse.modell.MeldingDao.Meldingtype.ENDRET_EGEN_ANSATT_STATUS
import no.nav.helse.modell.MeldingDao.Meldingtype.GODKJENNING
import no.nav.helse.modell.MeldingDao.Meldingtype.GODKJENT_TILBAKEDATERT_SYKMELDING
import no.nav.helse.modell.MeldingDao.Meldingtype.GOSYS_OPPGAVE_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.KLARGJØR_TILGANGSRELATERTE_DATA
import no.nav.helse.modell.MeldingDao.Meldingtype.NYE_VARSLER
import no.nav.helse.modell.MeldingDao.Meldingtype.OPPDATER_PERSONSNAPSHOT
import no.nav.helse.modell.MeldingDao.Meldingtype.OVERSTYRING_IGANGSATT
import no.nav.helse.modell.MeldingDao.Meldingtype.SAKSBEHANDLERLØSNING
import no.nav.helse.modell.MeldingDao.Meldingtype.SØKNAD_SENDT
import no.nav.helse.modell.MeldingDao.Meldingtype.UTBETALING_ENDRET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_FORKASTET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_NY_UTBETALING
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.modell.MeldingDao.Meldingtype.VEDTAK_FATTET
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteData
import no.nav.helse.modell.person.OppdaterPersondata
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
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
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class MeldingDao(queryRunner: QueryRunner) : MeldingRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(melding: Personmelding) {
        asSQL(
            """
            INSERT INTO hendelse(id, data, type)
                VALUES(:id, CAST(:data as json), :type)
            ON CONFLICT DO NOTHING
            """,
            "id" to melding.id,
            "data" to melding.toJson(),
            "type" to tilMeldingtype(melding).name,
        ).update()
        if (melding is Vedtaksperiodemelding) {
            opprettKobling(melding.vedtaksperiodeId(), melding.id)
        }
    }

    override fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int =
        asSQL(
            """
            SELECT count(1) AS antall
            FROM automatisering_korrigert_soknad aks
            WHERE vedtaksperiode_id = :vedtaksperiodeId
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single { it.int("antall") }

    override fun erKorrigertSøknadAutomatiskBehandlet(meldingId: UUID): Boolean =
        asSQL(
            """
            SELECT count(1) AS antall
            FROM automatisering_korrigert_soknad aks
            WHERE hendelse_ref = :hendelseId
            """,
            "hendelseId" to meldingId,
        ).single { it.int("antall") > 0 }

    override fun opprettAutomatiseringKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        asSQL(
            """
            INSERT INTO automatisering_korrigert_soknad (vedtaksperiode_id, hendelse_ref)
            VALUES (:vedtaksperiodeId, :hendelseId)
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseId" to meldingId,
        ).update()
    }

    override fun sisteOverstyringIgangsattOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): OverstyringIgangsattKorrigertSøknad? =
        asSQL(
            """
            SELECT h.data
            FROM hendelse h, json_array_elements(h.data -> 'berørtePerioder') AS bp
            WHERE data->>'fødselsnummer' = :fodselsnummer
            AND h.type='OVERSTYRING_IGANGSATT'
            AND bp ->> 'vedtaksperiodeId' = :vedtaksperiodeId
            ORDER BY h.data ->> '@opprettet' DESC
            LIMIT 1
            """,
            "fodselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId.toString(),
        ).singleOrNull { row ->
            row.stringOrNull("data")?.let {
                val data = objectMapper.readTree(it)
                if (data["årsak"].asText() != "KORRIGERT_SØKNAD") return@let null

                OverstyringIgangsattKorrigertSøknad(
                    periodeForEndringFom = data["periodeForEndringFom"].asText().let(LocalDate::parse),
                    meldingId = data["@id"].asText(),
                    berørtePerioder =
                        data["berørtePerioder"].map { berørtPeriode ->
                            BerørtPeriode(
                                vedtaksperiodeId = UUID.fromString(berørtPeriode["vedtaksperiodeId"].asText()),
                                periodeFom = berørtPeriode["periodeFom"].asText().let(LocalDate::parse),
                                orgnummer = berørtPeriode["orgnummer"].asText(),
                            )
                        },
                )
            }
        }

    data class OverstyringIgangsattKorrigertSøknad(
        val periodeForEndringFom: LocalDate,
        val meldingId: String,
        val berørtePerioder: List<BerørtPeriode>,
    )

    data class BerørtPeriode(
        val vedtaksperiodeId: UUID,
        val periodeFom: LocalDate,
        val orgnummer: String,
    )

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov {
        val melding =
            finn(meldingId)
                ?: throw IllegalArgumentException("Forventer å finne godkjenningsbehov for meldingId=$meldingId")
        check(melding is Godkjenningsbehov) { "Forventer at melding funnet med meldingId=$meldingId er et godkjenningsbehov" }
        return melding
    }

    override fun finn(id: UUID): Personmelding? =
        asSQL(
            "SELECT type, data FROM hendelse WHERE id = :id",
            "id" to id,
        ).singleOrNull { fraMeldingtype(enumValueOf(it.string("type")), it.string("data")) }

    private fun opprettKobling(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        asSQL(
            """ INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES(:vedtaksperiodeId, :meldingId)""",
            "vedtaksperiodeId" to vedtaksperiodeId,
            "meldingId" to meldingId,
        ).update()
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
            is StansAutomatiskBehandlingMelding -> Meldingtype.STANS_AUTOMATISK_BEHANDLING
            is AvvikVurdertMessage -> AVVIK_VURDERT
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
        STANS_AUTOMATISK_BEHANDLING,
        AVVIK_VURDERT,
    }
}
