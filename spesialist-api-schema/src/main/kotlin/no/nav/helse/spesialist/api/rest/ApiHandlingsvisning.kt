package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class ApiHandlingsvisning(
    val felter: List<ApiVisningsfelt>,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ApiVisningsfelt.Tekst::class),
    JsonSubTypes.Type(ApiVisningsfelt.Liste::class),
    JsonSubTypes.Type(ApiVisningsfelt.Endringer::class),
)
@Serializable
sealed interface ApiVisningsfelt {
    val tittel: String

    @SerialName("Tekst")
    @Serializable
    data class Tekst(
        override val tittel: String,
        val tekst: String,
    ) : ApiVisningsfelt

    @SerialName("Liste")
    @Serializable
    data class Liste(
        override val tittel: String,
        val elementer: List<String>,
    ) : ApiVisningsfelt

    @SerialName("Endringer")
    @Serializable
    data class Endringer(
        override val tittel: String,
        val grupper: List<ApiEndringsgruppe>,
    ) : ApiVisningsfelt
}

@Serializable
data class ApiEndringsgruppe(
    val rader: List<ApiEndringsrad>,
)

@Serializable
data class ApiEndringsrad(
    val navn: String,
    val fra: String?,
    val til: String,
)
