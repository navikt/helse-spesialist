package no.nav.helse.modell.command.nyny

internal abstract class Command {
    internal abstract fun execute() : Boolean
    internal abstract fun resume() : Boolean
    internal abstract fun undo()
}
