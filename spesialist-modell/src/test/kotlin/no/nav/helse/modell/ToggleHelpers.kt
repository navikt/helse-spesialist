package no.nav.helse.modell

internal object ToggleHelpers {

    internal fun Toggle.enable() = setPrivateValue(true)

    internal fun Toggle.disable() = setPrivateValue(false)

    private fun Toggle.setPrivateValue(value: Boolean) {
        this.javaClass.superclass.getDeclaredField("_enabled").let { field ->
            field.isAccessible = true
            field.set(this, value)
        }
    }
}