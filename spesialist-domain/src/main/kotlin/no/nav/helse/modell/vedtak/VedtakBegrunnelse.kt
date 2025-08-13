package no.nav.helse.modell.vedtak

data class VedtakBegrunnelse(
    val utfall: Utfall,
    val begrunnelse: String?,
)

enum class Utfall {
    AVSLAG,
    DELVIS_INNVILGELSE,
    INNVILGELSE,
}
