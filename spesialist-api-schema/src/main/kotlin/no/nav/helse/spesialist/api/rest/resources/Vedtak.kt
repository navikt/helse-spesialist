@file:kotlinx.serialization.UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.util.UUID

@Resource("vedtak")
class Vedtak {
    @Resource("{behandlingId}")
    class Id(
        val parent: Vedtak = Vedtak(),
        val behandlingId: UUID,
    ) {
        @Resource("fatt")
        class Fatt(
            val parent: Id,
        )
    }
}
