package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox

internal class ForberedBehandlingAvGodkjenningsbehov(
    private val godkjenningsbehovData: GodkjenningsbehovData,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        sessionContext.legacyPersonRepository.brukPerson(godkjenningsbehovData.fødselsnummer) {
            this.mottaSpleisVedtaksperioder(godkjenningsbehovData.spleisVedtaksperioder)
            this.flyttEventuelleAvviksvarsler(godkjenningsbehovData.vedtaksperiodeId, godkjenningsbehovData.skjæringstidspunkt)
            this.oppdaterPeriodeTilGodkjenning(
                godkjenningsbehovData.vedtaksperiodeId,
                godkjenningsbehovData.tags,
                godkjenningsbehovData.spleisBehandlingId,
                godkjenningsbehovData.utbetalingId,
            )
        }
        return true
    }
}
