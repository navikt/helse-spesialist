package no.nav.helse.modell.command.nyny

internal abstract class MacroCommand : Command() {
    private var currentIndex: Int = 0
    private val historikk: MutableList<Command> = mutableListOf()
    protected abstract val commands: List<Command>

    internal fun restore(currentIndex: Int) {
        historikk.clear()
        this.currentIndex = currentIndex
        for (i in 0..currentIndex) historikk.add(0, commands[i])
    }

    final override fun execute(kontekst: CommandContext): Boolean {
        require(commands.isNotEmpty())
        kontekst.register(this)
        return run(kontekst, commands)
    }

    final override fun resume(kontekst: CommandContext): Boolean {
        kontekst.register(this)
        if (!runCommand(kontekst, commands[currentIndex], Command::resume)) return false
        return run(kontekst, commands.subList(currentIndex, commands.size))
    }

    final override fun undo(kontekst: CommandContext) {
        kontekst.register(this)
        historikk.forEach { it.undo(kontekst) }
        kontekst.clear()
    }

    private fun run(kontekst: CommandContext, commands: List<Command>): Boolean {
        return commands.none { historikk.add(0, it); !runCommand(kontekst, it, Command::execute) }
    }

    private fun runCommand(kontekst: CommandContext, command: Command, commandAction: Command.(CommandContext) -> Boolean): Boolean {
        if (!commandAction(command, kontekst)) return false.also { kontekst.add(currentIndex, command) }
        currentIndex += 1
        return true
    }
}
