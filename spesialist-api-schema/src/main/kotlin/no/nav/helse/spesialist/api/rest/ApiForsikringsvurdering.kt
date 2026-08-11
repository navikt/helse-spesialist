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
sealed interface ApiForsikringBase {
    val virkningsdato: LocalDate
    val opphørsdato: LocalDate?
    val dekningsgrad: Int
    val dekningIVentetid: Boolean
}

@Serializable
data class ApiForsikring(
    override val virkningsdato: LocalDate,
    override val opphørsdato: LocalDate?,
    override val dekningsgrad: Int,
    override val dekningIVentetid: Boolean,
) : ApiForsikringBase

@Serializable
enum class ApiEkskluderingsårsak {
    SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO,
    SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO,
    OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT,
    ALDRI_BETALT,
}

@Serializable
data class ApiEkskludertForsikring(
    override val virkningsdato: LocalDate,
    override val opphørsdato: LocalDate?,
    override val dekningsgrad: Int,
    override val dekningIVentetid: Boolean,
    val ekskluderingsårsak: ApiEkskluderingsårsak,
) : ApiForsikringBase

@Serializable
data class ApiForsikringsvurdering(
    val eksisterer: Boolean,
    val forsikringInnhold: ForsikringInnhold?,
    val ekskluderteForsikringer: List<ApiEkskludertForsikring>,
    val gjeldendeForsikring: ApiForsikring?,
)

@Serializable
data class ForsikringInnhold(
    val gjelderFraDag: Int,
    val dekningsgrad: Int,
)
