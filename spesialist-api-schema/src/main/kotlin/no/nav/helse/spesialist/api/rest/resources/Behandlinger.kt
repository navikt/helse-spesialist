@file:kotlinx.serialization.UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.util.UUID

@Resource("behandlinger")
class Behandlinger {
    @Resource("{behandlingId}")
    class BehandlingId(
        val parent: Behandlinger = Behandlinger(),
        val behandlingId: UUID,
    ) {
        @Resource("notater")
        class Notater(
            val parent: BehandlingId,
        )
    }
}
