package no.nav.helse.behandlingsstatistikk

import no.nav.helse.vedtaksperiode.Periodetype

data class BehandlingsstatistikkDto(
    val oppgaverTilGodkjenning: OppgavestatistikkDto,
    val tildelteOppgaver: OppgavestatistikkDto,
    val fullførteBehandlinger: BehandlingerDto
) {
    data class OppgavestatistikkDto(
        val totalt: Int,
        val perPeriodetype: List<Pair<Periodetype, Int>>
    )
    data class BehandlingerDto(
        val totalt: Int,
        val annullert: Int,
        val manuelt: Int,
        val automatisk: Int
    )
}
