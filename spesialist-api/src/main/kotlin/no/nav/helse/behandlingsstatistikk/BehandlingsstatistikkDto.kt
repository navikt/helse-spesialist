package no.nav.helse.behandlingsstatistikk

data class BehandlingsstatistikkDto(
    val oppgaverTilGodkjenning: OppgavestatistikkDto,
    val tildelteOppgaver: OppgavestatistikkDto,
    val fullførteBehandlinger: BehandlingerDto
) {
    data class OppgavestatistikkDto(
        val totalt: Int,
        val perPeriodetype: List<Pair<BehandlingsstatistikkType, Int>>
    )
    data class BehandlingerDto(
        val totalt: Int,
        val annullert: Int,
        val manuelt: OppgavestatistikkDto,
        val automatisk: Int
    )
}
