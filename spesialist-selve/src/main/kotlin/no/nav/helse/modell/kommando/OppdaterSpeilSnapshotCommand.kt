package no.nav.helse.modell.kommando

internal class OppdaterSpeilSnapshotCommand : Command {

    override fun execute(context: CommandContext): Boolean {
        return true
    }
}
