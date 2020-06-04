package no.nav.helse.modell

import kotliquery.Session
import no.nav.helse.measureAsHistogram
import no.nav.helse.modell.command.RootCommand
import no.nav.helse.modell.vedtak.findVedtak
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

    override fun execute(session: Session) =
        measureAsHistogram("OppdaterVedtaksperiode_execute") {
            measureAsHistogram("OppdaterVedtaksperiode_execute_findVedtak") {
                session.findVedtak(vedtaksperiodeId)
            } ?: return@measureAsHistogram Resultat.Ok.System

            val snapshot = measureAsHistogram("OppdaterVedtaksperiode_execute_hentSpeilSnapshot") {
                speilSnapshotRestDao.hentSpeilSpapshot(fødselsnummer)
            }
            measureAsHistogram("OppdaterVedtaksperiode_execute_oppdaterSnapshot") {
                snapshotDao.oppdaterSnapshotForVedtaksperiode(
                    vedtaksperiodeId = vedtaksperiodeId,
                    snapshot = snapshot
                )
            }
            Resultat.Ok.System
        }

    override fun toJson() = "{}"
}
