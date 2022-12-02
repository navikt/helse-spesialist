package no.nav.helse.mediator

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object BeholdForlengelseMedOvergangTilUTS: Toggle("BEHOLD_FORELENGELSER_TIL_UTS")
    object VedtaksperiodeGenerasjoner: Toggle("VEDTAKSPERIODE_GENERASJONER")
    object PersonavstemmingForHistoriskeGenerasjoner: Toggle("PERSONAVSTEMMING_FOR_HISTORISKE_GENERASJONER")

    object Inntekter: Toggle("INNTEKTER", true)
}
