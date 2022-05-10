package no.nav.helse.modell.kommando

import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.helse.modell.VedtakDao

internal class OppdaterSnapshotCommand(
    private val snapshotClient: SnapshotClient,
    private val snapshotDao: SnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val warningDao: WarningDao,
    private val vedtakDao: VedtakDao,
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (null == vedtakDao.finnVedtakId(vedtaksperiodeId)) return ignorer()
        return oppdaterSnapshot()
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return snapshotClient.hentSnapshot(fnr = fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            log.info("oppdaterer warnings fra graphql-snapshot for $vedtaksperiodeId")
            warningDao.oppdaterSpleisWarnings(vedtaksperiodeId, Warning.graphQLWarnings(vedtaksperiodeId, person))
            true
        } ?: false
    }

    private fun ignorer(): Boolean {
        log.info("kjenner ikke til vedtaksperiode $vedtaksperiodeId")
        return true
    }
}
