package no.nav.helse.modell.kommando

import no.nav.helse.modell.SnapshotDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.slf4j.LoggerFactory

internal class SørgForAtSnapshotErOppdatertCommand(
    private val snapshotDao: SnapshotDao,
    private val snapshotClient: SnapshotClient,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        if (snapshotDao.utdatert(fødselsnummer)) {
            sikkerlogger.debug("Snapshot for $fødselsnummer er på utdatert versjon, henter nytt")
            snapshotClient.hentSnapshot(fødselsnummer).data?.person?.let {
                snapshotDao.lagre(fødselsnummer, it)
            }
        }
        return true
    }

}
