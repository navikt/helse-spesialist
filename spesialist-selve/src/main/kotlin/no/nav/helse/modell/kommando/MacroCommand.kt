package no.nav.helse.modell.kommando

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
        logg.info("Utfører makro-kommando ${this::class.simpleName}")
        context.register(this)
        return run(context, commands)
    }

    final override fun resume(context: CommandContext): Boolean {
        logg.info("Gjenopptar ${this::class.simpleName}, sti: ${context.sti()}")
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
        return CommandContext.run(context, commands) {
            historikk.add(0, it)
            runCommand(context, it, Command::execute)
        }
    }

    private fun runCommand(context: CommandContext, command: Command, commandAction: Command.(CommandContext) -> Boolean): Boolean {
        if (!commandAction(command, context)) {
            context.suspendert(currentIndex)
            logg.info("Kommando ${command::class.simpleName} suspenderte, nåværende sti: ${context.sti()}")
            return false
        }
        logg.info("Kommando ${command::class.simpleName} er ferdig")
        currentIndex += 1
        return true
    }
}
