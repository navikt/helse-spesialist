package no.nav.helse.modell

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    val enabled get() = _enabled

    object AutomatiserSpesialsak : Toggle("AUTOMATISER_SPESIALSAK", true)
    object LeggTilAvviksVarselPåNytt : Toggle("LEGG_TIL_AVVIKSVARSEL_PA_NYTT", false)
}
