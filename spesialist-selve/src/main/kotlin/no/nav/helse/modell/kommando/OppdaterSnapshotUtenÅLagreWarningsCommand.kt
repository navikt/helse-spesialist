package no.nav.helse.modell.kommando

import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotUtenÅLagreWarningsCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val snapshotDao: SnapshotDao,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotUtenÅLagreWarningsCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return oppdaterSnapshot()
    }

    private fun oppdaterSnapshot(): Boolean {
        val snapshot = speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer)
        snapshotDao.lagre(fødselsnummer, snapshot)
        log.info("Oppdaterte snapshot på person: ${fødselsnummer.substring(0, 4)}*******")
        return true
    }
}
