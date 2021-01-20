package no.nav.helse.modell.kommando

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import org.slf4j.LoggerFactory
import java.util.*

internal class OppdaterSnapshotCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao,
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

    override fun resume(context: CommandContext): Boolean {
        return oppdaterSnapshot()
    }

    override fun undo(context: CommandContext) {
        // sletting av snapshot gir ikke mening i denne kontekst
    }

    private fun ignorer(): Boolean {
        log.info("kjenner ikke til vedtaksperiode $vedtaksperiodeId")
        return true
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer).let { snapshot ->
            val snapshotBleOppdatert = snapshotDao.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, snapshot) != 0
            if (snapshotBleOppdatert) {
                log.info("oppdaterer warnings for $vedtaksperiodeId")
                warningDao.oppdaterSpleisWarnings(vedtaksperiodeId, Warning.warnings(
                    vedtaksperiodeId,
                    objectMapper.readValue(snapshot)
                ))
            }
            snapshotBleOppdatert
        }
    }
}
