package no.nav.helse.spesialist.api.behandlingsstatistikk

data class Statistikk(
    val automatisk: Int,
    val manuelt: Int,
    val tilgjengelig: Int,
)

data class BehandlingsstatistikkResponse(
    val enArbeidsgiver: Statistikk,
    val flereArbeidsgivere: Statistikk,
    val forstegangsbehandling: Statistikk,
    val forlengelser: Statistikk,
    val forlengelseIt: Statistikk,
    val utbetalingTilArbeidsgiver: Statistikk,
    val utbetalingTilSykmeldt: Statistikk,
    val faresignaler: Statistikk,
    val fortroligAdresse: Statistikk,
    val stikkprover: Statistikk,
    val revurdering: Statistikk,
    val delvisRefusjon: Statistikk,
    val beslutter: Statistikk,
    val egenAnsatt: Statistikk,
    val antallAnnulleringer: Int,
    val antallAvvisninger: Int,
)
