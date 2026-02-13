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

@Suppress("ktlint:standard:enum-entry-name-case")
enum class ApiOppgaveSorteringsfelt {
    tildeling,
    opprettetTidspunkt,
    paVentInfo_tidsfrist,
    behandlingOpprettetTidspunkt,
}

@Serializable
enum class ApiSorteringsrekkefølge {
    STIGENDE,
    SYNKENDE,
}
