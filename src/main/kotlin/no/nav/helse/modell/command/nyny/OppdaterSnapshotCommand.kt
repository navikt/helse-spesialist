package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import java.util.*

internal class OppdaterSnapshotCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String) : Command() {

    override fun execute(context: CommandContext): Boolean {
        if (null == vedtakDao.findVedtak(vedtaksperiodeId)) return true
        return oppdaterSnapshot()
    }

    override fun resume(context: CommandContext): Boolean {
        return oppdaterSnapshot()
    }

    override fun undo(context: CommandContext) {
        // sletting av snapshot gir ikke mening i denne kontekst
    }

    private fun oppdaterSnapshot(): Boolean {
        return speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer).let {
            snapshotDao.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, it) != 0
        }
    }
}
