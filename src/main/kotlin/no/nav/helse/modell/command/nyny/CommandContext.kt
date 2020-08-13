package no.nav.helse.modell.command.nyny

internal class CommandContext {
    private val data = mutableListOf<Any>()

    internal fun add(data: Any) {
        this.data.add(data)
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()
}
