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

data class Antall(
    val automatisk: Int,
    val manuelt: Int,
    val tilgjengelig: Int,
)

data class Behandlingsstatistikk(
    val enArbeidsgiver: Antall,
    val flereArbeidsgivere: Antall,
    val forstegangsbehandling: Antall,
    val forlengelser: Antall,
    val utbetalingTilSykmeldt: Antall,
    val faresignaler: Antall,
    val fortroligAdresse: Antall,
    val stikkprover: Antall,
    val revurdering: Antall,
    val delvisRefusjon: Antall,
    val beslutter: Antall,
)

data class StatistikkPerInntektOgPeriodetype(
    val perInntekttype: Map<Inntektskilde, Int>,
    val perPeriodetype: Map<Periodetype, Int>
)

data class InntektOgPeriodetyperad(
    val inntekttype: Inntektskilde,
    val periodetype: Periodetype,
    val antall: Int,
)