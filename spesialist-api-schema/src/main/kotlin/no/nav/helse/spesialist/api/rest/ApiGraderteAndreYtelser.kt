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
data class ApiLeggTilGraderteAndreYtelserRequest(
    val fodselsnummer: String,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelserType: ApiGraderteAndreYtelserType,
    val notatTilBeslutter: String,
)

@Serializable
enum class ApiGraderteAndreYtelserType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}

@Serializable
data class ApiGraderteAndreYtelserPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
)

@Serializable
data class ApiLeggTilGraderteAndreYtelserResponse(
    val andreYtelserId: UUID,
)

@Serializable
data class ApiGraderteAndreYtelser(
    val andreYtelserId: UUID,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelserType: ApiGraderteAndreYtelserType,
    val fjernet: Boolean,
    val events: List<ApiGraderteAndreYtelserEvent>,
)

@Serializable
data class ApiPatchEndreGraderteAndreYtelserRequest(
    val graderteAndreYtelserId: UUID,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelserType: ApiGraderteAndreYtelserType,
    val notatTilBeslutter: String,
)

@Serializable
data class ApiPostFjernGraderteAndreYtelserRequest(
    val notatTilBeslutter: String,
)

@Serializable
data class ApiPostGjenopprettGraderteAndreYtelserRequest(
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelserType: ApiGraderteAndreYtelserType,
    val notatTilBeslutter: String,
)

@Serializable
data class ApiPatchEndreGraderteAndreYtelserResponse(
    val andreYtelserId: UUID,
)

@Serializable
data class ApiPostFjernGraderteAndreYtelserResponse(
    val andreYtelserId: UUID,
)

@Serializable
data class ApiPostGjenopprettGraderteAndreYtelserResponse(
    val andreYtelserId: UUID,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiGraderteAndreYtelserEvent {
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
        val perioder: ListGradertAnnenYtelseEndring?,
        val andreYtelserType: GradertAnnenYtelseTypeEndring?,
    )

    @Serializable
    data class ListGradertAnnenYtelseEndring(
        val fra: List<ApiGradertAnnenYtelse>,
        val til: List<ApiGradertAnnenYtelse>,
    )

    @Serializable
    data class GradertAnnenYtelseTypeEndring(
        val fra: ApiGraderteAndreYtelserType,
        val til: ApiGraderteAndreYtelserType,
    )

    @Serializable
    data class ApiGradertAnnenYtelse(
        val periode: ApiDatoPeriode,
        val grad: Int,
    )
}

@Serializable
@SerialName("ApiGraderteAndreYtelserOpprettetEvent")
data class ApiGraderteAndreYtelserOpprettetEvent(
    override val metadata: ApiGraderteAndreYtelserEvent.Metadata,
    val perioder: List<ApiGraderteAndreYtelserPeriode>,
    val andreYtelserType: ApiGraderteAndreYtelserType,
) : ApiGraderteAndreYtelserEvent

@Serializable
@SerialName("ApiGraderteAndreYtelserEndretEvent")
data class ApiGraderteAndreYtelserEndretEvent(
    override val metadata: ApiGraderteAndreYtelserEvent.Metadata,
    val endringer: ApiGraderteAndreYtelserEvent.Endringer,
) : ApiGraderteAndreYtelserEvent

@Serializable
@SerialName("ApiGraderteAndreYtelserFjernetEvent")
data class ApiGraderteAndreYtelserFjernetEvent(
    override val metadata: ApiGraderteAndreYtelserEvent.Metadata,
) : ApiGraderteAndreYtelserEvent

@Serializable
@SerialName("ApiGraderteAndreYtelserGjenopprettetEvent")
data class ApiGraderteAndreYtelserGjenopprettetEvent(
    override val metadata: ApiGraderteAndreYtelserEvent.Metadata,
    val endringer: ApiGraderteAndreYtelserEvent.Endringer,
) : ApiGraderteAndreYtelserEvent
