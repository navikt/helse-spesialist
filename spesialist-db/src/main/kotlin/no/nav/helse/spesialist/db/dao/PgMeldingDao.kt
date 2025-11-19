package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.MeldingDao.BehandlingOpprettetKorrigertSøknad
import no.nav.helse.db.MeldingDao.Meldingtype
import no.nav.helse.db.MeldingDao.Meldingtype.ADRESSEBESKYTTELSE_ENDRET
import no.nav.helse.db.MeldingDao.Meldingtype.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.db.MeldingDao.Meldingtype.BEHANDLING_OPPRETTET
import no.nav.helse.db.MeldingDao.Meldingtype.ENDRET_EGEN_ANSATT_STATUS
import no.nav.helse.db.MeldingDao.Meldingtype.GODKJENNING
import no.nav.helse.db.MeldingDao.Meldingtype.GODKJENT_TILBAKEDATERT_SYKMELDING
import no.nav.helse.db.MeldingDao.Meldingtype.GOSYS_OPPGAVE_ENDRET
import no.nav.helse.db.MeldingDao.Meldingtype.KLARGJØR_TILGANGSRELATERTE_DATA
import no.nav.helse.db.MeldingDao.Meldingtype.NYE_VARSLER
import no.nav.helse.db.MeldingDao.Meldingtype.OPPDATER_PERSONSNAPSHOT
import no.nav.helse.db.MeldingDao.Meldingtype.SØKNAD_SENDT
import no.nav.helse.db.MeldingDao.Meldingtype.UTBETALING_ENDRET
import no.nav.helse.db.MeldingDao.Meldingtype.VEDTAKSPERIODE_FORKASTET
import no.nav.helse.db.MeldingDao.Meldingtype.VEDTAKSPERIODE_NY_UTBETALING
import no.nav.helse.db.MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetUtenVedtakMessage
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
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
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID
import javax.sql.DataSource

class PgMeldingDao private constructor(
    queryRunner: QueryRunner,
) : MeldingDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(melding: Personmelding) {
        if (melding is Vedtaksperiodemelding) {
            lagre(
                id = melding.id,
                json = melding.toJson(),
                meldingtype = tilMeldingtype(melding),
                vedtaksperiodeId = melding.vedtaksperiodeId(),
            )
        } else {
            lagre(
                id = melding.id,
                json = melding.toJson(),
                meldingtype = tilMeldingtype(melding),
            )
        }
    }

    override fun lagre(
        id: UUID,
        json: String,
        meldingtype: Meldingtype,
        vedtaksperiodeId: UUID?,
    ) {
        asSQL(
            """
                INSERT INTO hendelse(id, data, type)
                    VALUES(:id, CAST(:data as json), :type)
                ON CONFLICT DO NOTHING
                """,
            "id" to id,
            "data" to json,
            "type" to meldingtype.name,
        ).update()
        if (vedtaksperiodeId != null) {
            opprettKobling(vedtaksperiodeId, id)
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

    override fun sisteBehandlingOpprettetOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): BehandlingOpprettetKorrigertSøknad? =
        asSQL(
            """
            select h.data
            from hendelse h
            where type = 'BEHANDLING_OPPRETTET'
              and data->>'vedtaksperiodeId' = :vedtaksperiodeId
              and data->>'fødselsnummer' = :fodselsnummer
              and data->>'type' = 'Revurdering'
              and data->'kilde'->>'avsender' = 'SYKMELDT'
              and data->'@forårsaket_av'->>'event_name' = 'sendt_søknad_nav'
            order by h.data ->> '@opprettet' desc
            limit 1
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId.toString(),
        ).singleOrNull { row ->
            row.stringOrNull("data")?.let {
                val data = objectMapper.readTree(it)

                BehandlingOpprettetKorrigertSøknad(
                    meldingId = UUID.fromString(data["@id"].asText()),
                    vedtaksperiodeId = UUID.fromString(data["vedtaksperiodeId"].asText()),
                )
            }
        }

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov {
        val melding =
            finn(meldingId)
                ?: throw IllegalArgumentException("Forventer å finne godkjenningsbehov for meldingId=$meldingId")
        check(melding is Godkjenningsbehov) { "Forventer at melding funnet med meldingId=$meldingId er et godkjenningsbehov" }
        return melding
    }

    override fun finnSisteGodkjenningsbehov(spleisBehandlingId: UUID): Godkjenningsbehov? =
        asSQL(
            """
                SELECT id, type, data 
                FROM hendelse 
                WHERE id IN (
                    SELECT hendelse_ref 
                    FROM vedtaksperiode_hendelse vh, behandling b 
                    WHERE vh.vedtaksperiode_id = b.vedtaksperiode_id AND b.spleis_behandling_id = :spleisBehandlingId
                ) 
                  AND type = '$GODKJENNING'
                  AND data -> 'Godkjenning' ->> 'behandlingId' = :spleisBehandlingId::text
                ORDER BY data ->> '@opprettet' DESC
                LIMIT 1
            """.trimMargin(),
            "spleisBehandlingId" to spleisBehandlingId,
        ).singleOrNull {
            fraMeldingtype(enumValueOf(it.string("type")), it.string("data")) as? Godkjenningsbehov
                ?: error("Forventer at melding funnet med meldingId=${it.uuid("id")} er et godkjenningsbehov")
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
            GODKJENNING -> Godkjenningsbehov.fraJson(json)
            OPPDATER_PERSONSNAPSHOT -> OppdaterPersondata(jsonNode)
            GOSYS_OPPGAVE_ENDRET -> GosysOppgaveEndret(jsonNode)
            VEDTAKSPERIODE_FORKASTET -> VedtaksperiodeForkastet(jsonNode)
            GODKJENT_TILBAKEDATERT_SYKMELDING -> TilbakedateringBehandlet(jsonNode)
            UTBETALING_ENDRET -> UtbetalingEndret(jsonNode)
            VEDTAKSPERIODE_REBEREGNET -> VedtaksperiodeReberegnet(jsonNode)
            ENDRET_EGEN_ANSATT_STATUS -> EndretEgenAnsattStatus(jsonNode)
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
            is OppdaterPersondata -> OPPDATER_PERSONSNAPSHOT
            is UtbetalingEndret -> UTBETALING_ENDRET
            is VedtaksperiodeReberegnet -> VEDTAKSPERIODE_REBEREGNET
            is GosysOppgaveEndret -> GOSYS_OPPGAVE_ENDRET
            is EndretEgenAnsattStatus -> ENDRET_EGEN_ANSATT_STATUS
            is NyeVarsler -> NYE_VARSLER
            is SøknadSendt -> SØKNAD_SENDT
            is VedtaksperiodeNyUtbetaling -> VEDTAKSPERIODE_NY_UTBETALING
            is TilbakedateringBehandlet -> GODKJENT_TILBAKEDATERT_SYKMELDING
            is BehandlingOpprettet -> BEHANDLING_OPPRETTET
            is AvsluttetUtenVedtakMessage -> AVSLUTTET_UTEN_VEDTAK
            is KlargjørTilgangsrelaterteData -> KLARGJØR_TILGANGSRELATERTE_DATA
            is StansAutomatiskBehandlingMelding -> Meldingtype.STANS_AUTOMATISK_BEHANDLING
            else -> throw IllegalArgumentException("ukjent meldingtype: ${melding::class.simpleName}")
        }
}
