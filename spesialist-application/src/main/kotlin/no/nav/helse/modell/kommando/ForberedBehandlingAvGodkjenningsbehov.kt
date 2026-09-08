package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId

internal class ForberedBehandlingAvGodkjenningsbehov(
    private val godkjenningsbehovData: GodkjenningsbehovData,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        godkjenningsbehovData.spleisVedtaksperioder
            .mapNotNull { spleisVedtaksperiode ->
                sessionContext.behandlingRepository.finn(SpleisBehandlingId(spleisVedtaksperiode.spleisBehandlingId))?.let {
                    spleisVedtaksperiode to it
                }
            }.onEach { (spleisVedtaksperiode, behandling) ->
                behandling.oppdaterDatoer(fom = spleisVedtaksperiode.fom, tom = spleisVedtaksperiode.tom, skjæringstidspunkt = spleisVedtaksperiode.skjæringstidspunkt)
                sessionContext.behandlingRepository.lagre(behandling)
            }

        val behandlingTilGodkjenning = sessionContext.behandlingRepository.finn(SpleisBehandlingId(godkjenningsbehovData.spleisBehandlingId)) ?: error("Fant ikke behandling med id ${godkjenningsbehovData.spleisBehandlingId}")
        val andreBehandlinger = sessionContext.behandlingRepository.finnAndreBehandlingerISykefraværstilfelle(behandlingTilGodkjenning, godkjenningsbehovData.fødselsnummer)

        val varslerForAndreBehandlinger = sessionContext.varselRepository.finnAktiveVarslerFor(andreBehandlinger)

        val avviksvarsel = varslerForAndreBehandlinger.find { it.erVarselOmAvvik() }

        if (avviksvarsel != null) {
            avviksvarsel.flyttTil(behandlingTilGodkjenning.id, behandlingTilGodkjenning.spleisBehandlingId)
            sessionContext.varselRepository.lagre(avviksvarsel)
        }
        behandlingTilGodkjenning.oppdaterTags(godkjenningsbehovData.tags)
        behandlingTilGodkjenning.oppdaterUtbetalingId(UtbetalingId(godkjenningsbehovData.utbetalingId))
        sessionContext.behandlingRepository.lagre(behandlingTilGodkjenning)

        return true
    }
}
