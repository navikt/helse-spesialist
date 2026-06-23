package no.nav.helse.spesialist.api.rest

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Resource("behandlingsstatistikk")
class Behandlingsstatistikk

@Serializable
data class ApiBehandlingsstatistikkResponse(
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

@Serializable
data class ApiAntall(
    val automatisk: Int,
    val manuelt: Int,
    val tilgjengelig: Int,
)
