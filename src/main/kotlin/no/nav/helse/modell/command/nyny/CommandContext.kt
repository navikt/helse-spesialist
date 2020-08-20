package no.nav.helse.modell.command.nyny

import java.util.*

internal class CommandContext(internal val id: UUID = UUID.randomUUID()) {
    private val data = mutableListOf<Any>()
    private val behov = mutableMapOf<String, Map<String, Any>>()
    private val sti: MutableList<Int> = mutableListOf()

    internal fun behov(behovtype: String, params: Map<String, Any> = emptyMap()) {
        this.behov[behovtype] = params
    }

    internal fun behov() = behov.toMap()

    internal fun add(data: Any) {
        this.data.add(data)
    }

    internal fun add(index: Int, command: Command) {
        sti.add(0, index)
    }

    internal fun clear() {
        sti.clear()
    }

    internal fun register(command: MacroCommand) {
        if (sti.isEmpty()) return
        command.restore(sti.removeAt(0))
    }

    internal fun harBehov() = behov.isNotEmpty()

    internal fun sti() = sti.toList()

    internal fun sti(sti: List<Int>) {
        this.sti.apply { clear(); addAll(sti) }
    }

    internal fun run(command: Command) = when {
        sti.isEmpty() -> command.execute(this)
        else -> command.resume(this)
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()
}
