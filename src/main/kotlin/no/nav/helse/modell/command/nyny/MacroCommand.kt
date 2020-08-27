package no.nav.helse.modell.command.nyny

internal abstract class MacroCommand : Command {
    private var currentIndex: Int = 0
    private val historikk: MutableList<Command> = mutableListOf()
    protected abstract val commands: List<Command>

    internal fun restore(currentIndex: Int) {
        historikk.clear()
        this.currentIndex = currentIndex
        for (i in 0..currentIndex) historikk.add(0, commands[i])
    }

    final override fun execute(context: CommandContext): Boolean {
        require(commands.isNotEmpty())
        context.register(this)
        return run(context, commands)
    }

    final override fun resume(context: CommandContext): Boolean {
        context.register(this)
        if (!runCommand(context, commands[currentIndex], Command::resume)) return false
        return run(context, commands.subList(currentIndex, commands.size))
    }

    final override fun undo(context: CommandContext) {
        context.register(this)
        historikk.forEach { it.undo(context) }
        context.clear()
    }

    private fun run(context: CommandContext, commands: List<Command>): Boolean {
        return commands.none { historikk.add(0, it); !runCommand(context, it, Command::execute) }
    }

    private fun runCommand(context: CommandContext, command: Command, commandAction: Command.(CommandContext) -> Boolean): Boolean {
        if (!commandAction(command, context)) return false.also { context.add(currentIndex) }
        currentIndex += 1
        return true
    }
}
