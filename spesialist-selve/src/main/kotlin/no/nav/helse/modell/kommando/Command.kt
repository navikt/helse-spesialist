package no.nav.helse.modell.kommando

internal interface Command {
    fun execute(context: CommandContext) : Boolean
    fun resume(context: CommandContext) = true
    fun undo(context: CommandContext) {}
}

internal fun ikkesuspenderendeCommand(block: () -> Unit) = object : Command {
    override fun execute(context: CommandContext): Boolean {
        block()
        return true
    }
}
