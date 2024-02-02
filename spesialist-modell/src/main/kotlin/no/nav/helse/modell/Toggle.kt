package no.nav.helse.modell

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    val enabled get() = _enabled

    object Skjonnsfastsetting : Toggle("SKJONNSFASTSETTING", true)
    object AutomatiserSpesialsak : Toggle("AUTOMATISER_SPESIALSAK", true)
    object Avviksvurdering : Toggle("AVVIKSVURDERING", false)
    object RestartKommandokjede : Toggle("RESTART_KOMMANDOKJEDE", false)
}
