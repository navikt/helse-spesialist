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
        ).list { it.tilTilkommenInntektEvent() }
            .groupBy { it.metadata.tilkommenInntektId }
            .map { (_, events) -> events.tilTilkommenInntekt() }

    override fun finn(id: TilkommenInntektId): TilkommenInntekt? {
        val events =
            asSQL(
                """
                SELECT * FROM tilkommen_inntekt_events
                WHERE tilkommen_inntekt_id = :tilkommen_inntekt_id
                """.trimIndent(),
                "tilkommen_inntekt_id" to id.value,
            ).list { it.tilTilkommenInntektEvent() }
        return events.takeUnless { it.isEmpty() }?.tilTilkommenInntekt()
    }

    private fun Row.tilTilkommenInntektEvent(): TilkommenInntektEvent {
        val metadata =
            TilkommenInntektEvent.Metadata(
                tilkommenInntektId = TilkommenInntektId(uuid("tilkommen_inntekt_id")),
                sekvensnummer = int("sekvensnummer"),
                tidspunkt = instant("tidspunkt"),
                utførtAvSaksbehandlerIdent = string("utført_av_saksbehandler_ident"),
                notatTilBeslutter = string("notat_til_beslutter"),
                totrinnsvurderingId = TotrinnsvurderingId(long("totrinnsvurdering_id")),
            )

        return when (enumValueOf<DBEventType>(string("type"))) {
            DBEventType.OPPRETTET -> {
                val data = objectMapper.readValue(string("data_json"), DBOpprettetEventData::class.java)
                TilkommenInntektOpprettetEvent(
                    metadata = metadata,
                    fødselsnummer = string("fødselsnummer"),
                    // Det finnes bare identifikatorer som er organisasjonsnummer per nå
                    organisasjonsnummer = data.inntektskildeIdentifikator.verdi,
                    periode = data.periode.tilPeriode(),
                    periodebeløp = data.periodebeløp,
                    dager = TilkommenInntekt.tilDager(data.periode.tilPeriode(), data.ekskluderteUkedager),
                    ekskluderteUkedager = data.ekskluderteUkedager,
                )
            }

            DBEventType.ENDRET -> {
                val data = objectMapper.readValue(string("data_json"), DBEndretEventData::class.java)
                TilkommenInntektEndretEvent(
                    metadata = metadata,
                    endringer = data.endringer.tilEndringer(),
                )
            }

            DBEventType.FJERNET -> {
                TilkommenInntektFjernetEvent(metadata = metadata)
            }

            DBEventType.GJENOPPRETTET -> {
                val data = objectMapper.readValue(string("data_json"), DBGjenopprettetEventData::class.java)
                TilkommenInntektGjenopprettetEvent(
                    metadata = metadata,
                    endringer = data.endringer.tilEndringer(),
                )
            }
        }
    }

    private fun DBEndringer.tilEndringer(): TilkommenInntektEvent.Endringer =
        TilkommenInntektEvent.Endringer(
            // Det finnes bare identifikatorer som er organisasjonsnummer per nå
            organisasjonsnummer =
                inntektskildeIdentifikator?.let {
                    Endring(
                        fra = it.fra.verdi,
                        til = it.til.verdi,
                    )
                },
            periode =
                periode?.let {
                    Endring(
                        fra = it.fra.tilPeriode(),
                        til = it.til.tilPeriode(),
                    )
                },
            periodebeløp =
                periodebeløp?.let {
                    Endring(
                        fra = it.fra,
                        til = it.til,
                    )
                },
            dager = null,
            ekskluderteUkedager =
                ekskluderteUkedager?.let {
                    Endring(
                        fra = it.fra,
                        til = it.til,
                    )
                },
        )

    private fun DBPeriode.tilPeriode(): Periode = Periode(fom = fom, tom = tom)

    private fun List<TilkommenInntektEvent>.tilTilkommenInntekt(): TilkommenInntekt =
        TilkommenInntekt.fraLagring(events = sortedBy { it.metadata.sekvensnummer })

    override fun lagre(tilkommenInntekt: TilkommenInntekt) {
        val sistePersisterteSekvensnummer =
            asSQL(
                """
                SELECT MAX(sekvensnummer) FROM tilkommen_inntekt_events
                WHERE tilkommen_inntekt_id = :tilkommen_inntekt_id
                """.trimIndent(),
                "tilkommen_inntekt_id" to tilkommenInntekt.id().value,
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
                "fodselsnummer" to tilkommenInntekt.fødselsnummer,
                "tilkommen_inntekt_id" to event.metadata.tilkommenInntektId.value,
                "sekvensnummer" to event.metadata.sekvensnummer,
                "tidspunkt" to event.metadata.tidspunkt,
                "utfort_av_saksbehandler_ident" to event.metadata.utførtAvSaksbehandlerIdent,
                "notat_til_beslutter" to event.metadata.notatTilBeslutter,
                "totrinnsvurdering_id" to event.metadata.totrinnsvurderingId.value,
                "type" to event.tilDBEventType().name,
                "data_json" to event.tilDBEventData()?.let(objectMapper::writeValueAsString),
            ).update()
        }
    }

    private fun TilkommenInntektEvent.tilDBEventType(): DBEventType =
        when (this) {
            is TilkommenInntektOpprettetEvent -> DBEventType.OPPRETTET
            is TilkommenInntektEndretEvent -> DBEventType.ENDRET
            is TilkommenInntektFjernetEvent -> DBEventType.FJERNET
            is TilkommenInntektGjenopprettetEvent -> DBEventType.GJENOPPRETTET
        }

    private fun TilkommenInntektEvent.tilDBEventData(): DBEventData? =
        when (this) {
            is TilkommenInntektOpprettetEvent -> {
                DBOpprettetEventData(
                    inntektskildeIdentifikator =
                        DBInntektskildeIdentifikator(
                            verdi = organisasjonsnummer,
                            type = DBInntektskildeIdentifikator.Type.ORGANISASJONSNUMMER,
                        ),
                    periode = periode.tilDBPeriode(),
                    periodebeløp = periodebeløp,
                    ekskluderteUkedager = ekskluderteUkedager,
                )
            }

            is TilkommenInntektEndretEvent ->
                DBEndretEventData(
                    endringer = endringer.tilDBEndringer(),
                )

            is TilkommenInntektFjernetEvent -> null
            is TilkommenInntektGjenopprettetEvent ->
                DBGjenopprettetEventData(
                    endringer = endringer.tilDBEndringer(),
                )
        }

    private fun TilkommenInntektEvent.Endringer.tilDBEndringer() =
        DBEndringer(
            inntektskildeIdentifikator =
                organisasjonsnummer?.let {
                    DBEndringer.DBEndring(
                        fra =
                            DBInntektskildeIdentifikator(
                                verdi = it.fra,
                                type = DBInntektskildeIdentifikator.Type.ORGANISASJONSNUMMER,
                            ),
                        til =
                            DBInntektskildeIdentifikator(
                                verdi = it.til,
                                type = DBInntektskildeIdentifikator.Type.ORGANISASJONSNUMMER,
                            ),
                    )
                },
            periode =
                periode?.let {
                    DBEndringer.DBEndring(
                        fra = it.fra.tilDBPeriode(),
                        til = it.til.tilDBPeriode(),
                    )
                },
            periodebeløp =
                periodebeløp?.let {
                    DBEndringer.DBEndring(
                        fra = it.fra,
                        til = it.til,
                    )
                },
            ekskluderteUkedager =
                ekskluderteUkedager?.let {
                    DBEndringer.DBEndring(
                        fra = it.fra,
                        til = it.til,
                    )
                },
        )

    private fun Periode.tilDBPeriode(): DBPeriode = DBPeriode(fom = fom, tom = tom)
}

private enum class DBEventType {
    OPPRETTET,
    ENDRET,
    FJERNET,
    GJENOPPRETTET,
}

private data class DBOpprettetEventData(
    val inntektskildeIdentifikator: DBInntektskildeIdentifikator,
    val periode: DBPeriode,
    val periodebeløp: BigDecimal,
    val ekskluderteUkedager: SortedSet<LocalDate>,
) : DBEventData

private data class DBEndretEventData(
    val endringer: DBEndringer,
) : DBEventData

private data class DBGjenopprettetEventData(
    val endringer: DBEndringer,
) : DBEventData

private interface DBEventData

private data class DBEndringer(
    val inntektskildeIdentifikator: DBEndring<DBInntektskildeIdentifikator>?,
    val periode: DBEndring<DBPeriode>?,
    val periodebeløp: DBEndring<BigDecimal>?,
    val ekskluderteUkedager: DBEndring<SortedSet<LocalDate>>?,
) {
    data class DBEndring<T>(val fra: T, val til: T)
}

private data class DBInntektskildeIdentifikator(
    val verdi: String,
    val type: Type,
) {
    enum class Type {
        ORGANISASJONSNUMMER,
    }
}

private data class DBPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)
