package no.nav.helse.modell.kommando

internal interface Command {
    fun execute(context: CommandContext): Boolean

    fun resume(context: CommandContext) = true

    fun undo(context: CommandContext) {}

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
