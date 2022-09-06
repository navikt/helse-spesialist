package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse

data class Antall(
    val automatisk: Int,
    val manuelt: Int,
    val tilgjengelig: Int,
)

data class Behandlingsstatistikk(
    private val behandlingsstatistikkResponse: BehandlingsstatistikkResponse,
) {
    fun enArbeidsgiver(): Antall = behandlingsstatistikkResponse.enArbeidsgiver.tilAntall()

    fun flereArbeidsgivere(): Antall = behandlingsstatistikkResponse.flereArbeidsgivere.tilAntall()

    fun forstegangsbehandling(): Antall = behandlingsstatistikkResponse.forstegangsbehandling.tilAntall()

    fun forlengelser(): Antall = behandlingsstatistikkResponse.forlengelser.tilAntall()

    fun utbetalingTilArbeidsgiver(): Antall = behandlingsstatistikkResponse.utbetalingTilArbeidsgiver.tilAntall()

    fun utbetalingTilSykmeldt(): Antall = behandlingsstatistikkResponse.utbetalingTilSykmeldt.tilAntall()

    fun faresignaler(): Antall = behandlingsstatistikkResponse.faresignaler.tilAntall()

    fun fortroligAdresse(): Antall = behandlingsstatistikkResponse.fortroligAdresse.tilAntall()

    fun stikkprover(): Antall = behandlingsstatistikkResponse.stikkprover.tilAntall()

    fun revurdering(): Antall = behandlingsstatistikkResponse.revurdering.tilAntall()

    fun delvisRefusjon(): Antall = behandlingsstatistikkResponse.delvisRefusjon.tilAntall()

    fun beslutter(): Antall = behandlingsstatistikkResponse.beslutter.tilAntall()

    fun antallAnnulleringer(): Int = behandlingsstatistikkResponse.antallAnnulleringer
}

private fun Statistikk.tilAntall(): Antall =
    Antall(
        automatisk = automatisk,
        manuelt = manuelt,
        tilgjengelig = tilgjengelig,
    )