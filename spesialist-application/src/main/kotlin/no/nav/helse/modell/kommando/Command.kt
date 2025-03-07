package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.domain.Aktivitetslogg

abstract class Command {
    abstract fun execute(context: CommandContext): Boolean

    open fun resume(context: CommandContext) = true

    open fun hash(): String = name

    open val name: String = this::class.java.simpleName

    val aktivitetslogg: Aktivitetslogg by lazy { Aktivitetslogg(name) }
}

internal fun ikkesuspenderendeCommand(
    navnForLogging: String = "<navn ikke oppgitt>",
    block: () -> Unit,
) = object : Command() {
    override fun execute(context: CommandContext): Boolean {
        block()
        return true
    }

    override val name = navnForLogging
}
