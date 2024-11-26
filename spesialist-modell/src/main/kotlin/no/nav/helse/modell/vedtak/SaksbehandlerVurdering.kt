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
            Vurdering.AVSLAG -> SaksbehandlerVurderingDto(VurderingDto.AVSLAG, begrunnelse)
            Vurdering.DELVIS_INNVILGELSE -> SaksbehandlerVurderingDto(VurderingDto.DELVIS_INNVILGELSE, begrunnelse)
            Vurdering.INNVILGELSE -> SaksbehandlerVurderingDto(VurderingDto.INNVILGELSE, begrunnelse)
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
