package no.nav.helse.modell

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    val enabled get() = _enabled

    object Skjonnsfastsetting : Toggle("SKJONNSFASTSETTING", true)
    object AutomatiserSpesialsak : Toggle("AUTOMATISER_SPESIALSAK", true)
    object FellesPaVentBenk : Toggle("FELLES_PÅ_VENT_BENK", "dev-gcp" == System.getenv("NAIS_CLUSTER_NAME"))
}
