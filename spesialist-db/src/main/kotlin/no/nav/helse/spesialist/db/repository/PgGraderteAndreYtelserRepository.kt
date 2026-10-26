package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.GraderteAndreYtelserRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.*

class PgGraderteAndreYtelserRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    GraderteAndreYtelserRepository {
    override fun finnAlleForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<GraderteAndreYtelser> =
        asSQL(
            """
            SELECT * FROM graderte_andre_ytelser_events
            WHERE fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to identitetsnummer.value,
        ).list { it.tilGraderteAndreYtelserEvent() }
            .groupBy { it.metadata.graderteAndreYtelserId }
            .map { (_, events) -> events.tilGraderteAndreYtelser() }

    override fun finnOrNull(id: GraderteAndreYtelserId): GraderteAndreYtelser? {
        val events =
            asSQL(
                """
                SELECT * FROM graderte_andre_ytelser_events
                WHERE graderte_andre_ytelser_id = :graderte_andre_ytelser_id
                """.trimIndent(),
                "graderte_andre_ytelser_id" to id.value,
            ).list { it.tilGraderteAndreYtelserEvent() }
        return events.takeUnless { it.isEmpty() }?.tilGraderteAndreYtelser()
    }

    override fun lagre(graderteAndreYtelser: GraderteAndreYtelser) {
        val sistePersisterteSekvensnummer =
            asSQL(
                """
                SELECT MAX(sekvensnummer) FROM graderte_andre_ytelser_events
                WHERE graderte_andre_ytelser_id = :graderte_andre_ytelser_id
                """.trimIndent(),
                "graderte_andre_ytelser_id" to graderteAndreYtelser.id.value,
            ).singleOrNull { it.intOrNull(1) }
        if (sistePersisterteSekvensnummer != null) {
            graderteAndreYtelser.events.filter { it.metadata.sekvensnummer > sistePersisterteSekvensnummer }
        } else {
            graderteAndreYtelser.events
        }.forEach { event ->
            asSQL(
                """
                INSERT INTO graderte_andre_ytelser_events (
                  fødselsnummer,
                  graderte_andre_ytelser_id,
                  sekvensnummer,
                  tidspunkt,
                  utført_av_saksbehandler_ident,
                  notat_til_beslutter,
                  totrinnsvurdering_id,
                  type,
                  data_json
                )
                VALUES (
                  :fodselsnummer,
                  :graderte_andre_ytelser_id,
                  :sekvensnummer,
                  :tidspunkt,
                  :utfort_av_saksbehandler_ident,
                  :notat_til_beslutter,
                  :totrinnsvurdering_id,
                  :type,
                  :data_json
                )
                """.trimIndent(),
                "fodselsnummer" to graderteAndreYtelser.identitetsnummer.value,
                "graderte_andre_ytelser_id" to event.metadata.graderteAndreYtelserId.value,
                "sekvensnummer" to event.metadata.sekvensnummer,
                "tidspunkt" to event.metadata.tidspunkt,
                "utfort_av_saksbehandler_ident" to event.metadata.utførtAvSaksbehandlerIdent.value,
                "notat_til_beslutter" to event.metadata.notatTilBeslutter,
                "totrinnsvurdering_id" to event.metadata.totrinnsvurderingId.value,
                "type" to event.tilDBEventType().name,
                "data_json" to objectMapper.writeValueAsString(event.tilDBEventData()),
            ).update()
        }
    }

    private fun Row.tilGraderteAndreYtelserEvent(): GraderteAndreYtelserOpprettetEvent {
        val metadata =
            GraderteAndreYtelserEvent.Metadata(
                graderteAndreYtelserId = GraderteAndreYtelserId(uuid("graderte_andre_ytelser_id")),
                sekvensnummer = int("sekvensnummer"),
                tidspunkt = instant("tidspunkt"),
                utførtAvSaksbehandlerIdent = NAVIdent(string("utført_av_saksbehandler_ident")),
                notatTilBeslutter = string("notat_til_beslutter"),
                totrinnsvurderingId = TotrinnsvurderingId(long("totrinnsvurdering_id")),
            )

        return when (enumValueOf<DBEventType>(string("type"))) {
            DBEventType.OPPRETTET -> {
                val data = objectMapper.readValue(string("data_json"), DBOpprettetEventData::class.java)
                GraderteAndreYtelserOpprettetEvent(
                    metadata = metadata,
                    fødselsnummer = string("fødselsnummer"),
                    graderteAndreYtelserType = data.graderteAndreYtelserType.tilGraderteAndreYtelserType(),
                )
            }
        }
    }

    private fun DBGraderteAndreYtelserType.tilGraderteAndreYtelserType() =
        when (this) {
            DBGraderteAndreYtelserType.FORELDREPENGER -> GraderteAndreYtelserType.FORELDREPENGER
            DBGraderteAndreYtelserType.SVANGERSKAPSPENGER -> GraderteAndreYtelserType.SVANGERSKAPSPENGER
            DBGraderteAndreYtelserType.OMSORGSPENGER -> GraderteAndreYtelserType.OMSORGSPENGER
            DBGraderteAndreYtelserType.PLEIEPENGER -> GraderteAndreYtelserType.PLEIEPENGER
            DBGraderteAndreYtelserType.OPPLARINGSPENGER -> GraderteAndreYtelserType.OPPLARINGSPENGER
        }

    private fun List<GraderteAndreYtelserOpprettetEvent>.tilGraderteAndreYtelser(): GraderteAndreYtelser = GraderteAndreYtelser.fraLagring(single())

    private fun GraderteAndreYtelserEvent.tilDBEventType(): DBEventType =
        when (this) {
            is GraderteAndreYtelserOpprettetEvent -> DBEventType.OPPRETTET
        }

    private fun GraderteAndreYtelserEvent.tilDBEventData(): DBEventData =
        when (this) {
            is GraderteAndreYtelserOpprettetEvent -> {
                DBOpprettetEventData(
                    graderteAndreYtelserType = graderteAndreYtelserType.tilDBGraderteAndreYtelserType(),
                )
            }
        }

    private fun GraderteAndreYtelserType.tilDBGraderteAndreYtelserType() =
        when (this) {
            GraderteAndreYtelserType.FORELDREPENGER -> DBGraderteAndreYtelserType.FORELDREPENGER
            GraderteAndreYtelserType.SVANGERSKAPSPENGER -> DBGraderteAndreYtelserType.SVANGERSKAPSPENGER
            GraderteAndreYtelserType.OMSORGSPENGER -> DBGraderteAndreYtelserType.OMSORGSPENGER
            GraderteAndreYtelserType.PLEIEPENGER -> DBGraderteAndreYtelserType.PLEIEPENGER
            GraderteAndreYtelserType.OPPLARINGSPENGER -> DBGraderteAndreYtelserType.OPPLARINGSPENGER
        }

    private enum class DBEventType {
        OPPRETTET,
    }

    private enum class DBGraderteAndreYtelserType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
        OMSORGSPENGER,
        PLEIEPENGER,
        OPPLARINGSPENGER,
    }

    private data class DBOpprettetEventData(
        val graderteAndreYtelserType: DBGraderteAndreYtelserType,
    ) : DBEventData

    private interface DBEventData
}
