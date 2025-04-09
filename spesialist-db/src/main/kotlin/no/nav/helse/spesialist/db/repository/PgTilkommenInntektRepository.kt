package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.application.TilkommenInntektRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.tilkommeninntekt.Endring
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEndretEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektFjernetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektGjenopprettetEvent
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektOpprettetEvent
import java.math.BigDecimal
import java.time.LocalDate
import java.util.SortedSet

class PgTilkommenInntektRepository(
    session: Session,
) : QueryRunner by MedSession(session), TilkommenInntektRepository {
    override fun finnAlleForFødselsnummer(fødselsnummer: String): List<TilkommenInntekt> =
        asSQL(
            """
            SELECT * FROM tilkommen_inntekt_events
            WHERE fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).list { it.toTilkommenInntektEvent() }
            .groupBy { it.metadata.tilkommenInntektId }
            .map { (_, events) -> events.sortedBy { it.metadata.sekvensnummer }.let(TilkommenInntekt::fraLagring) }

    override fun finn(id: TilkommenInntektId): TilkommenInntekt? {
        val events =
            asSQL(
                """
                SELECT * FROM tilkommen_inntekt_events
                WHERE fødselsnummer = :fodselsnummer
                AND tilkommen_inntekt_id = :tilkommen_inntekt_id
                ORDER BY sekvensnummer
                """.trimIndent(),
                "fodselsnummer" to id.fødselsnummer,
                "tilkommen_inntekt_id" to id.uuid,
            ).list { it.toTilkommenInntektEvent() }
        return events.takeUnless { it.isEmpty() }?.let(TilkommenInntekt::fraLagring)
    }

    override fun lagre(tilkommenInntekt: TilkommenInntekt) {
        val sistePersisterteSekvensnummer =
            asSQL(
                """SELECT MAX(sekvensnummer) FROM tilkommen_inntekt_events WHERE fødselsnummer = :fodselsnummer""",
                "fodselsnummer" to tilkommenInntekt.id().fødselsnummer,
            ).singleOrNull { it.intOrNull(1) }
        if (sistePersisterteSekvensnummer != null) {
            tilkommenInntekt.events.filter { it.metadata.sekvensnummer > sistePersisterteSekvensnummer }
        } else {
            tilkommenInntekt.events
        }.forEach { event ->
            asSQL(
                """
                INSERT INTO tilkommen_inntekt_events (
                  fødselsnummer,
                  tilkommen_inntekt_id,
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
                  :tilkommen_inntekt_id,
                  :sekvensnummer,
                  :tidspunkt,
                  :utfort_av_saksbehandler_ident,
                  :notat_til_beslutter,
                  :totrinnsvurdering_id,
                  :type,
                  :data_json
                )
                """.trimIndent(),
                "fodselsnummer" to event.metadata.tilkommenInntektId.fødselsnummer,
                "tilkommen_inntekt_id" to event.metadata.tilkommenInntektId.uuid,
                "sekvensnummer" to event.metadata.sekvensnummer,
                "tidspunkt" to event.metadata.tidspunkt,
                "utfort_av_saksbehandler_ident" to event.metadata.utførtAvSaksbehandlerIdent,
                "notat_til_beslutter" to event.metadata.notatTilBeslutter,
                "totrinnsvurdering_id" to event.metadata.totrinnsvurderingId.value,
                "type" to
                    when (event) {
                        is TilkommenInntektOpprettetEvent -> EventType.OPPRETTET
                        is TilkommenInntektEndretEvent -> EventType.ENDRET
                        is TilkommenInntektFjernetEvent -> EventType.FJERNET
                        is TilkommenInntektGjenopprettetEvent -> EventType.GJENOPPRETTET
                    }.name,
                "data_json" to
                    when (event) {
                        is TilkommenInntektOpprettetEvent ->
                            OpprettetEventData(
                                organisasjonsnummer = event.organisasjonsnummer,
                                fom = event.periode.fom,
                                tom = event.periode.tom,
                                periodebeløp = event.periodebeløp,
                                dager = event.dager,
                            )

                        is TilkommenInntektEndretEvent ->
                            EndretEventData(
                                endringer =
                                    event.endringer.let { endring ->
                                        Endringer(
                                            organisasjonsnummer = endring.organisasjonsnummer.tilJsonEndring(),
                                            fom = endring.fom.tilJsonEndring(),
                                            tom = endring.tom.tilJsonEndring(),
                                            periodebeløp = endring.periodebeløp.tilJsonEndring(),
                                            dager = endring.dager.tilJsonEndring(),
                                        )
                                    },
                            )

                        is TilkommenInntektFjernetEvent -> null
                        is TilkommenInntektGjenopprettetEvent ->
                            GjenopprettetEventData(
                                endringer =
                                    event.endringer.let { endring ->
                                        Endringer(
                                            organisasjonsnummer = endring.organisasjonsnummer.tilJsonEndring(),
                                            fom = endring.fom.tilJsonEndring(),
                                            tom = endring.tom.tilJsonEndring(),
                                            periodebeløp = endring.periodebeløp.tilJsonEndring(),
                                            dager = endring.dager.tilJsonEndring(),
                                        )
                                    },
                            )
                    }?.let(objectMapper::writeValueAsString),
            ).update()
        }
    }

    private fun <T> Endring<T>?.tilJsonEndring(): Endringer.Endring<T>? = this?.let { Endringer.Endring(it.fra, it.til) }

    private fun Row.toTilkommenInntektEvent(): TilkommenInntektEvent {
        val metadata =
            TilkommenInntektEvent.Metadata(
                tilkommenInntektId =
                    TilkommenInntektId(
                        fødselsnummer = string("fødselsnummer"),
                        uuid = uuid("tilkommen_inntekt_id"),
                    ),
                sekvensnummer = int("sekvensnummer"),
                tidspunkt = instant("tidspunkt"),
                utførtAvSaksbehandlerIdent = string("utført_av_saksbehandler_ident"),
                notatTilBeslutter = string("notat_til_beslutter"),
                totrinnsvurderingId = TotrinnsvurderingId(long("totrinnsvurdering_id")),
            )

        return when (enumValueOf<EventType>(string("type"))) {
            EventType.OPPRETTET -> {
                val data = objectMapper.readValue(string("data_json"), OpprettetEventData::class.java)
                TilkommenInntektOpprettetEvent(
                    metadata = metadata,
                    organisasjonsnummer = data.organisasjonsnummer,
                    periode =
                        Periode(
                            fom = data.fom,
                            tom = data.tom,
                        ),
                    periodebeløp = data.periodebeløp,
                    dager = data.dager,
                )
            }

            EventType.ENDRET -> {
                val data = objectMapper.readValue(string("data_json"), EndretEventData::class.java)
                TilkommenInntektEndretEvent(
                    metadata = metadata,
                    endringer = data.endringer.tilDomainEndringer(),
                )
            }

            EventType.FJERNET -> {
                TilkommenInntektFjernetEvent(metadata = metadata)
            }

            EventType.GJENOPPRETTET -> {
                val data = objectMapper.readValue(string("data_json"), GjenopprettetEventData::class.java)
                TilkommenInntektGjenopprettetEvent(
                    metadata = metadata,
                    endringer = data.endringer.tilDomainEndringer(),
                )
            }
        }
    }

    private fun Endringer.tilDomainEndringer(): TilkommenInntektEvent.Endringer =
        TilkommenInntektEvent.Endringer(
            organisasjonsnummer = organisasjonsnummer.tilDomainEndring(),
            fom = fom.tilDomainEndring(),
            tom = tom.tilDomainEndring(),
            periodebeløp = periodebeløp.tilDomainEndring(),
            dager = dager.tilDomainEndring(),
        )

    private fun <T> Endringer.Endring<T>?.tilDomainEndring(): Endring<T>? = this?.let { Endring(fra = it.fra, til = it.til) }

    enum class EventType {
        OPPRETTET,
        ENDRET,
        FJERNET,
        GJENOPPRETTET,
    }

    data class OpprettetEventData(
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val periodebeløp: BigDecimal,
        val dager: SortedSet<LocalDate>,
    ) : EventData

    data class EndretEventData(
        val endringer: Endringer,
    ) : EventData

    data class GjenopprettetEventData(
        val endringer: Endringer,
    ) : EventData

    interface EventData

    data class Endringer(
        val organisasjonsnummer: Endring<String>?,
        val fom: Endring<LocalDate>?,
        val tom: Endring<LocalDate>?,
        val periodebeløp: Endring<BigDecimal>?,
        val dager: Endring<SortedSet<LocalDate>>?,
    ) {
        data class Endring<T>(val fra: T, val til: T)
    }
}
