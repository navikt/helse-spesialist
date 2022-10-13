package no.nav.helse.spesialist.api

internal object ToggleHelpers {

    internal fun Toggle.enable() = setPrivateValue(true)

    private fun Toggle.setPrivateValue(value: Boolean) {
        this.javaClass.superclass.getDeclaredField("_enabled").let { field ->
            field.isAccessible = true
            field.set(this, value)
        }
    }
}
