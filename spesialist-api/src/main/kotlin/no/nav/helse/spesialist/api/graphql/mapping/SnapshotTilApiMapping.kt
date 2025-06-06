package no.nav.helse.spesialist.api.graphql.mapping

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.spesialist.api.Toggle
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverinntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverrefusjon
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsgiverrefusjonV2
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidstakerinntektInfotrygd
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidstakerinntektSpleis
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidstakerinntektSpleisAvventerAvviksvurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiArsinntekt
import no.nav.helse.spesialist.api.graphql.schema.ApiBegrunnelse
import no.nav.helse.spesialist.api.graphql.schema.ApiDag
import no.nav.helse.spesialist.api.graphql.schema.ApiHendelse
import no.nav.helse.spesialist.api.graphql.schema.ApiHendelsetype
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektFraAOrdningen
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektFraAOrdningenV2
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
import no.nav.helse.spesialist.api.graphql.schema.ApiRefusjonselementV2
import no.nav.helse.spesialist.api.graphql.schema.ApiSammenligningsgrunnlag
import no.nav.helse.spesialist.api.graphql.schema.ApiSammenligningsgrunnlagV2
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
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagArbeidstakerInfotrygd
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagArbeidstakerSpleis
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagAvviksvurdering
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagInfotrygd
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagKrav
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagKravAlltidVurdert
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagSpleis
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagV2
import no.nav.helse.spesialist.api.graphql.schema.ApiVilkårsgrunnlagtype
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverrefusjon
import no.nav.helse.spesialist.application.snapshot.SnapshotBegrunnelse
import no.nav.helse.spesialist.application.snapshot.SnapshotDag
import no.nav.helse.spesialist.application.snapshot.SnapshotHendelse
import no.nav.helse.spesialist.application.snapshot.SnapshotInfotrygdVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektFraAOrdningen
import no.nav.helse.spesialist.application.snapshot.SnapshotInntekterFraAOrdningen
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektskilde
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektsmelding
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektstype
import no.nav.helse.spesialist.application.snapshot.SnapshotOmregnetArsinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetilstand
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetype
import no.nav.helse.spesialist.application.snapshot.SnapshotRefusjonselement
import no.nav.helse.spesialist.application.snapshot.SnapshotSkjonnsmessigFastsatt
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadArbeidsgiver
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadArbeidsledig
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadFrilans
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadNav
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadSelvstendig
import no.nav.helse.spesialist.application.snapshot.SnapshotSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagkilde
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagkildetype
import no.nav.helse.spesialist.application.snapshot.SnapshotSykdomsdagtype
import no.nav.helse.spesialist.application.snapshot.SnapshotSykepengegrunnlagsgrense
import no.nav.helse.spesialist.application.snapshot.SnapshotSykmelding
import no.nav.helse.spesialist.application.snapshot.SnapshotTidslinjeperiode
import no.nav.helse.spesialist.application.snapshot.SnapshotUkjentVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsdagType
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsinfo
import no.nav.helse.spesialist.application.snapshot.SnapshotVilkarsgrunnlag
import java.math.BigDecimal
import java.util.UUID

fun SnapshotDag.tilApiDag() =
    ApiDag(
        dato = dato,
        grad = grad,
        kilde = kilde.tilApiKilde(),
        sykdomsdagtype = sykdomsdagtype.tilApiSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilApiUtbetalingsdagtype(),
        utbetalingsinfo = utbetalingsinfo?.tilApiUtbetalingsinfo(),
        begrunnelser = begrunnelser?.map { it.tilApiBegrunnelse() },
    )

private fun SnapshotSykdomsdagkilde.tilApiKilde() =
    ApiKilde(
        id = id,
        type = type.tilApiKildetype(),
    )

private fun SnapshotSykdomsdagkildetype.tilApiKildetype() =
    when (this) {
        SnapshotSykdomsdagkildetype.INNTEKTSMELDING -> ApiKildetype.INNTEKTSMELDING
        SnapshotSykdomsdagkildetype.SAKSBEHANDLER -> ApiKildetype.SAKSBEHANDLER
        SnapshotSykdomsdagkildetype.SOKNAD -> ApiKildetype.SOKNAD
        SnapshotSykdomsdagkildetype.SYKMELDING -> ApiKildetype.SYKMELDING
        else -> ApiKildetype.UKJENT
    }

private fun SnapshotSykdomsdagtype.tilApiSykdomsdagtype() =
    when (this) {
        SnapshotSykdomsdagtype.ARBEIDSDAG -> ApiSykdomsdagtype.ARBEIDSDAG
        SnapshotSykdomsdagtype.ARBEIDSGIVERDAG -> ApiSykdomsdagtype.ARBEIDSGIVERDAG
        SnapshotSykdomsdagtype.AVSLATT -> ApiSykdomsdagtype.AVSLATT
        SnapshotSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG -> ApiSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG
        SnapshotSykdomsdagtype.FERIEDAG -> ApiSykdomsdagtype.FERIEDAG
        SnapshotSykdomsdagtype.FORELDETSYKEDAG -> ApiSykdomsdagtype.FORELDET_SYKEDAG
        SnapshotSykdomsdagtype.FRISKHELGEDAG -> ApiSykdomsdagtype.FRISK_HELGEDAG
        SnapshotSykdomsdagtype.PERMISJONSDAG -> ApiSykdomsdagtype.PERMISJONSDAG
        SnapshotSykdomsdagtype.SYKHELGEDAG -> ApiSykdomsdagtype.SYK_HELGEDAG
        SnapshotSykdomsdagtype.SYKEDAG -> ApiSykdomsdagtype.SYKEDAG
        SnapshotSykdomsdagtype.SYKEDAGNAV -> ApiSykdomsdagtype.SYKEDAG_NAV
        SnapshotSykdomsdagtype.UBESTEMTDAG -> ApiSykdomsdagtype.UBESTEMTDAG
        SnapshotSykdomsdagtype.ANDREYTELSERAAP -> ApiSykdomsdagtype.ANDRE_YTELSER_AAP
        SnapshotSykdomsdagtype.ANDREYTELSERDAGPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_DAGPENGER
        SnapshotSykdomsdagtype.ANDREYTELSERFORELDREPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_FORELDREPENGER
        SnapshotSykdomsdagtype.ANDREYTELSEROMSORGSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_OMSORGSPENGER
        SnapshotSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_OPPLARINGSPENGER
        SnapshotSykdomsdagtype.ANDREYTELSERPLEIEPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_PLEIEPENGER
        SnapshotSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_SVANGERSKAPSPENGER
        else -> throw Exception("Ukjent sykdomsdagtype $name")
    }

private fun SnapshotUtbetalingsdagType.tilApiUtbetalingsdagtype() =
    when (this) {
        SnapshotUtbetalingsdagType.ARBEIDSDAG -> ApiUtbetalingsdagtype.ARBEIDSDAG
        SnapshotUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG -> ApiUtbetalingsdagtype.ARBEIDSGIVERPERIODEDAG
        SnapshotUtbetalingsdagType.AVVISTDAG -> ApiUtbetalingsdagtype.AVVIST_DAG
        SnapshotUtbetalingsdagType.FERIEDAG -> ApiUtbetalingsdagtype.FERIEDAG
        SnapshotUtbetalingsdagType.FORELDETDAG -> ApiUtbetalingsdagtype.FORELDET_DAG
        SnapshotUtbetalingsdagType.HELGEDAG -> ApiUtbetalingsdagtype.HELGEDAG
        SnapshotUtbetalingsdagType.NAVDAG -> ApiUtbetalingsdagtype.NAVDAG
        SnapshotUtbetalingsdagType.NAVHELGDAG -> ApiUtbetalingsdagtype.NAVHELGDAG
        else -> ApiUtbetalingsdagtype.UKJENT_DAG
    }

private fun SnapshotUtbetalingsinfo.tilApiUtbetalingsinfo() =
    ApiUtbetalingsinfo(
        arbeidsgiverbelop = arbeidsgiverbelop,
        inntekt = inntekt,
        personbelop = personbelop,
        refusjonsbelop = refusjonsbelop,
        totalGrad = totalGrad,
        utbetaling = utbetaling,
    )

private fun SnapshotBegrunnelse.tilApiBegrunnelse() =
    when (this) {
        SnapshotBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE -> ApiBegrunnelse.EGENMELDING_UTENFOR_ARBEIDSGIVERPERIODE
        SnapshotBegrunnelse.ETTERDODSDATO -> ApiBegrunnelse.ETTER_DODSDATO
        SnapshotBegrunnelse.MANGLERMEDLEMSKAP -> ApiBegrunnelse.MANGLER_MEDLEMSKAP
        SnapshotBegrunnelse.MANGLEROPPTJENING -> ApiBegrunnelse.MANGLER_OPPTJENING
        SnapshotBegrunnelse.MINIMUMINNTEKT -> ApiBegrunnelse.MINIMUM_INNTEKT
        SnapshotBegrunnelse.MINIMUMINNTEKTOVER67 -> ApiBegrunnelse.MINIMUM_INNTEKT_OVER_67
        SnapshotBegrunnelse.MINIMUMSYKDOMSGRAD -> ApiBegrunnelse.MINIMUM_SYKDOMSGRAD
        SnapshotBegrunnelse.OVER70 -> ApiBegrunnelse.OVER_70
        SnapshotBegrunnelse.SYKEPENGEDAGEROPPBRUKT -> ApiBegrunnelse.SYKEPENGEDAGER_OPPBRUKT
        SnapshotBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67 -> ApiBegrunnelse.SYKEPENGEDAGER_OPPBRUKT_OVER_67
        SnapshotBegrunnelse.ANDREYTELSER -> ApiBegrunnelse.ANDREYTELSER
        SnapshotBegrunnelse.UNKNOWN_VALUE -> ApiBegrunnelse.UKJENT
    }

fun SnapshotHendelse.tilApiHendelse(): ApiHendelse =
    when (this) {
        is SnapshotInntektsmelding ->
            ApiInntektsmelding(
                id = UUID.fromString(id),
                type = ApiHendelsetype.INNTEKTSMELDING,
                mottattDato = mottattDato,
                beregnetInntekt = beregnetInntekt,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is SnapshotSoknadArbeidsgiver ->
            ApiSoknadArbeidsgiver(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_ARBEIDSGIVER,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtArbeidsgiver = sendtArbeidsgiver,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is SnapshotSoknadNav ->
            ApiSoknadNav(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_NAV,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is SnapshotSoknadArbeidsledig ->
            ApiSoknadArbeidsledig(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_ARBEIDSLEDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is SnapshotSoknadFrilans ->
            ApiSoknadFrilans(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_FRILANS,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is SnapshotSoknadSelvstendig ->
            ApiSoknadSelvstendig(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_SELVSTENDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is SnapshotSykmelding ->
            ApiSykmelding(
                id = UUID.fromString(id),
                type = ApiHendelsetype.NY_SOKNAD,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
            )

        is SnapshotInntektFraAOrdningen ->
            ApiInntektHentetFraAOrdningen(
                id = UUID.fromString(id),
                type = ApiHendelsetype.INNTEKT_HENTET_FRA_AORDNINGEN,
                mottattDato = mottattDato,
            )

        else -> throw Exception("Ukjent hendelsestype ${javaClass.name}")
    }

fun SnapshotOmregnetArsinntekt.tilApiOmregnetArsinntekt() =
    ApiOmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = inntekterFraAOrdningen?.map { it.tilApiInntektFraAOrdningen() },
        kilde = kilde.tilApiInntektskilde(),
        manedsbelop = manedsbelop,
    )

fun SnapshotOmregnetArsinntekt.tilApiArsinntekt() =
    ApiArsinntekt(
        belop = BigDecimal(belop),
        inntektFraAOrdningen = inntekterFraAOrdningen?.map { it.tilApiInntektFraAOrdningenV2() },
        kilde = kilde.tilApiInntektskilde(),
        manedsbelop = BigDecimal(manedsbelop),
    )

fun SnapshotSkjonnsmessigFastsatt.tilApiArsinntekt() =
    ApiArsinntekt(
        belop = BigDecimal(belop),
        inntektFraAOrdningen = null,
        kilde = ApiInntektskilde.SKJONNSMESSIG_FASTSATT,
        manedsbelop = BigDecimal(manedsbelop),
    )

fun SnapshotSkjonnsmessigFastsatt.tilApiOmregnetArsinntekt() =
    ApiOmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = null,
        kilde = ApiInntektskilde.SKJONNSMESSIG_FASTSATT,
        manedsbelop = manedsbelop,
    )

private fun SnapshotInntekterFraAOrdningen.tilApiInntektFraAOrdningen() =
    ApiInntektFraAOrdningen(
        maned = maned,
        sum = sum,
    )

private fun SnapshotInntekterFraAOrdningen.tilApiInntektFraAOrdningenV2() =
    ApiInntektFraAOrdningenV2(
        maned = maned,
        sum = BigDecimal(sum),
    )

private fun SnapshotInntektskilde.tilApiInntektskilde() =
    when (this) {
        SnapshotInntektskilde.AORDNINGEN -> ApiInntektskilde.AORDNINGEN
        SnapshotInntektskilde.INFOTRYGD -> ApiInntektskilde.INFOTRYGD
        SnapshotInntektskilde.INNTEKTSMELDING -> ApiInntektskilde.INNTEKTSMELDING
        SnapshotInntektskilde.SAKSBEHANDLER -> ApiInntektskilde.SAKSBEHANDLER
        SnapshotInntektskilde.IKKERAPPORTERT -> ApiInntektskilde.IKKE_RAPPORTERT
        else -> throw Exception("Ukjent inntektskilde ${this.name}")
    }

fun SnapshotArbeidsgiverrefusjon.tilApiArbeidsgiverrefusjon() =
    ApiArbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.map { it.tilApiRefusjonselement() },
    )

fun SnapshotArbeidsgiverrefusjon.tilApiArbeidsgiverrefusjonV2() =
    ApiArbeidsgiverrefusjonV2(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.map { it.tilApiRefusjonselementV2() },
    )

private fun SnapshotRefusjonselement.tilApiRefusjonselement() =
    ApiRefusjonselement(
        fom = fom,
        tom = tom,
        belop = belop,
        meldingsreferanseId = meldingsreferanseId,
    )

private fun SnapshotRefusjonselement.tilApiRefusjonselementV2() =
    ApiRefusjonselementV2(
        fom = fom,
        tom = tom,
        belop = BigDecimal(belop),
        meldingsreferanseId = meldingsreferanseId,
    )

fun SnapshotPeriodetilstand.tilApiPeriodetilstand(erSisteGenerasjon: Boolean) =
    when (this) {
        SnapshotPeriodetilstand.ANNULLERINGFEILET -> ApiPeriodetilstand.AnnulleringFeilet
        SnapshotPeriodetilstand.ANNULLERT -> ApiPeriodetilstand.Annullert
        SnapshotPeriodetilstand.INGENUTBETALING -> ApiPeriodetilstand.IngenUtbetaling
        SnapshotPeriodetilstand.REVURDERINGFEILET -> ApiPeriodetilstand.RevurderingFeilet
        SnapshotPeriodetilstand.TILANNULLERING -> ApiPeriodetilstand.TilAnnullering
        SnapshotPeriodetilstand.TILINFOTRYGD -> ApiPeriodetilstand.TilInfotrygd
        SnapshotPeriodetilstand.TILUTBETALING -> ApiPeriodetilstand.TilUtbetaling
        SnapshotPeriodetilstand.UTBETALT -> ApiPeriodetilstand.Utbetalt
        SnapshotPeriodetilstand.FORBEREDERGODKJENNING -> ApiPeriodetilstand.ForberederGodkjenning
        SnapshotPeriodetilstand.MANGLERINFORMASJON -> ApiPeriodetilstand.ManglerInformasjon
        SnapshotPeriodetilstand.TILGODKJENNING -> ApiPeriodetilstand.TilGodkjenning
        SnapshotPeriodetilstand.UTBETALINGFEILET -> ApiPeriodetilstand.UtbetalingFeilet
        SnapshotPeriodetilstand.VENTERPAANNENPERIODE -> ApiPeriodetilstand.VenterPaEnAnnenPeriode
        SnapshotPeriodetilstand.UTBETALTVENTERPAANNENPERIODE -> {
            if (Toggle.BehandleEnOgEnPeriode.enabled && erSisteGenerasjon) {
                ApiPeriodetilstand.VenterPaEnAnnenPeriode
            } else {
                ApiPeriodetilstand.UtbetaltVenterPaEnAnnenPeriode
            }
        }

        SnapshotPeriodetilstand.AVVENTERINNTEKTSOPPLYSNINGER -> ApiPeriodetilstand.AvventerInntektsopplysninger
        SnapshotPeriodetilstand.TILSKJONNSFASTSETTELSE -> ApiPeriodetilstand.TilSkjonnsfastsettelse
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

fun SnapshotTidslinjeperiode.tilApiPeriodetype() =
    when (periodetype) {
        SnapshotPeriodetype.FORLENGELSE -> ApiPeriodetype.FORLENGELSE
        SnapshotPeriodetype.FORSTEGANGSBEHANDLING -> ApiPeriodetype.FORSTEGANGSBEHANDLING
        SnapshotPeriodetype.INFOTRYGDFORLENGELSE -> ApiPeriodetype.INFOTRYGDFORLENGELSE
        SnapshotPeriodetype.OVERGANGFRAIT -> ApiPeriodetype.OVERGANG_FRA_IT
        else -> throw Exception("Ukjent periodetype $periodetype")
    }

fun SnapshotInntektstype.tilApiInntektstype() =
    when (this) {
        SnapshotInntektstype.ENARBEIDSGIVER -> ApiInntektstype.ENARBEIDSGIVER
        SnapshotInntektstype.FLEREARBEIDSGIVERE -> ApiInntektstype.FLEREARBEIDSGIVERE
        else -> throw Exception("Ukjent inntektstype $this")
    }

fun SnapshotVilkarsgrunnlag.tilVilkarsgrunnlagV2(avviksvurderingRepository: AvviksvurderingRepository): ApiVilkårsgrunnlagV2 {
    return when (this) {
        is SnapshotInfotrygdVilkarsgrunnlag ->
            ApiVilkårsgrunnlagArbeidstakerInfotrygd(
                id = id,
                skjaeringstidspunkt = skjaeringstidspunkt,
                sykepengegrunnlag = BigDecimal(sykepengegrunnlag),
                inntekter =
                    inntekter.map {
                        ApiArbeidstakerinntektInfotrygd(
                            arbeidsgiver = it.arbeidsgiver,
                            omregnetArsinntekt = it.omregnetArsinntekt.tilApiArsinntekt(),
                            deaktivert = it.deaktivert,
                        )
                    },
                arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilApiArbeidsgiverrefusjonV2() },
                omregnetArsinntekt = BigDecimal(omregnetArsinntekt),
            )

        is SnapshotSpleisVilkarsgrunnlag -> {
            val avviksvurdering = avviksvurderingRepository.hentAvviksvurdering(id)

            return ApiVilkårsgrunnlagArbeidstakerSpleis(
                id = id,
                skjaeringstidspunkt = skjaeringstidspunkt,
                sykepengegrunnlag = BigDecimal(sykepengegrunnlag),
                inntekter =
                    if (avviksvurdering == null) {
                        inntekter.map {
                            ApiArbeidstakerinntektSpleisAvventerAvviksvurdering(
                                arbeidsgiver = it.arbeidsgiver,
                                omregnetArsinntekt = it.omregnetArsinntekt.tilApiArsinntekt(),
                                deaktivert = it.deaktivert,
                                fom = it.fom,
                                tom = it.tom,
                            )
                        }
                    } else {
                        tilInntekter(inntekter, avviksvurdering)
                    },
                arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilApiArbeidsgiverrefusjonV2() },
                avviksvurdering =
                    avviksvurdering?.let {
                        ApiVilkårsgrunnlagAvviksvurdering(
                            avviksprosent = BigDecimal(it.avviksprosent),
                            omregnetArsinntekt = BigDecimal(it.beregningsgrunnlag.totalbeløp),
                            sammenligningsgrunnlag = BigDecimal(it.sammenligningsgrunnlag.totalbeløp),
                        )
                    },
                kravOmMedlemskap = oppfyllerKravOmMedlemskap.tilApiVilkårsgrunnlagKrav(),
                kravOmMinstelonn = oppfyllerKravOmMinstelonn.tilApiVilkårsgrunnlagKravAlltidVurdert(),
                kravOmOpptjening = oppfyllerKravOmOpptjening.tilApiVilkårsgrunnlagKravAlltidVurdert(),
                skjonnsmessigFastsattAarlig = skjonnsmessigFastsattAarlig?.let { BigDecimal(it) },
                grunnbelop = grunnbelop,
                sykepengegrunnlagsgrense = sykepengegrunnlagsgrense.tilSykepengegrunnlaggrense(),
                antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
                opptjeningFra = opptjeningFra,
            )
        }
        is SnapshotUkjentVilkarsgrunnlag -> throw Exception("Ukjent vilkårsgrunnlag ${this.javaClass.name}")
    }
}

private fun tilInntekter(
    inntekter: List<SnapshotArbeidsgiverinntekt>,
    avviksvurdering: Avviksvurdering,
): List<ApiArbeidstakerinntektSpleis> {
    val arbeidsgivereMedInntekter =
        (
            avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.map { it.arbeidsgiverreferanse } +
                inntekter.map { it.arbeidsgiver }
        ).toSet()

    return arbeidsgivereMedInntekter.map { arbeidsgiverreferanse ->
        val inntektFraSpleis =
            inntekter.singleOrNull { inntektFraSpleis -> inntektFraSpleis.arbeidsgiver == arbeidsgiverreferanse }
        val sammenligningsgrunnlagInntekt =
            avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter.singleOrNull { it.arbeidsgiverreferanse == arbeidsgiverreferanse }
        ApiArbeidstakerinntektSpleis(
            arbeidsgiver = arbeidsgiverreferanse,
            omregnetArsinntekt = inntektFraSpleis?.omregnetArsinntekt?.tilApiArsinntekt(),
            sammenligningsgrunnlag =
                sammenligningsgrunnlagInntekt?.let {
                    ApiSammenligningsgrunnlagV2(
                        belop = sammenligningsgrunnlagInntekt.inntekter.map { BigDecimal(it.beløp) }.sumOf { it },
                        inntektFraAOrdningen =
                            sammenligningsgrunnlagInntekt.inntekter.map { inntekt ->
                                ApiInntektFraAOrdningenV2(
                                    maned = inntekt.årMåned,
                                    sum = BigDecimal(inntekt.beløp),
                                )
                            },
                    )
                },
            skjonnsmessigFastsatt = inntektFraSpleis?.skjonnsmessigFastsatt?.tilApiArsinntekt(),
            deaktivert = inntektFraSpleis?.deaktivert,
            fom = inntektFraSpleis?.fom,
            tom = inntektFraSpleis?.tom,
        )
    }
}

private fun Boolean?.tilApiVilkårsgrunnlagKrav(): ApiVilkårsgrunnlagKrav =
    when (this) {
        true -> ApiVilkårsgrunnlagKrav.OPPFYLT
        false -> ApiVilkårsgrunnlagKrav.IKKE_OPPFYLT
        null -> ApiVilkårsgrunnlagKrav.IKKE_VURDERT
    }

private fun Boolean.tilApiVilkårsgrunnlagKravAlltidVurdert(): ApiVilkårsgrunnlagKravAlltidVurdert =
    when (this) {
        true -> ApiVilkårsgrunnlagKravAlltidVurdert.OPPFYLT
        false -> ApiVilkårsgrunnlagKravAlltidVurdert.IKKE_OPPFYLT
    }

fun SnapshotVilkarsgrunnlag.tilVilkarsgrunnlag(avviksvurderingRepository: AvviksvurderingRepository): ApiVilkårsgrunnlag {
    return when (this) {
        is SnapshotSpleisVilkarsgrunnlag -> {
            val avviksvurdering: Avviksvurdering =
                checkNotNull(avviksvurderingRepository.hentAvviksvurdering(id)) { "Fant ikke avviksvurdering for vilkårsgrunnlagId $id" }
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

        is SnapshotInfotrygdVilkarsgrunnlag ->
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

internal fun SnapshotSykepengegrunnlagsgrense.tilSykepengegrunnlaggrense() =
    ApiSykepengegrunnlagsgrense(
        grunnbelop,
        grense,
        virkningstidspunkt,
    )
