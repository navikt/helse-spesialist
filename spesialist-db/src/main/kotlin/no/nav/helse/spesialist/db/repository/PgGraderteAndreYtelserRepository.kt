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
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.*
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import java.time.LocalDate

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

    override fun finn(id: GraderteAndreYtelserId): GraderteAndreYtelser? {
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
                "data_json" to event.tilDBEventData()?.let(objectMapper::writeValueAsString),
            ).update()
        }
    }

    private fun Row.tilGraderteAndreYtelserEvent(): GraderteAndreYtelserEvent {
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
                    graderteAndreYtelserPerioder = data.graderteAndreYtelserPerioder.map { it.tilGraderteAndreYtelserPeriode() },
                    graderteAndreYtelserType = data.graderteAndreYtelserType.tilGraderteAndreYtelserType(),
                )
            }

            DBEventType.ENDRET -> {
                val data = objectMapper.readValue(string("data_json"), DBEndretEventData::class.java)
                GraderteAndreYtelserEndretEvent(
                    metadata = metadata,
                    endringer = data.endringer.tilEndringer(),
                )
            }

            DBEventType.FJERNET -> {
                GraderteAndreYtelserFjernetEvent(metadata = metadata)
            }

            DBEventType.GJENOPPRETTET -> {
                val data = objectMapper.readValue(string("data_json"), DBGjenopprettetEventData::class.java)
                GraderteAndreYtelserGjenopprettetEvent(
                    metadata = metadata,
                    endringer = data.endringer.tilEndringer(),
                )
            }
        }
    }

    private fun DBEndringer.tilEndringer(): GraderteAndreYtelserEvent.Endringer =
        GraderteAndreYtelserEvent.Endringer(
            graderteAndreYtelserPerioder =
                graderteAndreYtelserPerioder?.let {
                    Endring(
                        fra = it.fra.map { periode -> periode.tilGraderteAndreYtelserPeriode() },
                        til = it.til.map { periode -> periode.tilGraderteAndreYtelserPeriode() },
                    )
                },
            graderteAndreYtelserType =
                graderteAndreYtelserType?.let {
                    Endring(
                        fra = it.fra.tilGraderteAndreYtelserType(),
                        til = it.til.tilGraderteAndreYtelserType(),
                    )
                },
        )

    private fun DBGraderteAndreYtelserPeriode.tilGraderteAndreYtelserPeriode() =
        GraderteAndreYtelserPeriode(
            periode = periode.tilPeriode(),
            grad = grad,
        )

    private fun DBGraderteAndreYtelserType.tilGraderteAndreYtelserType() =
        when (this) {
            DBGraderteAndreYtelserType.FORELDREPENGER -> GraderteAndreYtelserType.FORELDREPENGER
            DBGraderteAndreYtelserType.SVANGERSKAPSPENGER -> GraderteAndreYtelserType.SVANGERSKAPSPENGER
            DBGraderteAndreYtelserType.OMSORGSPENGER -> GraderteAndreYtelserType.OMSORGSPENGER
            DBGraderteAndreYtelserType.PLEIEPENGER -> GraderteAndreYtelserType.PLEIEPENGER
            DBGraderteAndreYtelserType.OPPLARINGSPENGER -> GraderteAndreYtelserType.OPPLARINGSPENGER
        }

    private fun DBPeriode.tilPeriode(): Periode = Periode(fom = fom, tom = tom)

    private fun List<GraderteAndreYtelserEvent>.tilGraderteAndreYtelser(): GraderteAndreYtelser = GraderteAndreYtelser.fraLagring(events = sortedBy { it.metadata.sekvensnummer })

    private fun GraderteAndreYtelserEvent.tilDBEventType(): DBEventType =
        when (this) {
            is GraderteAndreYtelserOpprettetEvent -> DBEventType.OPPRETTET
            is GraderteAndreYtelserEndretEvent -> DBEventType.ENDRET
            is GraderteAndreYtelserFjernetEvent -> DBEventType.FJERNET
            is GraderteAndreYtelserGjenopprettetEvent -> DBEventType.GJENOPPRETTET
        }

    private fun GraderteAndreYtelserEvent.tilDBEventData(): DBEventData? =
        when (this) {
            is GraderteAndreYtelserOpprettetEvent -> {
                DBOpprettetEventData(
                    graderteAndreYtelserPerioder = graderteAndreYtelserPerioder.map { it.tilDBGraderteAndreYtelserPeriode() },
                    graderteAndreYtelserType = graderteAndreYtelserType.tilDBGraderteAndreYtelserType(),
                )
            }

            is GraderteAndreYtelserEndretEvent -> {
                DBEndretEventData(
                    endringer = endringer.tilDBEndringer(),
                )
            }

            is GraderteAndreYtelserFjernetEvent -> {
                null
            }

            is GraderteAndreYtelserGjenopprettetEvent -> {
                DBGjenopprettetEventData(
                    endringer = endringer.tilDBEndringer(),
                )
            }
        }

    private fun GraderteAndreYtelserEvent.Endringer.tilDBEndringer() =
        DBEndringer(
            graderteAndreYtelserPerioder =
                graderteAndreYtelserPerioder?.let {
                    DBEndringer.DBEndring(
                        fra = it.fra.map { periode -> periode.tilDBGraderteAndreYtelserPeriode() },
                        til = it.til.map { periode -> periode.tilDBGraderteAndreYtelserPeriode() },
                    )
                },
            graderteAndreYtelserType =
                graderteAndreYtelserType?.let {
                    DBEndringer.DBEndring(
                        fra = it.fra.tilDBGraderteAndreYtelserType(),
                        til = it.til.tilDBGraderteAndreYtelserType(),
                    )
                },
        )

    private fun GraderteAndreYtelserPeriode.tilDBGraderteAndreYtelserPeriode() =
        DBGraderteAndreYtelserPeriode(
            periode = periode.tilDBPeriode(),
            grad = grad,
        )

    private fun GraderteAndreYtelserType.tilDBGraderteAndreYtelserType() =
        when (this) {
            GraderteAndreYtelserType.FORELDREPENGER -> DBGraderteAndreYtelserType.FORELDREPENGER
            GraderteAndreYtelserType.SVANGERSKAPSPENGER -> DBGraderteAndreYtelserType.SVANGERSKAPSPENGER
            GraderteAndreYtelserType.OMSORGSPENGER -> DBGraderteAndreYtelserType.OMSORGSPENGER
            GraderteAndreYtelserType.PLEIEPENGER -> DBGraderteAndreYtelserType.PLEIEPENGER
            GraderteAndreYtelserType.OPPLARINGSPENGER -> DBGraderteAndreYtelserType.OPPLARINGSPENGER
        }

    private fun Periode.tilDBPeriode(): DBPeriode = DBPeriode(fom = fom, tom = tom)

    private enum class DBEventType {
        OPPRETTET,
        ENDRET,
        FJERNET,
        GJENOPPRETTET,
    }

    private enum class DBGraderteAndreYtelserType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
        OMSORGSPENGER,
        PLEIEPENGER,
        OPPLARINGSPENGER,
    }

    private data class DBOpprettetEventData(
        val graderteAndreYtelserPerioder: List<DBGraderteAndreYtelserPeriode>,
        val graderteAndreYtelserType: DBGraderteAndreYtelserType,
    ) : DBEventData

    private data class DBEndretEventData(
        val endringer: DBEndringer,
    ) : DBEventData

    private data class DBGjenopprettetEventData(
        val endringer: DBEndringer,
    ) : DBEventData

    private interface DBEventData

    private data class DBEndringer(
        val graderteAndreYtelserPerioder: DBEndring<List<DBGraderteAndreYtelserPeriode>>?,
        val graderteAndreYtelserType: DBEndring<DBGraderteAndreYtelserType>?,
    ) {
        data class DBEndring<T>(
            val fra: T,
            val til: T,
        )
    }

    private data class DBGraderteAndreYtelserPeriode(
        val periode: DBPeriode,
        val grad: Int,
    )

    private data class DBPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
