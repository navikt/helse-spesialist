package no.nav.helse.modell.command.nyny

internal abstract class MacroCommand : Command() {
    private var currentIndex: Int = 0
    protected abstract val commands: List<Command>

    final override fun execute() : Boolean {
        require(commands.isNotEmpty())
        commands.listIterator(currentIndex).forEach {
            if(!it.execute()) return false
            currentIndex += 1
        }
        return true
    }
}
