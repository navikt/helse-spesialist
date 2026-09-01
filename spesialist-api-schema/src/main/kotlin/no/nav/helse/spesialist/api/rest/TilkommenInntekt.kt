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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
            override val fra: ApiDatoPeriode,
            override val til: ApiDatoPeriode,
        ) : ApiEndringFraTil<ApiDatoPeriode>

        @Serializable
        data class BigDecimalEndring(
            override val fra: BigDecimal,
            override val til: BigDecimal,
        ) : ApiEndringFraTil<BigDecimal>

        @Serializable
        data class StringEndring(
            override val fra: String,
            override val til: String,
        ) : ApiEndringFraTil<String>

        @Serializable
        data class BooleanEndring(
            override val fra: Boolean,
            override val til: Boolean,
        ) : ApiEndringFraTil<Boolean>

        @Serializable
        data class ListLocalDateEndring(
            override val fra: List<LocalDate>,
            override val til: List<LocalDate>,
        ) : ApiEndringFraTil<List<LocalDate>>

        @Serializable
        sealed interface ApiEndringFraTil<T> {
            val fra: T
            val til: T
        }
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
data class ApiTilkommenInntektPatch(
    val endringer: ApiTilkommenInntektEndringer,
    val notatTilBeslutter: String,
) {
    @Serializable
    data class ApiTilkommenInntektEndringer(
        val organisasjonsnummer: ApiTilkommenInntektEvent.Endringer.StringEndring?,
        val periode: ApiTilkommenInntektEvent.Endringer.DatoPeriodeEndring?,
        val periodebeløp: ApiTilkommenInntektEvent.Endringer.BigDecimalEndring?,
        val ekskluderteUkedager: ApiTilkommenInntektEvent.Endringer.ListLocalDateEndring?,
        val fjernet: ApiTilkommenInntektEvent.Endringer.BooleanEndring?,
    )
}

@Serializable
data class ApiLeggTilTilkommenInntektRequest(
    val fodselsnummer: String,
    val verdier: ApiTilkommenInntektInput,
    val notatTilBeslutter: String,
)
