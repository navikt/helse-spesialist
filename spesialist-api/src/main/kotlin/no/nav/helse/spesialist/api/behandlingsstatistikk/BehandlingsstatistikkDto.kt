package no.nav.helse.spesialist.api.behandlingsstatistikk

import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

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

data class StatistikkPerInntektOgPeriodetype(
    val perInntekttype: Map<Inntektskilde, Int>,
    val perPeriodetype: Map<Periodetype, Int>
)

data class InntektOgPeriodetyperad(
    val inntekttype: Inntektskilde,
    val periodetype: Periodetype,
    val antall: Int,
)