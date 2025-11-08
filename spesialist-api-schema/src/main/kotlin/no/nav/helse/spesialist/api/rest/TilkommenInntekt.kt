@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiTilkommenInntektskilde(
    val organisasjonsnummer: String,
    val inntekter: List<ApiTilkommenInntekt>,
)

@Serializable
data class ApiTilkommenInntekt(
    val tilkommenInntektId: UUID,
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
    val fjernet: Boolean,
    val erDelAvAktivTotrinnsvurdering: Boolean,
    val events: List<ApiTilkommenInntektEvent>,
)

@Serializable
data class ApiTilkommenInntektInput(
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiTilkommenInntektEvent {
    @Suppress("ktlint:standard:backing-property-naming")
    val __typename: String
        get() = this::class.java.simpleName.removePrefix("Api")
    val metadata: Metadata

    @Serializable
    data class Metadata(
        val sekvensnummer: Int,
        val tidspunkt: LocalDateTime,
        val utfortAvSaksbehandlerIdent: String,
        val notatTilBeslutter: String,
    )

    @Serializable
    data class Endringer(
        val organisasjonsnummer: StringEndring?,
        val periode: DatoPeriodeEndring?,
        val periodebelop: BigDecimalEndring?,
        val ekskluderteUkedager: ListLocalDateEndring?,
    ) {
        @Serializable
        data class DatoPeriodeEndring(
            val fra: ApiDatoPeriode,
            val til: ApiDatoPeriode,
        )

        @Serializable
        data class BigDecimalEndring(
            val fra: BigDecimal,
            val til: BigDecimal,
        )

        @Serializable
        data class StringEndring(
            val fra: String,
            val til: String,
        )

        @Serializable
        data class ListLocalDateEndring(
            val fra: List<LocalDate>,
            val til: List<LocalDate>,
        )
    }
}

@Serializable
@SerialName("ApiTilkommenInntektOpprettetEvent")
data class ApiTilkommenInntektOpprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val organisasjonsnummer: String,
    val periode: ApiDatoPeriode,
    val periodebelop: BigDecimal,
    val ekskluderteUkedager: List<LocalDate>,
) : ApiTilkommenInntektEvent

@Serializable
@SerialName("ApiTilkommenInntektEndretEvent")
data class ApiTilkommenInntektEndretEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

@Serializable
@SerialName("ApiTilkommenInntektFjernetEvent")
data class ApiTilkommenInntektFjernetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
) : ApiTilkommenInntektEvent

@Serializable
@SerialName("ApiTilkommenInntektGjenopprettetEvent")
data class ApiTilkommenInntektGjenopprettetEvent(
    override val metadata: ApiTilkommenInntektEvent.Metadata,
    val endringer: ApiTilkommenInntektEvent.Endringer,
) : ApiTilkommenInntektEvent

@Serializable
data class ApiLeggTilTilkommenInntektResponse(
    val tilkommenInntektId: UUID,
)

@Serializable
data class ApiEndreTilkommenInntektRequest(
    val endretTil: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)

@Serializable
data class ApiFjernTilkommenInntektRequest(
    val notatTilBeslutter: String,
)

@Serializable
data class ApiGjenopprettTilkommenInntektRequest(
    val endretTil: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)

@Serializable
data class ApiTilkommenInntektPatch(
    val endringer: ApiTilkommenInntektEndringer,
    val notatTilBeslutter: String,
) {
    @Serializable
    data class ApiTilkommenInntektEndringer(
        val organisasjonsnummer: ApiPatchEndring<String>?,
        val periode: ApiPatchEndring<ApiDatoPeriode>?,
        val periodebeløp: ApiPatchEndring<BigDecimal>?,
        val ekskluderteUkedager: ApiPatchEndring<List<LocalDate>>?,
        val fjernet: ApiPatchEndring<Boolean>?,
    )
}

@Serializable
data class ApiPatchEndring<T>(
    val fra: T,
    val til: T,
)

@Serializable
data class ApiLeggTilTilkommenInntektRequest(
    val fodselsnummer: String,
    val verdier: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)
