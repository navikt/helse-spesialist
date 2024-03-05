package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao

internal class LagreBehandlingsInformasjonCommand(
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val tags: List<String>,
    private val generasjonDao: GenerasjonDao,
): Command {
    override fun execute(context: CommandContext): Boolean {
        val generasjonId = requireNotNull(generasjonDao.finnSisteGenerasjonFor(vedtaksperiodeId)) {
            "Fant ikke generasjonId for vedtaksperiodeId=$vedtaksperiodeId"
        }
        generasjonDao.oppdaterMedBehandlingsInformasjon(generasjonId, spleisBehandlingId, tags)
        return true
    }
}
