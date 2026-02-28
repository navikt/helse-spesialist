@file:kotlinx.serialization.UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.util.UUID

@Resource("dialoger")
class Dialoger {
    @Resource("{dialogId}")
    class DialogId(
        val parent: Dialoger,
        val dialogId: Long,
    ) {
        @Resource("kommentarer")
        class Kommentar(
            val parent: DialogId,
        ) {
            @Resource("{kommentarId}")
            class KommentarId(
                val parent: Kommentar,
                val kommentarId: Int,
            )
        }
    }
}
