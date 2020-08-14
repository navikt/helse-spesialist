package no.nav.helse.modell.command.nyny

internal class CommandContext {
    private val data = mutableListOf<Any>()
    private val tilstand: MutableList<Int> = mutableListOf()

    internal fun add(data: Any) {
        this.data.add(data)
    }

    internal fun add(index: Int, command: Command) {
        tilstand.add(0, index)
    }

    internal fun clear() {
        tilstand.clear()
    }

    internal fun register(command: MacroCommand) {
        if(tilstand.isEmpty()) return
        command.restore(tilstand.removeAt(0))
    }

    internal fun tilstand() = tilstand.toList()

    internal fun tilstand(tilstand: List<Int>) {
        this.tilstand.apply { clear(); addAll(tilstand) }
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()
}
