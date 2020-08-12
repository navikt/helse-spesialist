package no.nav.helse.modell.command.nyny

internal abstract class MacroCommand : Command() {
    private var indices: MutableList<Int> = mutableListOf()

    private var currentIndex: Int = 0
    protected abstract val commands: List<Command>

    internal fun state() = indices.reversed().toList()

    private fun restore(state: MutableList<Int>) {
        currentIndex = state.removeAt(0)
        commands.listIterator(currentIndex).forEach { if (it is MacroCommand) it.restore(state) } }

    internal fun restore(state: List<Int>) {
        restore(state.toMutableList())
    }

    final override fun resume(): Boolean {
        indices.clear()
        if (!runCommand(commands[currentIndex], Command::resume)) return false
        return run(commands.subList(currentIndex, commands.size))
    }

    final override fun execute(): Boolean {
        require(commands.isNotEmpty())
        indices.clear()
        return run(commands)
    }

    private fun run(commands: List<Command>): Boolean {
        return commands.none { !runCommand(it, Command::execute) }
    }

    private fun runCommand(command: Command, commandAction: Command.() -> Boolean): Boolean {
        if(command is MacroCommand) command.indices = indices
        if (!commandAction(command)) return false.also { indices.add(currentIndex) }
        currentIndex += 1
        return true
    }
}
