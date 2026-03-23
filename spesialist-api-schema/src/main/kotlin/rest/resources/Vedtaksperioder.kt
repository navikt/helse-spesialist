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
        )

        @Resource("annuller")
        class Annuller(
            val parent: VedtaksperiodeId,
        )
    }
}
