package no.nav.helse.modell.oppgave.behandlingsstatistikk

import no.nav.helse.modell.vedtak.Periodetype

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
