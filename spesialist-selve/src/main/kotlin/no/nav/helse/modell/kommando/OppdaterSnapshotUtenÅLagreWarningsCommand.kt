package no.nav.helse.modell.kommando

import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.SpeilSnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotUtenÅLagreWarningsCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val speilSnapshotDao: SpeilSnapshotDao,
    private val fødselsnummer: String,
    private val speilSnapshotGraphQLClient: SpeilSnapshotGraphQLClient,
    private val snapshotDao: SnapshotDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotUtenÅLagreWarningsCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        return oppdaterRestSnapshot() && oppdaterGraphQLSnapshot()
    }

    private fun oppdaterRestSnapshot(): Boolean {
        val snapshot = speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer)
        speilSnapshotDao.lagre(fødselsnummer, snapshot)
        log.info("Oppdaterte snapshot på person: ${fødselsnummer.substring(0, 4)}*******")
        return true
    }

    private fun oppdaterGraphQLSnapshot(): Boolean {
        return speilSnapshotGraphQLClient.hentSnapshot(fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer, person)
            log.info("Oppdaterte graphql-snapshot på person: ${fødselsnummer.substring(0, 4)}*******")
            true
        } ?: false
    }
}
