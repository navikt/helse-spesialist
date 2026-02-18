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
        @Resource("vedtak")
        class Vedtak(
            val parent: BehandlingId,
        )

        @Resource("forkasting")
        class Forkasting(
            val parent: BehandlingId,
        )

        @Resource("forsikring")
        class Forsikring(
            val parent: BehandlingId,
        )
    }
}
