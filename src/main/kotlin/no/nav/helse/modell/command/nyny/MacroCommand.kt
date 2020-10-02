package no.nav.helse.modell.command.nyny

import org.slf4j.LoggerFactory

internal abstract class MacroCommand : Command {
    private var currentIndex: Int = 0
    private val historikk: MutableList<Command> = mutableListOf()
    protected abstract val commands: List<Command>

    private companion object {
        private val logg = LoggerFactory.getLogger(MacroCommand::class.java)
    }

    internal fun restore(currentIndex: Int) {
        historikk.clear()
        this.currentIndex = currentIndex
        for (i in 0..currentIndex) historikk.add(0, commands[i])
    }

    final override fun execute(context: CommandContext): Boolean {
        require(commands.isNotEmpty())
        logg.info("Utfører ${this::class.simpleName}")
        context.register(this)
        return run(context, commands)
    }

    final override fun resume(context: CommandContext): Boolean {
        logg.info("Gjenopptar ${this::class.simpleName}")
        context.register(this)
        if (!runCommand(context, commands[currentIndex], Command::resume)) return false
        return run(context, commands.subList(currentIndex, commands.size))
    }

    final override fun undo(context: CommandContext) {
        logg.info("Reverserer utførelse av ${this::class.simpleName}")
        context.register(this)
        historikk.forEach { it.undo(context) }
        context.clear()
    }

    private fun run(context: CommandContext, commands: List<Command>): Boolean {
        return commands.none { historikk.add(0, it); !runCommand(context, it, Command::execute) }
    }

    private fun runCommand(context: CommandContext, command: Command, commandAction: Command.(CommandContext) -> Boolean): Boolean {
        logg.info("Utfører ${command::class.simpleName}")
        if (!commandAction(command, context)) return false.also { context.suspendert(currentIndex) }
        currentIndex += 1
        return true
    }
}
