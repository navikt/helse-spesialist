package no.nav.helse.spesialist.api.behandlingsstatistikk

import no.nav.helse.spesialist.api.graphql.schema.Utbetalingtype
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Mottakertype
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype

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
)

data class StatistikkPerKombinasjon(
    val perInntekttype: Map<Inntektskilde, Int>,
    val perPeriodetype: Map<Periodetype, Int>,
    val perMottakertype: Map<Mottakertype, Int>,
    val perUtbetalingtype: Map<Utbetalingtype, Int>,
)

data class AntallPerKombinasjonRad(
    val inntekttype: Inntektskilde,
    val periodetype: Periodetype,
    val mottakertype: Mottakertype?,
    val utbetalingtype: Utbetalingtype?,
    val antall: Int,
)
