package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkResponse
import no.nav.helse.spesialist.api.behandlingsstatistikk.Statistikk

@GraphQLName("Antall")
data class ApiAntall(
    val automatisk: Int,
    val manuelt: Int,
    val tilgjengelig: Int,
)

@GraphQLName("Behandlingsstatistikk")
data class ApiBehandlingsstatistikk(
    private val behandlingsstatistikkResponse: BehandlingsstatistikkResponse,
) {
    fun enArbeidsgiver(): ApiAntall = behandlingsstatistikkResponse.enArbeidsgiver.tilAntall()

    fun flereArbeidsgivere(): ApiAntall = behandlingsstatistikkResponse.flereArbeidsgivere.tilAntall()

    fun forstegangsbehandling(): ApiAntall = behandlingsstatistikkResponse.forstegangsbehandling.tilAntall()

    fun forlengelser(): ApiAntall = behandlingsstatistikkResponse.forlengelser.tilAntall()

    fun forlengelseIt(): ApiAntall = behandlingsstatistikkResponse.forlengelseIt.tilAntall()

    fun utbetalingTilArbeidsgiver(): ApiAntall = behandlingsstatistikkResponse.utbetalingTilArbeidsgiver.tilAntall()

    fun utbetalingTilSykmeldt(): ApiAntall = behandlingsstatistikkResponse.utbetalingTilSykmeldt.tilAntall()

    fun faresignaler(): ApiAntall = behandlingsstatistikkResponse.faresignaler.tilAntall()

    fun fortroligAdresse(): ApiAntall = behandlingsstatistikkResponse.fortroligAdresse.tilAntall()

    fun stikkprover(): ApiAntall = behandlingsstatistikkResponse.stikkprover.tilAntall()

    fun revurdering(): ApiAntall = behandlingsstatistikkResponse.revurdering.tilAntall()

    fun delvisRefusjon(): ApiAntall = behandlingsstatistikkResponse.delvisRefusjon.tilAntall()

    fun beslutter(): ApiAntall = behandlingsstatistikkResponse.beslutter.tilAntall()

    fun egenAnsatt(): ApiAntall = behandlingsstatistikkResponse.egenAnsatt.tilAntall()

    fun antallAnnulleringer(): Int = behandlingsstatistikkResponse.antallAnnulleringer

    fun antallAvvisninger(): Int = behandlingsstatistikkResponse.antallAvvisninger
}

private fun Statistikk.tilAntall(): ApiAntall =
    ApiAntall(
        automatisk = automatisk,
        manuelt = manuelt,
        tilgjengelig = tilgjengelig,
    )
