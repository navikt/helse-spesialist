package no.nav.helse.modell.command.nyny

internal abstract class MacroCommand : Command() {
    private val indices: MutableList<Int> = mutableListOf()
    private var currentIndex: Int = 0
    protected abstract val commands: List<Command>

    internal fun state() = indices.reversed().toList()

    final override fun resume(): Boolean {
        indices.clear()
        return resume(indices)
    }

    final override fun execute() : Boolean {
        require(commands.isNotEmpty())
        indices.clear()
        return execute(indices)
    }

    private fun execute(indices: MutableList<Int>): Boolean {
        commands.listIterator(currentIndex).forEach {
            if(!it.run(indices, MacroCommand::execute, Command::execute)) return false
        }
        return true
    }

    private fun resume(indices: MutableList<Int>): Boolean {
        commands[currentIndex].also {
            if(!it.run(indices, MacroCommand::resume, Command::resume)) return false
        }
        return execute(indices)
    }

    private fun Command.run(
        indices: MutableList<Int>,
        macroCommandAction: MacroCommand.(indices: MutableList<Int>) -> Boolean,
        commandAction: Command.() -> Boolean
    ): Boolean {
        val executeOk = if (this is MacroCommand) this.macroCommandAction(indices) else this.commandAction()
        if(!executeOk) return false.also { indices.add(currentIndex) }
        currentIndex += 1
        return true
    }
}
