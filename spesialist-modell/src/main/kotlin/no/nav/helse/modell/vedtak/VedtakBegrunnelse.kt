package no.nav.helse.modell.vedtak

import no.nav.helse.modell.vedtak.VedtakBegrunnelseDto.UtfallDto

data class VedtakBegrunnelse(
    private val utfall: Utfall,
    val begrunnelse: String?,
) {
    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.vedtakBegrunnelseData(this)
    }

    fun toDto() = VedtakBegrunnelseDto(utfall.toDto(), begrunnelse)
}

enum class Utfall {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
    ;

    internal fun toDto() =
        when (this) {
            AVSLAG -> UtfallDto.AVSLAG
            DELVIS_INNVILGELSE -> UtfallDto.DELVIS_INNVILGELSE
            INNVILGELSE -> UtfallDto.INNVILGELSE
        }
}
