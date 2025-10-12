@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Resource("tilkomne-inntekter")
class TilkomneInntekter {
    @Resource("{tilkommenInntektId}")
    class Id(
        val parent: TilkomneInntekter = TilkomneInntekter(),
        val tilkommenInntektId: UUID,
    ) {
        @Resource("endre")
        class Endre(
            val parent: Id,
        )

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
