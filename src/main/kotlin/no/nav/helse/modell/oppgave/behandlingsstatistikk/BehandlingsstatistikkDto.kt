package no.nav.helse.modell.oppgave.behandlingsstatistikk

import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype

data class BehandlingsstatistikkDto(
    val oppgaverTilGodkjenning: OppgaverTilGodkjenningDto,
    val antallTildelteOppgaver: Int,
    val antallGodkjenteOppgaver: Int,
    val antallAnnulleringer: Int
) {
    data class OppgaverTilGodkjenningDto(
        val totalt: Int,
        val perPeriodetype: Map<Saksbehandleroppgavetype, Int>
    )
}
