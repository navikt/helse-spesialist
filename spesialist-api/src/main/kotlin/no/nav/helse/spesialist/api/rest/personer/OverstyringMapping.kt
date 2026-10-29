package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.Skjonnsfastsettingstype
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.api.rest.ApiArbeidsforholdoverstyring
import no.nav.helse.spesialist.api.rest.ApiDagoverstyring
import no.nav.helse.spesialist.api.rest.ApiDagtype
import no.nav.helse.spesialist.api.rest.ApiInntektoverstyring
import no.nav.helse.spesialist.api.rest.ApiMinimumSykdomsgradOverstyring
import no.nav.helse.spesialist.api.rest.ApiOverstyring
import no.nav.helse.spesialist.api.rest.ApiOverstyrtDag
import no.nav.helse.spesialist.api.rest.ApiOverstyrtInntekt
import no.nav.helse.spesialist.api.rest.ApiOverstyrtMinimumSykdomsgrad
import no.nav.helse.spesialist.api.rest.ApiOverstyrtMinimumSykdomsgradPeriode
import no.nav.helse.spesialist.api.rest.ApiRefusjonsopplysning
import no.nav.helse.spesialist.api.rest.ApiSaksbehandler
import no.nav.helse.spesialist.api.rest.ApiSkjonnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.api.rest.ApiSkjonnsfastsettingstype
import no.nav.helse.spesialist.api.rest.ApiSykepengegrunnlagskjonnsfastsetting

internal fun OverstyringDto.tilApiOverstyring(): ApiOverstyring =
    when (this) {
        is OverstyringTidslinjeDto -> tilDagoverstyring()
        is OverstyringArbeidsforholdDto -> tilArbeidsforholdoverstyring()
        is OverstyringInntektDto -> tilInntektoverstyring()
        is SkjønnsfastsettingSykepengegrunnlagDto -> tilSykepengegrunnlagSkjønnsfastsetting()
        is OverstyringMinimumSykdomsgradDto -> tilMinimumSykdomsgradOverstyring()
    }

private fun OverstyringTidslinjeDto.tilDagoverstyring() =
    ApiDagoverstyring(
        hendelseId = hendelseId,
        begrunnelse = begrunnelse,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        dager =
            overstyrteDager.map { dag ->
                ApiOverstyrtDag(
                    dato = dag.dato,
                    type = dag.type.tilApiDagtype(),
                    fraType = dag.fraType?.tilApiDagtype(),
                    grad = dag.grad,
                    fraGrad = dag.fraGrad,
                )
            },
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
    )

private fun Dagtype.tilApiDagtype() =
    when (this) {
        Dagtype.Sykedag -> ApiDagtype.Sykedag
        Dagtype.SykedagNav -> ApiDagtype.SykedagNav
        Dagtype.Feriedag -> ApiDagtype.Feriedag
        Dagtype.Egenmeldingsdag -> ApiDagtype.Egenmeldingsdag
        Dagtype.Permisjonsdag -> ApiDagtype.Permisjonsdag
        Dagtype.Arbeidsdag -> ApiDagtype.Arbeidsdag
        Dagtype.ArbeidIkkeGjenopptattDag -> ApiDagtype.ArbeidIkkeGjenopptattDag
        Dagtype.Foreldrepengerdag -> ApiDagtype.Foreldrepengerdag
        Dagtype.AAPdag -> ApiDagtype.AAPdag
        Dagtype.Omsorgspengerdag -> ApiDagtype.Omsorgspengerdag
        Dagtype.Pleiepengerdag -> ApiDagtype.Pleiepengerdag
        Dagtype.Svangerskapspengerdag -> ApiDagtype.Svangerskapspengerdag
        Dagtype.Opplaringspengerdag -> ApiDagtype.Opplaringspengerdag
        Dagtype.Dagpengerdag -> ApiDagtype.Dagpengerdag
        Dagtype.Avvistdag -> ApiDagtype.Avvistdag
        Dagtype.Helg -> ApiDagtype.Helg
        Dagtype.MeldingTilNavdag -> ApiDagtype.MeldingTilNavdag
        Dagtype.AvslattMeldingTilNavdag -> ApiDagtype.AvslattMeldingTilNavdag
    }

private fun OverstyringInntektDto.tilInntektoverstyring() =
    ApiInntektoverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        inntekt =
            ApiOverstyrtInntekt(
                forklaring = forklaring,
                begrunnelse = begrunnelse,
                manedligInntekt = månedligInntekt,
                fraManedligInntekt = fraMånedligInntekt,
                skjaeringstidspunkt = skjæringstidspunkt,
                refusjonsopplysninger =
                    refusjonsopplysninger?.map {
                        ApiRefusjonsopplysning(
                            fom = it.fom,
                            tom = it.tom,
                            belop = it.beløp,
                        )
                    } ?: emptyList(),
                fraRefusjonsopplysninger =
                    fraRefusjonsopplysninger?.map {
                        ApiRefusjonsopplysning(
                            fom = it.fom,
                            tom = it.tom,
                            belop = it.beløp,
                        )
                    } ?: emptyList(),
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
    )

private fun OverstyringArbeidsforholdDto.tilArbeidsforholdoverstyring() =
    ApiArbeidsforholdoverstyring(
        hendelseId = hendelseId,
        begrunnelse = begrunnelse,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        deaktivert = deaktivert,
        skjaeringstidspunkt = skjæringstidspunkt,
        forklaring = forklaring,
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
    )

private fun SkjønnsfastsettingSykepengegrunnlagDto.tilSykepengegrunnlagSkjønnsfastsetting() =
    ApiSykepengegrunnlagskjonnsfastsetting(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        skjonnsfastsatt =
            ApiSkjonnsfastsattSykepengegrunnlag(
                arsak = årsak,
                type = type.tilApiSkjonnsfastsettingstype(),
                begrunnelse = begrunnelse,
                begrunnelseMal = begrunnelseMal,
                begrunnelseFritekst = begrunnelseFritekst,
                begrunnelseKonklusjon = begrunnelseKonklusjon,
                arlig = årlig,
                fraArlig = fraÅrlig,
                skjaeringstidspunkt = skjæringstidspunkt,
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
    )

private fun Skjonnsfastsettingstype.tilApiSkjonnsfastsettingstype() =
    when (this) {
        Skjonnsfastsettingstype.OMREGNET_ARSINNTEKT -> ApiSkjonnsfastsettingstype.OMREGNET_ARSINNTEKT
        Skjonnsfastsettingstype.RAPPORTERT_ARSINNTEKT -> ApiSkjonnsfastsettingstype.RAPPORTERT_ARSINNTEKT
        Skjonnsfastsettingstype.ANNET -> ApiSkjonnsfastsettingstype.ANNET
    }

private fun OverstyringMinimumSykdomsgradDto.tilMinimumSykdomsgradOverstyring() =
    ApiMinimumSykdomsgradOverstyring(
        hendelseId = hendelseId,
        timestamp = timestamp,
        saksbehandler =
            ApiSaksbehandler(
                navn = saksbehandlerNavn,
                ident = saksbehandlerIdent,
            ),
        minimumSykdomsgrad =
            ApiOverstyrtMinimumSykdomsgrad(
                perioderVurdertOk =
                    perioderVurdertOk.map {
                        ApiOverstyrtMinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                perioderVurdertIkkeOk =
                    perioderVurdertIkkeOk.map {
                        ApiOverstyrtMinimumSykdomsgradPeriode(
                            fom = it.fom,
                            tom = it.tom,
                        )
                    },
                begrunnelse = begrunnelse,
                initierendeVedtaksperiodeId = vedtaksperiodeId,
            ),
        ferdigstilt = ferdigstilt,
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
    )
