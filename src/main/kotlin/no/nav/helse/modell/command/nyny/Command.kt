package no.nav.helse.modell.command.nyny

internal abstract class Command {
    internal abstract fun execute(context: CommandContext) : Boolean
    internal open fun resume(context: CommandContext) : Boolean { return true }
    internal open fun undo(context: CommandContext) {}
}
