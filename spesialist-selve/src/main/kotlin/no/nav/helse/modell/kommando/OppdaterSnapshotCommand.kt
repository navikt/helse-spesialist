package no.nav.helse.modell.kommando

import no.nav.helse.mediator.api.graphql.SpleisGraphQLClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import org.slf4j.LoggerFactory
import java.util.*

internal class OppdaterSnapshotCommand(
    private val spleisGraphQLClient: SpleisGraphQLClient,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (null == vedtakDao.finnVedtakId(vedtaksperiodeId)) return ignorer()
        return oppdaterSnapshot()
    }

    private fun ignorer(): Boolean {
        log.info("kjenner ikke til vedtaksperiode $vedtaksperiodeId")
        return true
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return spleisGraphQLClient.hentSnapshot(fnr = fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            true
        } ?: false
    }
}
