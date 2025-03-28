package no.nav.helse.modell.kommando

internal class OverstyringIgangsattCommand : Command {
    override fun execute(context: CommandContext): Boolean {
        return true
    }
}
