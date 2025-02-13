package no.nav.helse.spesialist.api.graphql.mapping

import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Toggle
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverinntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverrefusjon
import no.nav.helse.spesialist.api.graphql.schema.ApiBegrunnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiDag
import no.nav.helse.spesialist.api.graphql.schema.ApiHendelse
import no.nav.helse.spesialist.api.graphql.schema.ApiHendelsetype
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektHentetFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektskilde
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektsmelding
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektstype
import no.nav.helse.spesialist.api.graphql.schema.ApiKilde
import no.nav.helse.spesialist.api.graphql.schema.ApiKildetype
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiNotat
import no.nav.helse.spesialist.api.graphql.schema.ApiOmregnetArsinntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetilstand
import no.nav.helse.spesialist.api.graphql.schema.ApiPeriodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiRefusjonselement
import no.nav.helse.spesialist.api.graphql.schema.ApiSammenligningsgrunnlag
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadArbeidsgiver
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadArbeidsledig
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadFrilans
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadNav
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadSelvstendig
import no.nav.helse.spesialist.api.graphql.schema.ApiSykdomsdagtype
import no.nav.helse.spesialist.api.graphql.schema.ApiSykepengegrunnlagsgrense
import no.nav.helse.spesialist.api.graphql.schema.ApiSykmelding
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetalingsdagtype
import no.nav.helse.spesialist.api.graphql.schema.ApiUtbetalingsinfo
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlag
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagInfotrygd
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagSpleis
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagtype
import no.nav.helse.spleis.graphql.enums.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLDag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLRefusjonselement
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSkjonnsmessigFastsatt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsledig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadFrilans
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadSelvstendig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykdomsdagkilde
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykmelding
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingsinfo
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVilkarsgrunnlag
import java.util.UUID

fun GraphQLDag.tilApiDag() =
    ApiDag(
        dato = dato,
        grad = grad,
        kilde = kilde.tilApiKilde(),
        sykdomsdagtype = sykdomsdagtype.tilApiSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilApiUtbetalingsdagtype(),
        utbetalingsinfo = utbetalingsinfo?.tilApiUtbetalingsinfo(),
        begrunnelser = begrunnelser?.map { it.tilApiBegrunnelse() },
    )

private fun GraphQLSykdomsdagkilde.tilApiKilde() =
    ApiKilde(
        id = id,
        type = type.tilApiKildetype(),
    )

private fun GraphQLSykdomsdagkildetype.tilApiKildetype() =
    when (this) {
        GraphQLSykdomsdagkildetype.INNTEKTSMELDING -> ApiKildetype.INNTEKTSMELDING
        GraphQLSykdomsdagkildetype.SAKSBEHANDLER -> ApiKildetype.SAKSBEHANDLER
        GraphQLSykdomsdagkildetype.SOKNAD -> ApiKildetype.SOKNAD
        GraphQLSykdomsdagkildetype.SYKMELDING -> ApiKildetype.SYKMELDING
        else -> ApiKildetype.UKJENT
    }

private fun GraphQLSykdomsdagtype.tilApiSykdomsdagtype() =
    when (this) {
        GraphQLSykdomsdagtype.ARBEIDSDAG -> ApiSykdomsdagtype.ARBEIDSDAG
        GraphQLSykdomsdagtype.ARBEIDSGIVERDAG -> ApiSykdomsdagtype.ARBEIDSGIVERDAG
        GraphQLSykdomsdagtype.AVSLATT -> ApiSykdomsdagtype.AVSLATT
        GraphQLSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG -> ApiSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG
        GraphQLSykdomsdagtype.FERIEDAG -> ApiSykdomsdagtype.FERIEDAG
        GraphQLSykdomsdagtype.FORELDETSYKEDAG -> ApiSykdomsdagtype.FORELDET_SYKEDAG
        GraphQLSykdomsdagtype.FRISKHELGEDAG -> ApiSykdomsdagtype.FRISK_HELGEDAG
        GraphQLSykdomsdagtype.PERMISJONSDAG -> ApiSykdomsdagtype.PERMISJONSDAG
        GraphQLSykdomsdagtype.SYKHELGEDAG -> ApiSykdomsdagtype.SYK_HELGEDAG
        GraphQLSykdomsdagtype.SYKEDAG -> ApiSykdomsdagtype.SYKEDAG
        GraphQLSykdomsdagtype.SYKEDAGNAV -> ApiSykdomsdagtype.SYKEDAG_NAV
        GraphQLSykdomsdagtype.UBESTEMTDAG -> ApiSykdomsdagtype.UBESTEMTDAG
        GraphQLSykdomsdagtype.ANDREYTELSERAAP -> ApiSykdomsdagtype.ANDRE_YTELSER_AAP
        GraphQLSykdomsdagtype.ANDREYTELSERDAGPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_DAGPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERFORELDREPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_FORELDREPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROMSORGSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_OMSORGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_OPPLARINGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERPLEIEPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_PLEIEPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_SVANGERSKAPSPENGER
        else -> throw Exception("Ukjent sykdomsdagtype $name")
    }

private fun GraphQLUtbetalingsdagType.tilApiUtbetalingsdagtype() =
    when (this) {
        GraphQLUtbetalingsdagType.ARBEIDSDAG -> ApiUtbetalingsdagtype.ARBEIDSDAG
        GraphQLUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG -> ApiUtbetalingsdagtype.ARBEIDSGIVERPERIODEDAG
        GraphQLUtbetalingsdagType.AVVISTDAG -> ApiUtbetalingsdagtype.AVVIST_DAG
        GraphQLUtbetalingsdagType.FERIEDAG -> ApiUtbetalingsdagtype.FERIEDAG
        GraphQLUtbetalingsdagType.FORELDETDAG -> ApiUtbetalingsdagtype.FORELDET_DAG
        GraphQLUtbetalingsdagType.HELGEDAG -> ApiUtbetalingsdagtype.HELGEDAG
        GraphQLUtbetalingsdagType.NAVDAG -> ApiUtbetalingsdagtype.NAVDAG
        GraphQLUtbetalingsdagType.NAVHELGDAG -> ApiUtbetalingsdagtype.NAVHELGDAG
        else -> ApiUtbetalingsdagtype.UKJENT_DAG
    }

private fun GraphQLUtbetalingsinfo.tilApiUtbetalingsinfo() =
    ApiUtbetalingsinfo(
        arbeidsgiverbelop = arbeidsgiverbelop,
        inntekt = inntekt,
        personbelop = personbelop,
        refusjonsbelop = refusjonsbelop,
        totalGrad = totalGrad,
        utbetaling = utbetaling,
    )

private fun GraphQLBegrunnelse.tilApiBegrunnelse() =
    when (this) {
        GraphQLBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE -> ApiBegrunnelse.EGENMELDING_UTENFOR_ARBEIDSGIVERPERIODE
        GraphQLBegrunnelse.ETTERDODSDATO -> ApiBegrunnelse.ETTER_DODSDATO
        GraphQLBegrunnelse.MANGLERMEDLEMSKAP -> ApiBegrunnelse.MANGLER_MEDLEMSKAP
        GraphQLBegrunnelse.MANGLEROPPTJENING -> ApiBegrunnelse.MANGLER_OPPTJENING
        GraphQLBegrunnelse.MINIMUMINNTEKT -> ApiBegrunnelse.MINIMUM_INNTEKT
        GraphQLBegrunnelse.MINIMUMINNTEKTOVER67 -> ApiBegrunnelse.MINIMUM_INNTEKT_OVER_67
        GraphQLBegrunnelse.MINIMUMSYKDOMSGRAD -> ApiBegrunnelse.MINIMUM_SYKDOMSGRAD
        GraphQLBegrunnelse.OVER70 -> ApiBegrunnelse.OVER_70
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKT -> ApiBegrunnelse.SYKEPENGEDAGER_OPPBRUKT
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67 -> ApiBegrunnelse.SYKEPENGEDAGER_OPPBRUKT_OVER_67
        GraphQLBegrunnelse.ANDREYTELSER -> ApiBegrunnelse.ANDREYTELSER
        GraphQLBegrunnelse.__UNKNOWN_VALUE -> ApiBegrunnelse.UKJENT
    }

fun GraphQLHendelse.tilApiHendelse(): ApiHendelse =
    when (this) {
        is GraphQLInntektsmelding ->
            ApiInntektsmelding(
                id = UUID.fromString(id),
                type = ApiHendelsetype.INNTEKTSMELDING,
                mottattDato = mottattDato,
                beregnetInntekt = beregnetInntekt,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadArbeidsgiver ->
            ApiSoknadArbeidsgiver(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_ARBEIDSGIVER,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtArbeidsgiver = sendtArbeidsgiver,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadNav ->
            ApiSoknadNav(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_NAV,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadArbeidsledig ->
            ApiSoknadArbeidsledig(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_ARBEIDSLEDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadFrilans ->
            ApiSoknadFrilans(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_FRILANS,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadSelvstendig ->
            ApiSoknadSelvstendig(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_SELVSTENDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSykmelding ->
            ApiSykmelding(
                id = UUID.fromString(id),
                type = ApiHendelsetype.NY_SOKNAD,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
            )

        is GraphQLInntektFraAOrdningen ->
            ApiInntektHentetFraAOrdningen(
                id = UUID.fromString(id),
                type = ApiHendelsetype.INNTEKT_HENTET_FRA_AORDNINGEN,
                mottattDato = mottattDato,
            )

        else -> throw Exception("Ukjent hendelsestype ${javaClass.name}")
    }

fun GraphQLOmregnetArsinntekt.tilApiOmregnetArsinntekt() =
    ApiOmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = inntekterFraAOrdningen?.map { it.tilApiInntektFraAOrdningen() },
        kilde = kilde.tilApiInntektskilde(),
        manedsbelop = manedsbelop,
    )

fun GraphQLSkjonnsmessigFastsatt.tilApiOmregnetArsinntekt() =
    ApiOmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = null,
        kilde = ApiInntektskilde.SKJONNSMESSIG_FASTSATT,
        manedsbelop = manedsbelop,
    )

private fun GraphQLInntekterFraAOrdningen.tilApiInntektFraAOrdningen() =
    ApiInntektFraAOrdningen(
        maned = maned,
        sum = sum,
    )

private fun GraphQLInntektskilde.tilApiInntektskilde() =
    when (this) {
        GraphQLInntektskilde.AORDNINGEN -> ApiInntektskilde.AORDNINGEN
        GraphQLInntektskilde.INFOTRYGD -> ApiInntektskilde.INFOTRYGD
        GraphQLInntektskilde.INNTEKTSMELDING -> ApiInntektskilde.INNTEKTSMELDING
        GraphQLInntektskilde.SAKSBEHANDLER -> ApiInntektskilde.SAKSBEHANDLER
        GraphQLInntektskilde.IKKERAPPORTERT -> ApiInntektskilde.IKKE_RAPPORTERT
        else -> throw Exception("Ukjent inntektskilde ${this.name}")
    }

fun GraphQLArbeidsgiverrefusjon.tilApiArbeidsgiverrefusjon() =
    ApiArbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.map { it.tilApiRefusjonselement() },
    )

private fun GraphQLRefusjonselement.tilApiRefusjonselement() =
    ApiRefusjonselement(
        fom = fom,
        tom = tom,
        belop = belop,
        meldingsreferanseId = meldingsreferanseId,
    )

fun GraphQLPeriodetilstand.tilApiPeriodetilstand(erSisteGenerasjon: Boolean) =
    when (this) {
        GraphQLPeriodetilstand.ANNULLERINGFEILET -> ApiPeriodetilstand.AnnulleringFeilet
        GraphQLPeriodetilstand.ANNULLERT -> ApiPeriodetilstand.Annullert
        GraphQLPeriodetilstand.INGENUTBETALING -> ApiPeriodetilstand.IngenUtbetaling
        GraphQLPeriodetilstand.REVURDERINGFEILET -> ApiPeriodetilstand.RevurderingFeilet
        GraphQLPeriodetilstand.TILANNULLERING -> ApiPeriodetilstand.TilAnnullering
        GraphQLPeriodetilstand.TILINFOTRYGD -> ApiPeriodetilstand.TilInfotrygd
        GraphQLPeriodetilstand.TILUTBETALING -> ApiPeriodetilstand.TilUtbetaling
        GraphQLPeriodetilstand.UTBETALT -> ApiPeriodetilstand.Utbetalt
        GraphQLPeriodetilstand.FORBEREDERGODKJENNING -> ApiPeriodetilstand.ForberederGodkjenning
        GraphQLPeriodetilstand.MANGLERINFORMASJON -> ApiPeriodetilstand.ManglerInformasjon
        GraphQLPeriodetilstand.TILGODKJENNING -> ApiPeriodetilstand.TilGodkjenning
        GraphQLPeriodetilstand.UTBETALINGFEILET -> ApiPeriodetilstand.UtbetalingFeilet
        GraphQLPeriodetilstand.VENTERPAANNENPERIODE -> ApiPeriodetilstand.VenterPaEnAnnenPeriode
        GraphQLPeriodetilstand.UTBETALTVENTERPAANNENPERIODE -> {
            if (Toggle.BehandleEnOgEnPeriode.enabled && erSisteGenerasjon) {
                ApiPeriodetilstand.VenterPaEnAnnenPeriode
            } else {
                ApiPeriodetilstand.UtbetaltVenterPaEnAnnenPeriode
            }
        }

        GraphQLPeriodetilstand.AVVENTERINNTEKTSOPPLYSNINGER -> ApiPeriodetilstand.AvventerInntektsopplysninger
        GraphQLPeriodetilstand.TILSKJONNSFASTSETTELSE -> ApiPeriodetilstand.TilSkjonnsfastsettelse
        else -> ApiPeriodetilstand.Ukjent
    }

fun NotatApiDao.NotatDto.tilApiNotat() =
    ApiNotat(
        id = id,
        dialogRef = dialogRef,
        tekst = tekst,
        opprettet = opprettet,
        saksbehandlerOid = saksbehandlerOid,
        saksbehandlerNavn = saksbehandlerNavn,
        saksbehandlerEpost = saksbehandlerEpost,
        saksbehandlerIdent = saksbehandlerIdent,
        vedtaksperiodeId = vedtaksperiodeId,
        feilregistrert = feilregistrert,
        feilregistrert_tidspunkt = feilregistrert_tidspunkt,
        type = type.tilSkjematype(),
        kommentarer =
            kommentarer.map { kommentar ->
                ApiKommentar(
                    id = kommentar.id,
                    tekst = kommentar.tekst,
                    opprettet = kommentar.opprettet,
                    saksbehandlerident = kommentar.saksbehandlerident,
                    feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt,
                )
            },
    )

fun GraphQLTidslinjeperiode.tilApiPeriodetype() =
    when (periodetype) {
        GraphQLPeriodetype.FORLENGELSE -> ApiPeriodetype.FORLENGELSE
        GraphQLPeriodetype.FORSTEGANGSBEHANDLING -> ApiPeriodetype.FORSTEGANGSBEHANDLING
        GraphQLPeriodetype.INFOTRYGDFORLENGELSE -> ApiPeriodetype.INFOTRYGDFORLENGELSE
        GraphQLPeriodetype.OVERGANGFRAIT -> ApiPeriodetype.OVERGANG_FRA_IT
        else -> throw Exception("Ukjent periodetype $periodetype")
    }

fun GraphQLInntektstype.tilApiInntektstype() =
    when (this) {
        GraphQLInntektstype.ENARBEIDSGIVER -> ApiInntektstype.ENARBEIDSGIVER
        GraphQLInntektstype.FLEREARBEIDSGIVERE -> ApiInntektstype.FLEREARBEIDSGIVERE
        else -> throw Exception("Ukjent inntektstype $this")
    }

fun GraphQLVilkarsgrunnlag.tilVilkarsgrunnlag(avviksvurderinghenter: Avviksvurderinghenter): ApiVilkårsgrunnlag {
    return when (this) {
        is GraphQLSpleisVilkarsgrunnlag -> {
            val avviksvurdering: Avviksvurdering =
                checkNotNull(avviksvurderinghenter.hentAvviksvurdering(id)) { "Fant ikke avviksvurdering for vilkårsgrunnlagId $id" }
            val orgnrs =
                (
                    avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.map {
                        it.arbeidsgiverreferanse
                    } + inntekter.map { it.arbeidsgiver }
                ).toSet()
            val inntekter =
                orgnrs.map { arbeidsgiverreferanse ->
                    val inntektFraSpleis =
                        inntekter.singleOrNull { inntektFraSpleis -> inntektFraSpleis.arbeidsgiver == arbeidsgiverreferanse }
                    val sammenligningsgrunnlagInntekt =
                        avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.singleOrNull { it.arbeidsgiverreferanse == arbeidsgiverreferanse }
                    ApiArbeidsgiverinntekt(
                        arbeidsgiver = arbeidsgiverreferanse,
                        omregnetArsinntekt = inntektFraSpleis?.omregnetArsinntekt?.tilApiOmregnetArsinntekt(),
                        sammenligningsgrunnlag =
                            sammenligningsgrunnlagInntekt?.let {
                                ApiSammenligningsgrunnlag(
                                    belop = sammenligningsgrunnlagInntekt.inntekter.sumOf { it.beløp },
                                    inntektFraAOrdningen =
                                        sammenligningsgrunnlagInntekt.inntekter.map { inntekt ->
                                            ApiInntektFraAOrdningen(
                                                maned = inntekt.årMåned,
                                                sum = inntekt.beløp,
                                            )
                                        },
                                )
                            },
                        skjonnsmessigFastsatt = inntektFraSpleis?.skjonnsmessigFastsatt?.tilApiOmregnetArsinntekt(),
                        deaktivert = inntektFraSpleis?.deaktivert,
                        fom = inntektFraSpleis?.fom,
                        tom = inntektFraSpleis?.tom,
                    )
                }

            ApiVilkårsgrunnlagSpleis(
                inntekter = inntekter,
                omregnetArsinntekt = avviksvurdering.beregningsgrunnlag.totalbeløp,
                sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag.totalbeløp,
                avviksprosent = avviksvurdering.avviksprosent,
                vilkarsgrunnlagtype = ApiVilkårsgrunnlagtype.SPLEIS,
                id = id,
                arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilApiArbeidsgiverrefusjon() },
                skjonnsmessigFastsattAarlig = skjonnsmessigFastsattAarlig,
                skjaeringstidspunkt = skjaeringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag,
                antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
                grunnbelop = grunnbelop,
                sykepengegrunnlagsgrense = sykepengegrunnlagsgrense.tilSykepengegrunnlaggrense(),
                oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap,
                oppfyllerKravOmMinstelonn = oppfyllerKravOmMinstelonn,
                oppfyllerKravOmOpptjening = oppfyllerKravOmOpptjening,
                opptjeningFra = opptjeningFra,
            )
        }

        is GraphQLInfotrygdVilkarsgrunnlag ->
            ApiVilkårsgrunnlagInfotrygd(
                id = id,
                vilkarsgrunnlagtype = ApiVilkårsgrunnlagtype.INFOTRYGD,
                inntekter =
                    inntekter.map {
                        ApiArbeidsgiverinntekt(
                            arbeidsgiver = it.arbeidsgiver,
                            omregnetArsinntekt = it.omregnetArsinntekt.tilApiOmregnetArsinntekt(),
                            sammenligningsgrunnlag = null,
                            skjonnsmessigFastsatt = null,
                            deaktivert = it.deaktivert,
                        )
                    },
                arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilApiArbeidsgiverrefusjon() },
                omregnetArsinntekt = omregnetArsinntekt,
                skjaeringstidspunkt = skjaeringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag,
            )

        else -> throw Exception("Ukjent vilkårsgrunnlag ${this.javaClass.name}")
    }
}

internal fun GraphQLSykepengegrunnlagsgrense.tilSykepengegrunnlaggrense() =
    ApiSykepengegrunnlagsgrense(
        grunnbelop,
        grense,
        virkningstidspunkt,
    )
