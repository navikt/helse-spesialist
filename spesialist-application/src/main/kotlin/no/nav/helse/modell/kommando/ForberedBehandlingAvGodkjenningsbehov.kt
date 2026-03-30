package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox

internal class ForberedBehandlingAvGodkjenningsbehov(
    private val commandData: GodkjenningsbehovData,
    private val person: LegacyPerson,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        person.mottaSpleisVedtaksperioder(commandData.spleisVedtaksperioder)
        person.flyttEventuelleAvviksvarsler(commandData.vedtaksperiodeId, commandData.skjæringstidspunkt)
        person.oppdaterPeriodeTilGodkjenning(
            commandData.vedtaksperiodeId,
            commandData.tags,
            commandData.spleisBehandlingId,
            commandData.utbetalingId,
        )
        return true
    }
}
