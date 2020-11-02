package no.nav.helse.modell.kommando

import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotV2Command(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val vedtakDao: VedtakDao,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotV2Command::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return oppdaterSnapshot()
    }

    private fun oppdaterSnapshot(): Boolean {
        val snapshot = speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer)
        vedtakDao.oppdaterSnapshot(fødselsnummer, snapshot)
        log.info("Oppdaterte snapshot på person: ${fødselsnummer.substring(4)}*******")
        return true
    }
}
