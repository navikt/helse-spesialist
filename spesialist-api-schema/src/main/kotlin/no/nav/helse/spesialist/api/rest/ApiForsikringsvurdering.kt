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
    val navn: String
    val folketrygdlovenreferanse: ApiFolketrygdlovenreferanse
}

@Serializable
data class ApiFolketrygdlovenreferanse(
    val kapittel: Int,
    val paragrafIKapittel: Int,
    val ledd: Int?,
    val bokstav: Char?,
)

@Serializable
data class ApiForsikring(
    override val virkningsdato: LocalDate,
    override val opphørsdato: LocalDate?,
    override val dekningsgrad: Int,
    override val dekningIVentetid: Boolean,
    override val navn: String,
    override val folketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
) : ApiForsikringBase

@Serializable
enum class ApiEkskluderingsårsak {
    SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO,
    SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO,
    OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT,
    ALDRI_BETALT,
}

@Serializable
data class ApiEkskluderingsbegrunnelse(
    val forklaring: String,
    val folketrygdlovenreferanse: ApiFolketrygdlovenreferanse?,
)

@Serializable
data class ApiEkskludertForsikring(
    override val virkningsdato: LocalDate,
    override val opphørsdato: LocalDate?,
    override val dekningsgrad: Int,
    override val dekningIVentetid: Boolean,
    override val navn: String,
    override val folketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
    val ekskluderingsårsak: ApiEkskluderingsårsak,
    val ekskluderingsbegrunnelse: ApiEkskluderingsbegrunnelse,
) : ApiForsikringBase

@Serializable
data class ApiKollektivForsikring(
    val navn: String,
    val dekningFolketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
    val kollektivFolketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
)

@Serializable
data class ApiNavKjøptForsikringKonklusjon(
    val forklaring: String,
    val folketrygdlovenreferanse: ApiFolketrygdlovenreferanse?,
)

@Serializable
data class ApiNavKjøptForsikring(
    val navn: String,
    val dekningFolketrygdlovenreferanse: ApiFolketrygdlovenreferanse,
    val virkningsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val konklusjon: ApiNavKjøptForsikringKonklusjon,
    val lagtTilGrunn: Boolean,
)

@Serializable
data class ApiDekning(
    val grad: Int,
    val fraDag: Int,
)

@Serializable
data class ApiForsikringsvurdering(
    val eksisterer: Boolean,
    val forsikringInnhold: ForsikringInnhold?,
    val ekskluderteForsikringer: List<ApiEkskludertForsikring>,
    val gjeldendeForsikring: ApiForsikring?,
    val dataHentetTidspunkt: Instant,
    val samletDekning: ApiDekning?,
    val kollektivForsikring: ApiKollektivForsikring?,
    val navKjøpteForsikringer: List<ApiNavKjøptForsikring>,
    val vurdertTidspunkt: Instant,
)

@Serializable
data class ForsikringInnhold(
    val gjelderFraDag: Int,
    val dekningsgrad: Int,
)
