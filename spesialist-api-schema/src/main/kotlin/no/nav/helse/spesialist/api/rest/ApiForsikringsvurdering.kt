@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiFolketrygdlovenreferanse(
    val kapittel: Int,
    val paragrafIKapittel: Int,
    val ledd: Int?,
    val bokstav: Char?,
)

@Serializable
data class ApiKollektivForsikring(
    val navn: String,
    val dekningFolketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
    val kollektivFolketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
)

@Serializable
data class ApiIndividuellForsikringKonklusjon(
    val forklaring: String,
    val folketrygdlovenreferanse: ApiFolketrygdlovenreferanse?,
)

@Serializable
data class ApiIndividuellForsikring(
    val navn: String,
    val dekningFolketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val konklusjon: ApiIndividuellForsikringKonklusjon,
    val lagtTilGrunn: Boolean,
)

@Serializable
data class ApiDekning(
    val grad: Int,
    val fraDag: Int,
)

@Serializable
data class ApiForsikringsvurdering(
    val samletDekning: ApiDekning?,
    val kollektivForsikring: ApiKollektivForsikring?,
    val individuelleForsikringer: List<ApiIndividuellForsikring>,
    val vurdertTidspunkt: Instant,
)
