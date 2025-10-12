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

@Resource("personer")
class Personer {
    @Resource("{aktørId}")
    class AktørId(
        val parent: Personer = Personer(),
        val aktørId: String,
    ) {
        @Resource("tilkomne-inntektskilder")
        class TilkomneInntektskilder(
            val parent: AktørId,
        )
    }
}
