@file:kotlinx.serialization.UseContextualSerialization(
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.util.UUID

@Resource("vedtaksperioder")
class Vedtaksperioder {
    @Resource("{vedtaksperiodeId}")
    class VedtaksperiodeId(
        val parent: Vedtaksperioder = Vedtaksperioder(),
        val vedtaksperiodeId: UUID,
    ) {
        @Resource("notater")
        class Notater(
            val parent: VedtaksperiodeId,
        ) {
            @Resource("{notatId}")
            class NotatId(
                val parent: Notater,
                val notatId: Int,
            ) {
                @Resource("feilregistrer")
                class Feilregistrer(
                    val parent: NotatId,
                )

                @Resource("kommentarer")
                class Kommentarer(
                    val parent: NotatId,
                )
            }
        }

        @Resource("annuller")
        class Annuller(
            val parent: VedtaksperiodeId,
        )
    }
}
