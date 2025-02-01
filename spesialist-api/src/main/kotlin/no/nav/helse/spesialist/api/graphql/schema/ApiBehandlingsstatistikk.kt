package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName

@GraphQLName("Antall")
data class ApiAntall(
    val automatisk: Int,
    val manuelt: Int,
    val tilgjengelig: Int,
)

@GraphQLName("Behandlingsstatistikk")
data class ApiBehandlingsstatistikk(
    val enArbeidsgiver: ApiAntall,
    val flereArbeidsgivere: ApiAntall,
    val forstegangsbehandling: ApiAntall,
    val forlengelser: ApiAntall,
    val forlengelseIt: ApiAntall,
    val utbetalingTilArbeidsgiver: ApiAntall,
    val utbetalingTilSykmeldt: ApiAntall,
    val faresignaler: ApiAntall,
    val fortroligAdresse: ApiAntall,
    val stikkprover: ApiAntall,
    val revurdering: ApiAntall,
    val delvisRefusjon: ApiAntall,
    val beslutter: ApiAntall,
    val egenAnsatt: ApiAntall,
    val antallAnnulleringer: Int,
    val antallAvvisninger: Int,
)
