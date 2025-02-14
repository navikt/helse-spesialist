package no.nav.helse.modell.vedtak

data class VedtakBegrunnelse(
    val utfall: Utfall,
    val begrunnelse: String?,
) {
    internal fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.vedtakBegrunnelseData(this)
    }
}

enum class Utfall {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
}
