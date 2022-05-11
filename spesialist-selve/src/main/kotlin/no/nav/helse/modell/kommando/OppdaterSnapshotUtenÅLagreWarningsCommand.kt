package no.nav.helse.modell.kommando

import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.SnapshotDao
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotUtenÅLagreWarningsCommand(
    private val fødselsnummer: String,
    private val snapshotClient: SnapshotClient,
    private val snapshotDao: SnapshotDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotUtenÅLagreWarningsCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return oppdaterSnapshot()
    }

    private fun oppdaterSnapshot(): Boolean {
        return snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer, person)
            log.info("Oppdaterte snapshot på person: ${fødselsnummer.substring(0, 4)}*******")
            true
        } ?: false
    }
}
