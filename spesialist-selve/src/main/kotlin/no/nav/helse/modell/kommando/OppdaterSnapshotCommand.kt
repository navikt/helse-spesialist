package no.nav.helse.modell.kommando

import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning
import org.slf4j.LoggerFactory
import java.util.*

internal class OppdaterSnapshotCommand(
    private val speilSnapshotGraphQLClient: SpeilSnapshotGraphQLClient,
    private val snapshotDao: SnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val warningDao: WarningDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
    }

    override fun execute(context: CommandContext) = oppdaterSnapshot()

    private fun oppdaterSnapshot(): Boolean {
        if (!Toggle.GraphQLApi.enabled) return true
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return speilSnapshotGraphQLClient.hentSnapshot(fnr = fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            log.info("oppdaterer warnings fra graphql-snapshot for $vedtaksperiodeId")
            warningDao.oppdaterSpleisWarnings(vedtaksperiodeId, Warning.graphQLWarnings(vedtaksperiodeId, person))
            true
        } ?: false
    }
}
