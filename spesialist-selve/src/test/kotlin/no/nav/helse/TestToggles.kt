import no.nav.helse.mediator.Toggle

internal object TestToggles {

    internal fun Toggle.enable() {
        _enabled = true
    }

    internal fun Toggle.disable() {
        _enabled = false
    }

}
