package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Behandling
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
        oppdaterDatoer(sessionContext)
        val behandlingTilGodkjenning = sessionContext.behandlingRepository.finn(SpleisBehandlingId(godkjenningsbehovData.spleisBehandlingId))

        flyttKanskjeAvviksvarsel(sessionContext, behandlingTilGodkjenning)
        behandlingTilGodkjenning.oppdaterTags(godkjenningsbehovData.tags)
        behandlingTilGodkjenning.oppdaterUtbetalingId(UtbetalingId(godkjenningsbehovData.utbetalingId))
        sessionContext.behandlingRepository.lagre(behandlingTilGodkjenning)

        return true
    }

    private fun flyttKanskjeAvviksvarsel(
        sessionContext: SessionContext,
        behandling: Behandling,
    ) {
        val andreBehandlinger = sessionContext.behandlingRepository.finnAndreBehandlingerISykefraværstilfelle(behandling, godkjenningsbehovData.fødselsnummer)

        val varslerForAndreBehandlinger = sessionContext.varselRepository.finnAktiveVarslerFor(andreBehandlinger)

        val avviksvarsel = varslerForAndreBehandlinger.find { it.erVarselOmAvvik() }

        if (avviksvarsel != null) {
            avviksvarsel.flyttTil(behandling.id, behandling.spleisBehandlingId)
            sessionContext.varselRepository.lagre(avviksvarsel)
        }
    }

    private fun oppdaterDatoer(sessionContext: SessionContext) {
        godkjenningsbehovData.spleisBehandlinger
            .mapNotNull { spleisBehandling ->
                sessionContext.behandlingRepository.finnOrNull(SpleisBehandlingId(spleisBehandling.spleisBehandlingId))?.let {
                    spleisBehandling to it
                }
            }.onEach { (spleisBehandling, behandling) ->
                behandling.oppdaterDatoer(fom = spleisBehandling.fom, tom = spleisBehandling.tom, skjæringstidspunkt = spleisBehandling.skjæringstidspunkt)
                sessionContext.behandlingRepository.lagre(behandling)
            }
    }
}
