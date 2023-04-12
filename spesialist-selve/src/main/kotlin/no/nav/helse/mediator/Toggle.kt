package no.nav.helse.mediator

import no.nav.helse.mediator.api.erDev

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object Inntekter : Toggle("INNTEKTER", true)
    object AutomatiserRevuderinger : Toggle("AUTOMATISER_REVURDERINGER", erDev())
    object Totrinnsvurdering : Toggle("TOTRINNSVURDERING", true)
    object AutomatiserUtbetalingTilSykmeldt : Toggle("AUTOMATISER_UTS", true)
    object HentDataFraMsGraph : Toggle("HENT_DATA_FRA_MS_GRAPH", erDev())
}
