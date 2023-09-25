package no.nav.helse.mediator

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object Skjonnsfastsetting : Toggle("SKJONNSFASTSETTING", true)
    object TilgangsstyrteEgenskaper : Toggle("TILGANGSSTYRTEEGENSKAPER", false)
    object EgenAnsatt : Toggle("EGENANSATT", false)
}
