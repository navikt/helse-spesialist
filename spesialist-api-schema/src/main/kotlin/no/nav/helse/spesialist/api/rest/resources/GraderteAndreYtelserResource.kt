@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Resource("graderte-andre-ytelser")
class GraderteAndreYtelserResource {
    @Resource("{graderteAndreYtelserId}")
    class Id(
        val parent: GraderteAndreYtelserResource = GraderteAndreYtelserResource(),
        val graderteAndreYtelserId: UUID,
    ) {
        @Resource("fjern")
        class Fjern(
            val parent: Id,
        )

        @Resource("gjenopprett")
        class Gjenopprett(
            val parent: Id,
        )
    }
}
