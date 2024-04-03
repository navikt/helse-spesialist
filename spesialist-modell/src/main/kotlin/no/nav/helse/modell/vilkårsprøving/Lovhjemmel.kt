package no.nav.helse.modell.vilkårsprøving

data class Lovhjemmel(
    private val paragraf: String,
    private val ledd: String? = null,
    private val bokstav: String? = null,
    private val lovverk: String,
    private val lovverksversjon: String,
) {
    fun byggEvent() =
        LovhjemmelEvent(
            paragraf = paragraf,
            ledd = ledd,
            bokstav = bokstav,
            lovverk = lovverk,
            lovverksversjon = lovverksversjon,
        )

    fun toDto() =
        LovhjemmelDto(
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

data class LovhjemmelDto(
    val paragraf: String,
    val ledd: String?,
    val bokstav: String?,
    val lovverk: String?,
    val lovverksversjon: String?,
)
