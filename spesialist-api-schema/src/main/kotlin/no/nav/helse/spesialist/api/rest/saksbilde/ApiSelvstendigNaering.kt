@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import com.expediagroup.graphql.generator.annotations.GraphQLName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@GraphQLName("SelvstendigNaering")
@Serializable
data class ApiSelvstendigNaering(
    val behandlinger: List<ApiBehandling>,
    val overstyringer: List<ApiOverstyring>,
)
