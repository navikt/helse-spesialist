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
data class ApiVeilederStans(
    val erStanset: Boolean,
    val årsaker: Set<Årsak>,
    val tidspunkt: LocalDateTime?,
) {
    enum class Årsak {
        MEDISINSK_VILKAR,
        AKTIVITETSKRAV,
        MANGLENDE_MEDVIRKING,

        // BESTRIDELSE_SYKMELDING er ikke lenger i bruk hos iSyfo, men spesialist har historiske meldinger med denne årsaken
        BESTRIDELSE_SYKMELDING,
    }
}
