package no.nav.helse.modell.kommando

interface Command {
    fun execute(context: CommandContext): Boolean

    fun resume(context: CommandContext) = true

    fun hash(): String = name

    val name: String get() = this::class.java.simpleName
}

internal fun ikkesuspenderendeCommand(
    navnForLogging: String = "<navn ikke oppgitt>",
    block: () -> Unit,
) = object : Command {
    override fun execute(context: CommandContext): Boolean {
        block()
        return true
    }

    override val name = navnForLogging
}
