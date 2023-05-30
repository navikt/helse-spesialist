package no.nav.helse.spesialist.api

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object BehandleEnOgEnPeriode : Toggle("BEHANDLE_EN_OG_EN_PERIODE", erDev())
}

private fun erDev() = "dev-gcp" == System.getenv("NAIS_CLUSTER_NAME")
