@file:kotlinx.serialization.UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.util.UUID

@Resource("notater")
class Notater {
    @Resource("{notatId}")
    class NotatId(
        val parent: Notater,
        val notatId: Int,
    )
}
