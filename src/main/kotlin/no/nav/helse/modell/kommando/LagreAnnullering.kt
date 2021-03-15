package no.nav.helse.modell.kommando

import org.slf4j.LoggerFactory

internal class LagreAnnullering() : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotUtenÅLagreWarningsCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        TODO("Not yet implemented")
    }
}
