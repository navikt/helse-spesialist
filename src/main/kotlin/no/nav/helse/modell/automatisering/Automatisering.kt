package no.nav.helse.modell.automatisering

import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.risiko.Risikovurdering
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import java.util.*

internal class Automatisering(
    private val vedtakDao: VedtakDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val automatiseringDao: AutomatiseringDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao
) {
    fun godkjentForAutomatisertBehandling(fødselsnummer: String, vedtaksperiodeId: UUID): Boolean {
        val okRisikovurdering = risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
            ?.let { Risikovurdering.restore(it).kanBehandlesAutomatisk() } ?: false
        val ingenWarnings = vedtakDao.finnWarnings(vedtaksperiodeId).isEmpty()
        val støttetOppgavetype = vedtakDao.finnVedtaksperiodetype(vedtaksperiodeId) == Saksbehandleroppgavetype.FORLENGELSE
        val erDigital = digitalKontaktinformasjonDao.erDigital(fødselsnummer) ?: false

        return okRisikovurdering && ingenWarnings && støttetOppgavetype && erDigital
    }

    fun lagre(bleAutomatisert: Boolean, vedtaksperiodeId: UUID, hendelseId: UUID) {
        automatiseringDao.lagre(bleAutomatisert, vedtaksperiodeId, hendelseId)
    }

    fun harBlittAutomatiskBehandlet(vedtaksperiodeId: UUID, hendelseId: UUID) =
        automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId)?.automatisert ?: false
}
