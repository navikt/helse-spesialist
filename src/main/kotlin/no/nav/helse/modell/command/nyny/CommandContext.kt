package no.nav.helse.modell.command.nyny

import java.util.*

internal class CommandContext(internal val id: UUID = UUID.randomUUID()) {
    private val data = mutableListOf<Any>()
    private val behov = mutableMapOf<String, Map<String, Any>>()
    private val tilstand: MutableList<Int> = mutableListOf()

    internal fun behov(behovtype: String, params: Map<String, Any> = emptyMap()) {
        this.behov[behovtype] = params
    }

    internal fun behov(packet: Map<String, Any>) =
        if (!harBehov()) packet
        else packet.toMutableMap().apply {
            this["contextId"] = id
            this["behov"] = behov.keys.toList()
            behov.forEach { (behovtype, params) ->
                if (params.isNotEmpty()) {
                    this[behovtype] = params
                }
            }
        }

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
        if (tilstand.isEmpty()) return
        command.restore(tilstand.removeAt(0))
    }

    internal fun harBehov() = behov.isNotEmpty()

    internal fun tilstand() = tilstand.toList()

    internal fun tilstand(tilstand: List<Int>) {
        this.tilstand.apply { clear(); addAll(tilstand) }
    }

    internal fun run(command: Command) = when {
        tilstand.isEmpty() -> command.execute(this)
        else -> command.resume(this)
    }

    internal inline fun <reified T> get(): T? = data.filterIsInstance<T>().firstOrNull()
}
