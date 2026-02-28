@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiSamlingAvVurderteInngangsvilkår(
    val samlingAvVurderteInngangsvilkårId: UUID,
    val versjon: Int,
    val skjæringstidspunkt: LocalDate,
    val vurderteInngangsvilkår: List<ApiVurdertInngangsvilkår>,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface ApiVurdertInngangsvilkår {
    val id: UUID
    val vilkårskode: String
    val vurderingskode: String?
    val tidspunkt: Instant

    @SerialName("AUTOMATISK")
    @Serializable
    data class Automatisk(
        override val id: UUID,
        override val vilkårskode: String,
        override val vurderingskode: String?,
        override val tidspunkt: Instant,
        val automatiskVurdering: ApiAutomatiskVurdering,
    ) : ApiVurdertInngangsvilkår

    @SerialName("MANUELL")
    @Serializable
    data class Manuell(
        override val id: UUID,
        override val vilkårskode: String,
        override val vurderingskode: String?,
        override val tidspunkt: Instant,
        val manuellVurdering: ApiManuellVurdering,
    ) : ApiVurdertInngangsvilkår
}

@Serializable
data class ApiAutomatiskVurdering(
    val system: String,
    val versjon: String,
    val grunnlagsdata: Map<String, String>,
)

@Serializable
data class ApiManuellVurdering(
    val navident: String,
    val begrunnelse: String,
)

@Serializable
data class ApiPostManuelleInngangsvilkårVurderingerRequest(
    val versjon: Int,
    val vurderinger: List<ApiManuellInngangsvilkårVurdering>,
)

@Serializable
data class ApiManuellInngangsvilkårVurdering(
    val vilkårskode: String,
    val vurderingskode: String,
    val begrunnelse: String,
)
