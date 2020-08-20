package no.nav.helse.modell.command.nyny

internal interface Command {
    fun execute(context: CommandContext) : Boolean
    fun resume(context: CommandContext) = true
    fun undo(context: CommandContext) {}
}
