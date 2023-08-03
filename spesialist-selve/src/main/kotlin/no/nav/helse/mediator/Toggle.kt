package no.nav.helse.mediator

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object Inntekter : Toggle("INNTEKTER", true)

    object BeholdRevurderingerMedVergemålEllerUtland : Toggle("BEHOLD_REVURDERINGER_MED_VERGEMÅL_ELLER_UTLAND", false)
}
