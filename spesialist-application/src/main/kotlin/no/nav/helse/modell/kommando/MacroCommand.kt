package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg

abstract class MacroCommand : Command {
    private var currentIndex: Int = 0
    private val historikk: MutableList<Command> = mutableListOf()
    protected abstract val commands: List<Command>

    internal fun restore(currentIndex: Int) {
        historikk.clear()
        this.currentIndex = currentIndex
        for (i in 0..currentIndex) historikk.add(0, commands[i])
    }

    final override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        require(commands.isNotEmpty())
        logg.info("Utfører makro-kommando ${this::class.simpleName}")
        context.register(this)
        return run(context, sessionContext, outbox, commands)
    }

    final override fun resume(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        logg.info("Gjenopptar ${this::class.simpleName}, sti: ${context.sti()}")
        context.register(this)
        if (!runCommand(context, sessionContext, outbox, commands[currentIndex], Command::resume)) return false
        return run(context, sessionContext, outbox, commands.subList(currentIndex, commands.size))
    }

    final override fun hash(): String = name + commands.joinToString { it.hash() }

    private fun run(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
        commands: List<Command>,
    ): Boolean =
        CommandContext.run(context, commands) {
            historikk.add(0, it)
            runCommand(context, sessionContext, outbox, it, Command::execute)
        }

    private fun runCommand(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
        command: Command,
        commandAction: Command.(CommandContext, SessionContext, Outbox) -> Boolean,
    ): Boolean {
        if (!commandAction(command, context, sessionContext, outbox)) {
            context.suspendert(currentIndex)
            logg.info("Kommando ${command::class.simpleName} suspenderte, nåværende sti: ${context.sti()}")
            return false
        }
        logg.info("Kommando ${command.name} er ferdig")
        currentIndex += 1
        return true
    }
}
