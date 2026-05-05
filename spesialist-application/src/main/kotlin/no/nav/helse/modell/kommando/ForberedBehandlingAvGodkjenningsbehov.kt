package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox

internal class ForberedBehandlingAvGodkjenningsbehov(
    private val godkjenningsbehovData: GodkjenningsbehovData,
    private val person: LegacyPerson,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        person.mottaSpleisVedtaksperioder(godkjenningsbehovData.spleisVedtaksperioder)
        person.flyttEventuelleAvviksvarsler(godkjenningsbehovData.vedtaksperiodeId, godkjenningsbehovData.skjæringstidspunkt)
        person.oppdaterPeriodeTilGodkjenning(
            godkjenningsbehovData.vedtaksperiodeId,
            godkjenningsbehovData.tags,
            godkjenningsbehovData.spleisBehandlingId,
            godkjenningsbehovData.utbetalingId,
        )
        return true
    }
}
