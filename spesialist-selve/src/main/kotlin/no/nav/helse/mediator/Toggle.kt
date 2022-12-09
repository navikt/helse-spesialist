package no.nav.helse.mediator

import no.nav.helse.mediator.api.erDev

abstract class Toggle(private var _enabled: Boolean) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    internal val enabled get() = _enabled

    object VedtaksperiodeGenerasjoner: Toggle("VEDTAKSPERIODE_GENERASJONER")
    object Inntekter: Toggle("INNTEKTER", true)

    object Refusjonsendringer : Toggle("REFUSJONSENDRINGER", erDev())

    object AutomatiserRevuderinger : Toggle("AUTOMATISER_REVURDERINGER", erDev())
}
