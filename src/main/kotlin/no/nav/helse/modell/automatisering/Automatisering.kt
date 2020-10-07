package no.nav.helse.modell.automatisering

import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.slf4j.LoggerFactory
import java.util.*

internal class Automatisering(
    private val vedtakDao: VedtakDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao
) {
    companion object {
        private val logg = LoggerFactory.getLogger(Automatisering::class.java)
    }

    fun godkjentForAutomatisertBehandling(fødselsnummer: String, vedtaksperiodeId: UUID): Boolean {
        val okRisikovurdering =
            risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)?.kanBehandlesAutomatisk() ?: false
        val ingenWarnings = vedtakDao.finnWarnings(vedtaksperiodeId).isEmpty()
        val støttetOppgavetype =
            vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId) == Saksbehandleroppgavetype.FORLENGELSE
        val erDigital = digitalKontaktinformasjonDao.erDigital(fødselsnummer) ?: false
        val harÅpneGosysOppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(fødselsnummer)?.let { it > 0 } ?: true
        return (okRisikovurdering && ingenWarnings && støttetOppgavetype && erDigital && !harÅpneGosysOppgaver)
            .also { okForAutomatisering ->
                logg.info(
                    "Vedtaksperiode: {} vurderes som {} for automatisering [okRisikovurdering: {}, ingenWarnings: {}, støttetOppgavetype: {}, erDigital: {}, harÅpneGosysoppgaver: {}]",
                    vedtaksperiodeId.toString(),
                    if (okForAutomatisering) "ok" else "ikke ok",
                    okRisikovurdering,
                    ingenWarnings,
                    støttetOppgavetype,
                    erDigital,
                    harÅpneGosysOppgaver
                )
            }
    }

    fun lagre(bleAutomatisert: Boolean, vedtaksperiodeId: UUID, hendelseId: UUID) {
        automatiseringDao.lagre(bleAutomatisert, vedtaksperiodeId, hendelseId)
    }

    fun harBlittAutomatiskBehandlet(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId)?.automatisert ?: false
}
