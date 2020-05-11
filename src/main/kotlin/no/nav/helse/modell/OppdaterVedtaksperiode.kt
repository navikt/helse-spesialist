package no.nav.helse.modell

import no.nav.helse.modell.command.RootCommand
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import java.time.Duration
import java.util.*

internal class OppdaterVedtaksperiode(
    eventId: UUID,
    override val fødselsnummer: String,
    override val vedtaksperiodeId: UUID,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val snapshotDao: SnapshotDao
) : RootCommand(eventId, Duration.ofHours(1)) {
    override val orgnummer: String? = null

    override fun execute(): Resultat.Ok.System {
        val snapshot = speilSnapshotRestDao.hentSpeilSpapshot(fødselsnummer)
        snapshotDao.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId, snapshot = snapshot)
        return Resultat.Ok.System
    }

    override fun toJson() = "{}"
}
