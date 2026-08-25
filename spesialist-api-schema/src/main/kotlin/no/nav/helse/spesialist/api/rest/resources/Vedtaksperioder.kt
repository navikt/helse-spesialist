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

        @Resource("anmod-om-forkasting")
        class AnmodOmForkasting(
            val parent: VedtaksperiodeId,
        )

        @Resource("overstyringer")
        class Overstyringer(
            val parent: VedtaksperiodeId,
        ) {
            @Resource("tidslinje")
            class Tidslinje(
                val parent: Overstyringer,
            )

            @Resource("inntekt-og-refusjon")
            class InntektOgRefusjon(
                val parent: Overstyringer,
            )

            @Resource("arbeidsforhold")
            class Arbeidsforhold(
                val parent: Overstyringer,
            )
        }
    }
}
