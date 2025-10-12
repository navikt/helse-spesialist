@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import io.github.smiley4.schemakenerator.core.annotations.Name
import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
@Name("TilkommenInntektskilde")
data class ApiTilkommenInntektskilde(
    val organisasjonsnummer: String,
    val inntekter: List<ApiTilkommenInntekt>,
)

@Serializable
@Name("TilkommenInntekt")
data class ApiTilkommenInntekt(
    val tilkommenInntektId: UUID,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
    val fjernet: Boolean,
    val erDelAvAktivTotrinnsvurdering: Boolean,
    val events: List<ApiTilkommenInntektEvent>,
)

@Serializable
@Name("TilkommenInntektInput")
data class ApiTilkommenInntektInput(
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
)

@Serializable
@Name("TilkommenInntektEvent")
sealed interface ApiTilkommenInntektEvent {
    @Suppress("ktlint:standard:backing-property-naming")
    val __typename: String
        get() = this::class.java.simpleName.removePrefix("Api")
    val metadata: Metadata

    @Serializable
    @Name("TilkommenInntektEventMetadata")
    data class Metadata(
        val sekvensnummer: Int,
        val tidspunkt: LocalDateTime,
        val utfortAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
    )

    @Serializable
    @Name("TilkommenInntektEventEndringer")
    data class Endringer(
        val organisasjonsnummer: StringEndring?,
        val periode: DatoPeriodeEndring?,
        val periodebelop: BigDecimalEndring?,
        val ekskluderteUkedager: ListLocalDateEndring?,
    ) {
        @Serializable
        @Name("TilkommenInntektEventEndringerDatoPeriodeEndring")
        data class DatoPeriodeEndring(
            val fra: ApiDatoPeriode,
            val til: ApiDatoPeriode,
        )

        @Serializable
        @Name("TilkommenInntektEventEndringerBigDecimalEndring")
        data class BigDecimalEndring(
            val fra: BigDecimal,
            val til: BigDecimal,
        )

        @Serializable
        @Name("TilkommenInntektEventEndringerStringEndring")
        data class StringEndring(
            val fra: String,
            val til: String,
        )

        @Serializable
        @Name("TilkommenInntektEventEndringerListLocalDateEndring")
        data class ListLocalDateEndring(
            val fra: List<LocalDate>,
            val til: List<LocalDate>,
        )
    }
}

@Serializable
@Name("TilkommenInntektOpprettetEvent")
data class ApiTilkommenInntektOpprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
) : ApiTilkommenInntektEvent

@Serializable
@Name("TilkommenInntektEndretEvent")
data class ApiTilkommenInntektEndretEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

@Serializable
@Name("TilkommenInntektFjernetEvent")
data class ApiTilkommenInntektFjernetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
) : ApiTilkommenInntektEvent

@Serializable
@Name("TilkommenInntektGjenopprettetEvent")
data class ApiTilkommenInntektGjenopprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

@Serializable
@Name("LeggTilTilkommenInntektResponse")
data class ApiLeggTilTilkommenInntektResponse(
    val tilkommenInntektId: UUID,
)

@Serializable
@Name("EndreTilkommenInntektRequest")
data class ApiEndreTilkommenInntektRequest(
    val endretTil: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)

@Serializable
@Name("FjernTilkommenInntektRequest")
data class ApiFjernTilkommenInntektRequest(
    val notatTilBeslutter: String,
)

@Serializable
@Name("GjenopprettTilkommenInntektRequest")
data class ApiGjenopprettTilkommenInntektRequest(
    val endretTil: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)

@Serializable
@Name("LeggTilTilkommenInntektRequest")
data class ApiLeggTilTilkommenInntektRequest(
    val fodselsnummer: String,
    val verdier: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)
