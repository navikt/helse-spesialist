package no.nav.helse.modell.kommando

import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData

internal class ForberedBehandlingAvGodkjenningsbehov(
    private val commandData: GodkjenningsbehovData,
    private val person: Person,
) : Command {
    override fun execute(context: CommandContext): Boolean {
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
