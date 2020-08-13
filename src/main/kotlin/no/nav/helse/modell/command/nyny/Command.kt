package no.nav.helse.modell.command.nyny

internal abstract class Command {
    internal abstract fun execute(context: CommandContext) : Boolean
    internal abstract fun resume(context: CommandContext) : Boolean
    internal abstract fun undo()
}
