package no.nav.helse.modell.command.nyny

internal abstract class MacroCommand : Command() {
    private val indices: MutableList<Int> = mutableListOf()
    private var currentIndex: Int = 0
    protected abstract val commands: List<Command>

    private fun execute(indices: MutableList<Int>): Boolean {
        commands.listIterator(currentIndex).forEach {
            val executeOk = if (it is MacroCommand) it.execute(indices) else it.execute()
            if(!executeOk) return false.also { indices.add(currentIndex) }
            currentIndex += 1
        }
        return true
    }

    internal fun state() = indices.reversed().toList()

    final override fun execute() : Boolean {
        require(commands.isNotEmpty())
        indices.clear()
        return execute(this.indices)
    }
}
