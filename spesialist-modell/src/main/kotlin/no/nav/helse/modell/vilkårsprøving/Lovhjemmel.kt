package no.nav.helse.modell.vilkårsprøving

data class Lovhjemmel(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
    val lovverk: String,
    val lovverksversjon: String,
) {
    fun byggEvent() =
        LovhjemmelEvent(
            paragraf = paragraf,
            ledd = ledd,
            bokstav = bokstav,
            lovverk = lovverk,
            lovverksversjon = lovverksversjon,
        )
}

data class LovhjemmelEvent(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
    val lovverk: String,
    val lovverksversjon: String,
)
