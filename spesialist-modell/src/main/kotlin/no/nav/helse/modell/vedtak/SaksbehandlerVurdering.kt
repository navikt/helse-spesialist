package no.nav.helse.modell.vedtak

import no.nav.helse.modell.vedtak.SaksbehandlerVurderingDto.VurderingDto

data class SaksbehandlerVurdering(
    private val vurdering: Vurdering,
    val begrunnelse: String?,
) {
    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.saksbehandlerVurderingData(this)
    }

    fun toDto() =
        when (vurdering) {
            Vurdering.AVSLAG -> SaksbehandlerVurderingDto.Avslag(begrunnelse)
            Vurdering.DELVIS_INNVILGELSE -> SaksbehandlerVurderingDto.DelvisInnvilgelse(begrunnelse)
            Vurdering.INNVILGELSE -> SaksbehandlerVurderingDto.Innvilgelse(begrunnelse)
        }

    companion object {
        fun Innvilgelse(innvilgelsesbegrunnelse: String? = null) = SaksbehandlerVurdering(Vurdering.INNVILGELSE, innvilgelsesbegrunnelse)

        fun Avslag(avslagsbegrunnelse: String?) = SaksbehandlerVurdering(Vurdering.AVSLAG, avslagsbegrunnelse)

        fun DelvisInnvilgelse(delvisInnvilgelsebegrunnelse: String?) =
            SaksbehandlerVurdering(Vurdering.DELVIS_INNVILGELSE, delvisInnvilgelsebegrunnelse)
    }
}

enum class Vurdering {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
    ;

    internal fun toDto() =
        when (this) {
            AVSLAG -> VurderingDto.AVSLAG
            DELVIS_INNVILGELSE -> VurderingDto.DELVIS_INNVILGELSE
            INNVILGELSE -> VurderingDto.INNVILGELSE
        }
}
