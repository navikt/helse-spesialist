package no.nav.helse.modell.command.ny

import kotliquery.Session
import no.nav.helse.measureAsHistogram
import no.nav.helse.modell.vedtak.findVedtak
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtak.snapshot.oppdaterSnapshotForVedtaksperiode
import org.slf4j.LoggerFactory
import java.util.*


internal class NyOppdaterVedtaksperiodeCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : NyCommand {
    private val log = LoggerFactory.getLogger(this::class.java)
    override val type = "OppdaterVedtaksperiodeCommand"

    override fun execute(session: Session) =
        measureAsHistogram("OppdaterVedtaksperiode_execute") {
            log.info("Henter vedtaksperiode for $vedtaksperiodeId")
            measureAsHistogram("OppdaterVedtaksperiode_execute_findVedtak") {
                session.findVedtak(vedtaksperiodeId)
            } ?: return@measureAsHistogram NyCommand.Resultat.Ok

            val snapshot = measureAsHistogram("OppdaterVedtaksperiode_execute_hentSpeilSnapshot") {
                speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer)
            }
            measureAsHistogram("OppdaterVedtaksperiode_execute_oppdaterSnapshot") {
                session.oppdaterSnapshotForVedtaksperiode(
                    vedtaksperiodeId = vedtaksperiodeId,
                    snapshot = snapshot
                )
            }
            NyCommand.Resultat.Ok
        }


    override fun resume(session: Session): NyCommand.Resultat {
        throw NotImplementedError()
    }
}
