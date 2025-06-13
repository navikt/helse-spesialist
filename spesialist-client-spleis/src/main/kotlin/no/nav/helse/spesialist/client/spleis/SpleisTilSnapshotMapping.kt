package no.nav.helse.spesialist.client.spleis

import no.nav.helse.spesialist.application.snapshot.SnapshotAlder
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiver
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverrefusjon
import no.nav.helse.spesialist.application.snapshot.SnapshotBegrunnelse
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotDag
import no.nav.helse.spesialist.application.snapshot.SnapshotGenerasjon
import no.nav.helse.spesialist.application.snapshot.SnapshotGhostPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotHendelsetype
import no.nav.helse.spesialist.application.snapshot.SnapshotInfotrygdVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektFraAOrdningen
import no.nav.helse.spesialist.application.snapshot.SnapshotInntekterFraAOrdningen
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektskilde
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektsmelding
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektstype
import no.nav.helse.spesialist.application.snapshot.SnapshotOmregnetArsinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotOppdrag
import no.nav.helse.spesialist.application.snapshot.SnapshotPensjonsgivendeInntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetilstand
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetype
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodevilkar
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import no.nav.helse.spesialist.application.snapshot.SnapshotRefusjonselement
import no.nav.helse.spesialist.application.snapshot.SnapshotSimulering
import no.nav.helse.spesialist.application.snapshot.SnapshotSimuleringsdetaljer
import no.nav.helse.spesialist.application.snapshot.SnapshotSimuleringsperiode
import no.nav.helse.spesialist.application.snapshot.SnapshotSimuleringsutbetaling
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
import no.nav.helse.spesialist.application.snapshot.SnapshotSykepengedager
import no.nav.helse.spesialist.application.snapshot.SnapshotSykepengegrunnlagsgrense
import no.nav.helse.spesialist.application.snapshot.SnapshotSykmelding
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotUkjentHendelse
import no.nav.helse.spesialist.application.snapshot.SnapshotUkjentTidslinjeperiode
import no.nav.helse.spesialist.application.snapshot.SnapshotUkjentVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetaling
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsdagType
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsinfo
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingslinje
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingstatus
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingtype
import no.nav.helse.spesialist.application.snapshot.SnapshotVurdering
import no.nav.helse.spleis.graphql.enums.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.enums.GraphQLHendelsetype
import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.DefaultGraphQLHendelseImplementation
import no.nav.helse.spleis.graphql.hentsnapshot.DefaultGraphQLTidslinjeperiodeImplementation
import no.nav.helse.spleis.graphql.hentsnapshot.DefaultGraphQLVilkarsgrunnlagImplementation
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLDag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPensjonsgivendeInntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLRefusjonselement
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimulering
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsdetaljer
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsperiode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsutbetaling
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
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingsinfo
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingslinje
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import java.math.BigDecimal

fun Alder.tilSnapshotAlder() =
    SnapshotAlder(
        alderSisteSykedag = alderSisteSykedag,
        oppfylt = oppfylt,
    )

fun GraphQLArbeidsgiver.tilSnapshotArbeidsgiver() =
    SnapshotArbeidsgiver(
        organisasjonsnummer = organisasjonsnummer,
        ghostPerioder = ghostPerioder.map { it.tilSnapshotGhostPeriode() },
        generasjoner = generasjoner.map { it.tilSnapshotGenerasjon() },
    )

fun GraphQLArbeidsgiverinntekt.tilSnapshotArbeidsgiverinntekt() =
    SnapshotArbeidsgiverinntekt(
        arbeidsgiver = arbeidsgiver,
        omregnetArsinntekt = omregnetArsinntekt.tilSnapshotOmregnetArsinntekt(),
        skjonnsmessigFastsatt = skjonnsmessigFastsatt?.tilSnapshotSkjonnsmessigFastsatt(),
        deaktivert = deaktivert,
        fom = fom,
        tom = tom,
    )

fun GraphQLArbeidsgiverrefusjon.tilSnapshotArbeidsgiverrefusjon() =
    SnapshotArbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.map { it.tilSnapshotRefusjonselement() },
    )

fun GraphQLDag.tilSnapshotDag() =
    SnapshotDag(
        begrunnelser = begrunnelser?.map { it.tilSnapshotBegrunnelse() },
        dato = dato,
        grad = grad,
        kilde = kilde.tilSnapshotSykdomsdagkilde(),
        sykdomsdagtype = sykdomsdagtype.tilSnapshotSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilSnapshotUtbetalingsdagType(),
        utbetalingsinfo = utbetalingsinfo?.tilSnapshotUtbetalingsinfo(),
    )

fun GraphQLGenerasjon.tilSnapshotGenerasjon() =
    SnapshotGenerasjon(
        id = id,
        perioder = perioder.map { it.tilSnapshotTidslinjeperiode() },
    )

fun GraphQLGhostPeriode.tilSnapshotGhostPeriode() =
    SnapshotGhostPeriode(
        fom = fom,
        tom = tom,
        skjaeringstidspunkt = skjaeringstidspunkt,
        vilkarsgrunnlagId = vilkarsgrunnlagId,
        deaktivert = deaktivert,
    )

fun GraphQLInntektsmelding.tilSnapshotInntektsmelding() =
    SnapshotInntektsmelding(
        beregnetInntekt = beregnetInntekt,
        id = id,
        mottattDato = mottattDato,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun GraphQLSoknadArbeidsgiver.tilSnapshotSoknadArbeidsgiver() =
    SnapshotSoknadArbeidsgiver(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtArbeidsgiver = sendtArbeidsgiver,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun GraphQLSoknadNav.tilSnapshotSoknadNav() =
    SnapshotSoknadNav(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun GraphQLSoknadArbeidsledig.tilSnapshotSoknadArbeidsledig() =
    SnapshotSoknadArbeidsledig(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun GraphQLSoknadFrilans.tilSnapshotSoknadFrilans() =
    SnapshotSoknadFrilans(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun GraphQLSoknadSelvstendig.tilSnapshotSoknadSelvstendig() =
    SnapshotSoknadSelvstendig(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun GraphQLSykmelding.tilSnapshotSykmelding() =
    SnapshotSykmelding(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
    )

fun GraphQLInntektFraAOrdningen.tilSnapshotInntektFraAOrdningen() =
    SnapshotInntektFraAOrdningen(
        id = id,
        mottattDato = mottattDato,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

fun DefaultGraphQLHendelseImplementation.tilSnapshotUkjentHendelse() = SnapshotUkjentHendelse

fun GraphQLInntekterFraAOrdningen.tilSnapshotInntekterFraAOrdningen() =
    SnapshotInntekterFraAOrdningen(
        maned = maned,
        sum = sum,
    )

fun GraphQLOmregnetArsinntekt.tilSnapshotOmregnetArsinntekt() =
    SnapshotOmregnetArsinntekt(
        belop = belop,
        inntekterFraAOrdningen = inntekterFraAOrdningen?.map { it.tilSnapshotInntekterFraAOrdningen() },
        kilde = kilde.tilSnapshotInntektskilde(),
        manedsbelop = manedsbelop,
    )

fun GraphQLOppdrag.tilSnapshotOppdrag() =
    SnapshotOppdrag(
        fagsystemId = fagsystemId,
        tidsstempel = tidsstempel,
        utbetalingslinjer = utbetalingslinjer.map { it.tilSnapshotUtbetalingslinje() },
        simulering = simulering?.tilSnapshotSimulering(),
    )

fun GraphQLPeriodevilkar.tilSnapshotPeriodevilkar() =
    SnapshotPeriodevilkar(
        alder = alder.tilSnapshotAlder(),
        sykepengedager = sykepengedager.tilSnapshotSykepengedager(),
    )

fun GraphQLPerson.tilSnapshotPerson() =
    SnapshotPerson(
        aktorId = aktorId,
        arbeidsgivere = arbeidsgivere.map { it.tilSnapshotArbeidsgiver() },
        dodsdato = dodsdato,
        fodselsnummer = fodselsnummer,
        versjon = versjon,
        vilkarsgrunnlag = vilkarsgrunnlag.map { it.tilSnapshotVilkarsgrunnlag() },
    )

fun GraphQLRefusjonselement.tilSnapshotRefusjonselement() =
    SnapshotRefusjonselement(
        fom = fom,
        tom = tom,
        belop = belop,
        meldingsreferanseId = meldingsreferanseId,
    )

fun GraphQLSimulering.tilSnapshotSimulering() =
    SnapshotSimulering(
        totalbelop = totalbelop,
        perioder = perioder.map { it.tilSnapshotSimuleringsperiode() },
    )

fun GraphQLSimuleringsdetaljer.tilSnapshotSimuleringsdetaljer() =
    SnapshotSimuleringsdetaljer(
        belop = belop,
        antallSats = antallSats,
        faktiskFom = faktiskFom,
        faktiskTom = faktiskTom,
        klassekode = klassekode,
        klassekodeBeskrivelse = klassekodeBeskrivelse,
        konto = konto,
        refunderesOrgNr = refunderesOrgNr,
        sats = sats,
        tilbakeforing = tilbakeforing,
        typeSats = typeSats,
        uforegrad = uforegrad,
        utbetalingstype = utbetalingstype,
    )

fun GraphQLSimuleringsperiode.tilSnapshotSimuleringsperiode() =
    SnapshotSimuleringsperiode(
        fom = fom,
        tom = tom,
        utbetalinger = utbetalinger.map { it.tilSnapshotSimuleringsutbetaling() },
    )

fun GraphQLSimuleringsutbetaling.tilSnapshotSimuleringsutbetaling() =
    SnapshotSimuleringsutbetaling(
        detaljer = detaljer.map { it.tilSnapshotSimuleringsdetaljer() },
        feilkonto = feilkonto,
        forfall = forfall,
        utbetalesTilId = utbetalesTilId,
        utbetalesTilNavn = utbetalesTilNavn,
    )

fun GraphQLSkjonnsmessigFastsatt.tilSnapshotSkjonnsmessigFastsatt() =
    SnapshotSkjonnsmessigFastsatt(
        belop = belop,
        manedsbelop = manedsbelop,
    )

fun GraphQLSykdomsdagkilde.tilSnapshotSykdomsdagkilde() =
    SnapshotSykdomsdagkilde(
        id = id,
        type = type.tilSnapshotSykdomsdagkildetype(),
    )

fun Sykepengedager.tilSnapshotSykepengedager() =
    SnapshotSykepengedager(
        forbrukteSykedager = forbrukteSykedager,
        gjenstaendeSykedager = gjenstaendeSykedager,
        maksdato = maksdato,
        oppfylt = oppfylt,
        skjaeringstidspunkt = skjaeringstidspunkt,
    )

fun GraphQLSykepengegrunnlagsgrense.tilSnapshotSykepengegrunnlagsgrense() =
    SnapshotSykepengegrunnlagsgrense(
        grunnbelop = grunnbelop,
        grense = grense,
        virkningstidspunkt = virkningstidspunkt,
    )

fun GraphQLTidslinjeperiode.tilSnapshotTidslinjeperiode() =
    when (this) {
        is GraphQLUberegnetPeriode -> tilSnapshotUberegnetPeriode()
        is GraphQLBeregnetPeriode -> tilSnapshotBeregnetPeriode()
        is DefaultGraphQLTidslinjeperiodeImplementation -> tilSnapshotUkjentTidslinjeperiode()
        else -> error("Uhåndtert tidslinjeperiodetype: ${this.javaClass.simpleName}")
    }

fun GraphQLUberegnetPeriode.tilSnapshotUberegnetPeriode() =
    SnapshotUberegnetPeriode(
        behandlingId = behandlingId,
        erForkastet = erForkastet,
        fom = fom,
        tom = tom,
        inntektstype = inntektstype.tilSnapshotInntektstype(),
        opprettet = opprettet,
        periodetype = periodetype.tilSnapshotPeriodetype(),
        periodetilstand = periodetilstand.tilSnapshotPeriodetilstand(),
        skjaeringstidspunkt = skjaeringstidspunkt,
        tidslinje = tidslinje.map { it.tilSnapshotDag() },
        hendelser = hendelser.map { it.tilSnapshotHendelse() },
        vedtaksperiodeId = vedtaksperiodeId,
    )

fun GraphQLBeregnetPeriode.tilSnapshotBeregnetPeriode() =
    SnapshotBeregnetPeriode(
        behandlingId = behandlingId,
        erForkastet = erForkastet,
        fom = fom,
        tom = tom,
        inntektstype = inntektstype.tilSnapshotInntektstype(),
        opprettet = opprettet,
        periodetype = periodetype.tilSnapshotPeriodetype(),
        periodetilstand = periodetilstand.tilSnapshotPeriodetilstand(),
        skjaeringstidspunkt = skjaeringstidspunkt,
        tidslinje = tidslinje.map { it.tilSnapshotDag() },
        hendelser = hendelser.map { it.tilSnapshotHendelse() },
        vedtaksperiodeId = vedtaksperiodeId,
        beregningId = beregningId,
        forbrukteSykedager = forbrukteSykedager,
        gjenstaendeSykedager = gjenstaendeSykedager,
        maksdato = maksdato,
        periodevilkar = periodevilkar.tilSnapshotPeriodevilkar(),
        utbetaling = utbetaling.tilSnapshotUtbetaling(),
        vilkarsgrunnlagId = vilkarsgrunnlagId,
        pensjonsgivendeInntekter = pensjonsgivendeInntekter.map { it.tilSnapshotPensjonsgivendeInntekt() },
    )

fun DefaultGraphQLTidslinjeperiodeImplementation.tilSnapshotUkjentTidslinjeperiode() =
    SnapshotUkjentTidslinjeperiode(
        behandlingId = behandlingId,
        erForkastet = erForkastet,
        fom = fom,
        tom = tom,
        inntektstype = inntektstype.tilSnapshotInntektstype(),
        opprettet = opprettet,
        periodetype = periodetype.tilSnapshotPeriodetype(),
        periodetilstand = periodetilstand.tilSnapshotPeriodetilstand(),
        skjaeringstidspunkt = skjaeringstidspunkt,
        tidslinje = tidslinje.map { it.tilSnapshotDag() },
        hendelser = hendelser.map { it.tilSnapshotHendelse() },
        vedtaksperiodeId = vedtaksperiodeId,
    )

fun GraphQLHendelse.tilSnapshotHendelse() =
    when (this) {
        is GraphQLInntektsmelding -> tilSnapshotInntektsmelding()
        is GraphQLSoknadArbeidsgiver -> tilSnapshotSoknadArbeidsgiver()
        is GraphQLSoknadNav -> tilSnapshotSoknadNav()
        is GraphQLSoknadArbeidsledig -> tilSnapshotSoknadArbeidsledig()
        is GraphQLSoknadFrilans -> tilSnapshotSoknadFrilans()
        is GraphQLSoknadSelvstendig -> tilSnapshotSoknadSelvstendig()
        is GraphQLSykmelding -> tilSnapshotSykmelding()
        is GraphQLInntektFraAOrdningen -> tilSnapshotInntektFraAOrdningen()
        is DefaultGraphQLHendelseImplementation -> tilSnapshotUkjentHendelse()
        else -> error("Uhåndtert hendelsetype: ${this.javaClass.simpleName}")
    }

fun GraphQLUtbetaling.tilSnapshotUtbetaling() =
    SnapshotUtbetaling(
        id = id,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        arbeidsgiverNettoBelop = arbeidsgiverNettoBelop,
        personFagsystemId = personFagsystemId,
        personNettoBelop = personNettoBelop,
        statusEnum = statusEnum.tilSnapshotUtbetalingstatus(),
        typeEnum = typeEnum.tilSnapshotUtbetalingtype(),
        vurdering = vurdering?.tilSnapshotVurdering(),
        personoppdrag = personoppdrag?.tilSnapshotOppdrag(),
        arbeidsgiveroppdrag = arbeidsgiveroppdrag?.tilSnapshotOppdrag(),
    )

fun GraphQLUtbetalingsinfo.tilSnapshotUtbetalingsinfo() =
    SnapshotUtbetalingsinfo(
        arbeidsgiverbelop = arbeidsgiverbelop,
        inntekt = inntekt,
        personbelop = personbelop,
        refusjonsbelop = refusjonsbelop,
        totalGrad = totalGrad,
        utbetaling = utbetaling,
    )

fun GraphQLUtbetalingslinje.tilSnapshotUtbetalingslinje() =
    SnapshotUtbetalingslinje(
        tom = tom,
        fom = fom,
        grad = grad,
        dagsats = dagsats,
    )

fun GraphQLVilkarsgrunnlag.tilSnapshotVilkarsgrunnlag() =
    when (this) {
        is GraphQLInfotrygdVilkarsgrunnlag -> tilSnapshotInfotrygdVilkarsgrunnlag()
        is GraphQLSpleisVilkarsgrunnlag -> tilSnapshotSpleisVilkarsgrunnlag()
        is DefaultGraphQLVilkarsgrunnlagImplementation -> tilSnapshotUkjentVilkarsgrunnlag()
        else -> error("Uhåndtert vilkårsgrunnlagtype: ${this.javaClass.simpleName}")
    }

fun GraphQLInfotrygdVilkarsgrunnlag.tilSnapshotInfotrygdVilkarsgrunnlag() =
    SnapshotInfotrygdVilkarsgrunnlag(
        id = id,
        inntekter = inntekter.map { it.tilSnapshotArbeidsgiverinntekt() },
        arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilSnapshotArbeidsgiverrefusjon() },
        omregnetArsinntekt = omregnetArsinntekt,
        skjaeringstidspunkt = skjaeringstidspunkt,
        sykepengegrunnlag = sykepengegrunnlag,
    )

fun GraphQLSpleisVilkarsgrunnlag.tilSnapshotSpleisVilkarsgrunnlag() =
    SnapshotSpleisVilkarsgrunnlag(
        id = id,
        inntekter = inntekter.map { it.tilSnapshotArbeidsgiverinntekt() },
        arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilSnapshotArbeidsgiverrefusjon() },
        omregnetArsinntekt = omregnetArsinntekt,
        skjaeringstidspunkt = skjaeringstidspunkt,
        sykepengegrunnlag = sykepengegrunnlag,
        antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
        skjonnsmessigFastsattAarlig = skjonnsmessigFastsattAarlig,
        grunnbelop = grunnbelop,
        sykepengegrunnlagsgrense = sykepengegrunnlagsgrense.tilSnapshotSykepengegrunnlagsgrense(),
        oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap,
        oppfyllerKravOmMinstelonn = oppfyllerKravOmMinstelonn,
        oppfyllerKravOmOpptjening = oppfyllerKravOmOpptjening,
        opptjeningFra = opptjeningFra,
        beregingsgrunnlag = BigDecimal.valueOf(beregningsgrunnlag),
    )

fun DefaultGraphQLVilkarsgrunnlagImplementation.tilSnapshotUkjentVilkarsgrunnlag() =
    SnapshotUkjentVilkarsgrunnlag(
        id = id,
        inntekter = inntekter.map { it.tilSnapshotArbeidsgiverinntekt() },
        arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilSnapshotArbeidsgiverrefusjon() },
        omregnetArsinntekt = omregnetArsinntekt,
        skjaeringstidspunkt = skjaeringstidspunkt,
        sykepengegrunnlag = sykepengegrunnlag,
    )

fun GraphQLVurdering.tilSnapshotVurdering() =
    SnapshotVurdering(
        automatisk = automatisk,
        godkjent = godkjent,
        ident = ident,
        tidsstempel = tidsstempel,
    )

fun GraphQLPensjonsgivendeInntekt.tilSnapshotPensjonsgivendeInntekt(): SnapshotPensjonsgivendeInntekt =
    SnapshotPensjonsgivendeInntekt(
        arligBelop = BigDecimal.valueOf(arligBelop),
        inntektsar = inntektsar,
    )

fun GraphQLBegrunnelse.tilSnapshotBegrunnelse() =
    when (this) {
        GraphQLBegrunnelse.ANDREYTELSER -> SnapshotBegrunnelse.ANDREYTELSER
        GraphQLBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE -> SnapshotBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE
        GraphQLBegrunnelse.ETTERDODSDATO -> SnapshotBegrunnelse.ETTERDODSDATO
        GraphQLBegrunnelse.MANGLERMEDLEMSKAP -> SnapshotBegrunnelse.MANGLERMEDLEMSKAP
        GraphQLBegrunnelse.MANGLEROPPTJENING -> SnapshotBegrunnelse.MANGLEROPPTJENING
        GraphQLBegrunnelse.MINIMUMINNTEKT -> SnapshotBegrunnelse.MINIMUMINNTEKT
        GraphQLBegrunnelse.MINIMUMINNTEKTOVER67 -> SnapshotBegrunnelse.MINIMUMINNTEKTOVER67
        GraphQLBegrunnelse.MINIMUMSYKDOMSGRAD -> SnapshotBegrunnelse.MINIMUMSYKDOMSGRAD
        GraphQLBegrunnelse.OVER70 -> SnapshotBegrunnelse.OVER70
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKT -> SnapshotBegrunnelse.SYKEPENGEDAGEROPPBRUKT
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67 -> SnapshotBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67
        GraphQLBegrunnelse.__UNKNOWN_VALUE -> SnapshotBegrunnelse.UNKNOWN_VALUE
    }

fun GraphQLHendelsetype.tilSnapshotHendelsetype() =
    when (this) {
        GraphQLHendelsetype.INNTEKTFRAAORDNINGEN -> SnapshotHendelsetype.INNTEKTFRAAORDNINGEN
        GraphQLHendelsetype.INNTEKTSMELDING -> SnapshotHendelsetype.INNTEKTSMELDING
        GraphQLHendelsetype.NYSOKNAD -> SnapshotHendelsetype.NYSOKNAD
        GraphQLHendelsetype.SENDTSOKNADARBEIDSGIVER -> SnapshotHendelsetype.SENDTSOKNADARBEIDSGIVER
        GraphQLHendelsetype.SENDTSOKNADARBEIDSLEDIG -> SnapshotHendelsetype.SENDTSOKNADARBEIDSLEDIG
        GraphQLHendelsetype.SENDTSOKNADFRILANS -> SnapshotHendelsetype.SENDTSOKNADFRILANS
        GraphQLHendelsetype.SENDTSOKNADNAV -> SnapshotHendelsetype.SENDTSOKNADNAV
        GraphQLHendelsetype.SENDTSOKNADSELVSTENDIG -> SnapshotHendelsetype.SENDTSOKNADSELVSTENDIG
        GraphQLHendelsetype.UKJENT -> SnapshotHendelsetype.UKJENT
        GraphQLHendelsetype.__UNKNOWN_VALUE -> SnapshotHendelsetype.UNKNOWN_VALUE
    }

fun GraphQLInntektskilde.tilSnapshotInntektskilde() =
    when (this) {
        GraphQLInntektskilde.AORDNINGEN -> SnapshotInntektskilde.AORDNINGEN
        GraphQLInntektskilde.IKKERAPPORTERT -> SnapshotInntektskilde.IKKERAPPORTERT
        GraphQLInntektskilde.INFOTRYGD -> SnapshotInntektskilde.INFOTRYGD
        GraphQLInntektskilde.INNTEKTSMELDING -> SnapshotInntektskilde.INNTEKTSMELDING
        GraphQLInntektskilde.SAKSBEHANDLER -> SnapshotInntektskilde.SAKSBEHANDLER
        GraphQLInntektskilde.__UNKNOWN_VALUE -> SnapshotInntektskilde.UNKNOWN_VALUE
    }

fun GraphQLInntektstype.tilSnapshotInntektstype() =
    when (this) {
        GraphQLInntektstype.ENARBEIDSGIVER -> SnapshotInntektstype.ENARBEIDSGIVER
        GraphQLInntektstype.FLEREARBEIDSGIVERE -> SnapshotInntektstype.FLEREARBEIDSGIVERE
        GraphQLInntektstype.__UNKNOWN_VALUE -> SnapshotInntektstype.UNKNOWN_VALUE
    }

fun GraphQLPeriodetilstand.tilSnapshotPeriodetilstand() =
    when (this) {
        GraphQLPeriodetilstand.ANNULLERINGFEILET -> SnapshotPeriodetilstand.ANNULLERINGFEILET
        GraphQLPeriodetilstand.ANNULLERT -> SnapshotPeriodetilstand.ANNULLERT
        GraphQLPeriodetilstand.AVVENTERINNTEKTSOPPLYSNINGER -> SnapshotPeriodetilstand.AVVENTERINNTEKTSOPPLYSNINGER
        GraphQLPeriodetilstand.FORBEREDERGODKJENNING -> SnapshotPeriodetilstand.FORBEREDERGODKJENNING
        GraphQLPeriodetilstand.INGENUTBETALING -> SnapshotPeriodetilstand.INGENUTBETALING
        GraphQLPeriodetilstand.MANGLERINFORMASJON -> SnapshotPeriodetilstand.MANGLERINFORMASJON
        GraphQLPeriodetilstand.REVURDERINGFEILET -> SnapshotPeriodetilstand.REVURDERINGFEILET
        GraphQLPeriodetilstand.TILANNULLERING -> SnapshotPeriodetilstand.TILANNULLERING
        GraphQLPeriodetilstand.TILGODKJENNING -> SnapshotPeriodetilstand.TILGODKJENNING
        GraphQLPeriodetilstand.TILINFOTRYGD -> SnapshotPeriodetilstand.TILINFOTRYGD
        GraphQLPeriodetilstand.TILSKJONNSFASTSETTELSE -> SnapshotPeriodetilstand.TILSKJONNSFASTSETTELSE
        GraphQLPeriodetilstand.TILUTBETALING -> SnapshotPeriodetilstand.TILUTBETALING
        GraphQLPeriodetilstand.UTBETALINGFEILET -> SnapshotPeriodetilstand.UTBETALINGFEILET
        GraphQLPeriodetilstand.UTBETALT -> SnapshotPeriodetilstand.UTBETALT
        GraphQLPeriodetilstand.UTBETALTVENTERPAANNENPERIODE -> SnapshotPeriodetilstand.UTBETALTVENTERPAANNENPERIODE
        GraphQLPeriodetilstand.VENTERPAANNENPERIODE -> SnapshotPeriodetilstand.VENTERPAANNENPERIODE
        GraphQLPeriodetilstand.__UNKNOWN_VALUE -> SnapshotPeriodetilstand.UNKNOWN_VALUE
    }

fun GraphQLPeriodetype.tilSnapshotPeriodetype() =
    when (this) {
        GraphQLPeriodetype.FORLENGELSE -> SnapshotPeriodetype.FORLENGELSE
        GraphQLPeriodetype.FORSTEGANGSBEHANDLING -> SnapshotPeriodetype.FORSTEGANGSBEHANDLING
        GraphQLPeriodetype.INFOTRYGDFORLENGELSE -> SnapshotPeriodetype.INFOTRYGDFORLENGELSE
        GraphQLPeriodetype.OVERGANGFRAIT -> SnapshotPeriodetype.OVERGANGFRAIT
        GraphQLPeriodetype.__UNKNOWN_VALUE -> SnapshotPeriodetype.UNKNOWN_VALUE
    }

fun GraphQLSykdomsdagkildetype.tilSnapshotSykdomsdagkildetype() =
    when (this) {
        GraphQLSykdomsdagkildetype.INNTEKTSMELDING -> SnapshotSykdomsdagkildetype.INNTEKTSMELDING
        GraphQLSykdomsdagkildetype.SAKSBEHANDLER -> SnapshotSykdomsdagkildetype.SAKSBEHANDLER
        GraphQLSykdomsdagkildetype.SOKNAD -> SnapshotSykdomsdagkildetype.SOKNAD
        GraphQLSykdomsdagkildetype.SYKMELDING -> SnapshotSykdomsdagkildetype.SYKMELDING
        GraphQLSykdomsdagkildetype.UKJENT -> SnapshotSykdomsdagkildetype.UKJENT
        GraphQLSykdomsdagkildetype.__UNKNOWN_VALUE -> SnapshotSykdomsdagkildetype.UNKNOWN_VALUE
    }

fun GraphQLSykdomsdagtype.tilSnapshotSykdomsdagtype() =
    when (this) {
        GraphQLSykdomsdagtype.ANDREYTELSERAAP -> SnapshotSykdomsdagtype.ANDREYTELSERAAP
        GraphQLSykdomsdagtype.ANDREYTELSERDAGPENGER -> SnapshotSykdomsdagtype.ANDREYTELSERDAGPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERFORELDREPENGER -> SnapshotSykdomsdagtype.ANDREYTELSERFORELDREPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROMSORGSPENGER -> SnapshotSykdomsdagtype.ANDREYTELSEROMSORGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER -> SnapshotSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERPLEIEPENGER -> SnapshotSykdomsdagtype.ANDREYTELSERPLEIEPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER -> SnapshotSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER
        GraphQLSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG -> SnapshotSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG
        GraphQLSykdomsdagtype.ARBEIDSDAG -> SnapshotSykdomsdagtype.ARBEIDSDAG
        GraphQLSykdomsdagtype.ARBEIDSGIVERDAG -> SnapshotSykdomsdagtype.ARBEIDSGIVERDAG
        GraphQLSykdomsdagtype.AVSLATT -> SnapshotSykdomsdagtype.AVSLATT
        GraphQLSykdomsdagtype.FERIEDAG -> SnapshotSykdomsdagtype.FERIEDAG
        GraphQLSykdomsdagtype.FORELDETSYKEDAG -> SnapshotSykdomsdagtype.FORELDETSYKEDAG
        GraphQLSykdomsdagtype.FRISKHELGEDAG -> SnapshotSykdomsdagtype.FRISKHELGEDAG
        GraphQLSykdomsdagtype.PERMISJONSDAG -> SnapshotSykdomsdagtype.PERMISJONSDAG
        GraphQLSykdomsdagtype.SYKHELGEDAG -> SnapshotSykdomsdagtype.SYKHELGEDAG
        GraphQLSykdomsdagtype.SYKEDAG -> SnapshotSykdomsdagtype.SYKEDAG
        GraphQLSykdomsdagtype.SYKEDAGNAV -> SnapshotSykdomsdagtype.SYKEDAGNAV
        GraphQLSykdomsdagtype.UBESTEMTDAG -> SnapshotSykdomsdagtype.UBESTEMTDAG
        GraphQLSykdomsdagtype.__UNKNOWN_VALUE -> SnapshotSykdomsdagtype.UNKNOWN_VALUE
    }

fun GraphQLUtbetalingsdagType.tilSnapshotUtbetalingsdagType() =
    when (this) {
        GraphQLUtbetalingsdagType.ARBEIDSDAG -> SnapshotUtbetalingsdagType.ARBEIDSDAG
        GraphQLUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG -> SnapshotUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG
        GraphQLUtbetalingsdagType.AVVISTDAG -> SnapshotUtbetalingsdagType.AVVISTDAG
        GraphQLUtbetalingsdagType.FERIEDAG -> SnapshotUtbetalingsdagType.FERIEDAG
        GraphQLUtbetalingsdagType.FORELDETDAG -> SnapshotUtbetalingsdagType.FORELDETDAG
        GraphQLUtbetalingsdagType.HELGEDAG -> SnapshotUtbetalingsdagType.HELGEDAG
        GraphQLUtbetalingsdagType.NAVDAG -> SnapshotUtbetalingsdagType.NAVDAG
        GraphQLUtbetalingsdagType.NAVHELGDAG -> SnapshotUtbetalingsdagType.NAVHELGDAG
        GraphQLUtbetalingsdagType.UKJENTDAG -> SnapshotUtbetalingsdagType.UKJENTDAG
        GraphQLUtbetalingsdagType.__UNKNOWN_VALUE -> SnapshotUtbetalingsdagType.UNKNOWN_VALUE
    }

fun GraphQLUtbetalingstatus.tilSnapshotUtbetalingstatus() =
    when (this) {
        GraphQLUtbetalingstatus.ANNULLERT -> SnapshotUtbetalingstatus.ANNULLERT
        GraphQLUtbetalingstatus.FORKASTET -> SnapshotUtbetalingstatus.FORKASTET
        GraphQLUtbetalingstatus.GODKJENT -> SnapshotUtbetalingstatus.GODKJENT
        GraphQLUtbetalingstatus.GODKJENTUTENUTBETALING -> SnapshotUtbetalingstatus.GODKJENTUTENUTBETALING
        GraphQLUtbetalingstatus.IKKEGODKJENT -> SnapshotUtbetalingstatus.IKKEGODKJENT
        GraphQLUtbetalingstatus.OVERFORT -> SnapshotUtbetalingstatus.OVERFORT
        GraphQLUtbetalingstatus.SENDT -> SnapshotUtbetalingstatus.SENDT
        GraphQLUtbetalingstatus.UBETALT -> SnapshotUtbetalingstatus.UBETALT
        GraphQLUtbetalingstatus.UTBETALINGFEILET -> SnapshotUtbetalingstatus.UTBETALINGFEILET
        GraphQLUtbetalingstatus.UTBETALT -> SnapshotUtbetalingstatus.UTBETALT
        GraphQLUtbetalingstatus.__UNKNOWN_VALUE -> SnapshotUtbetalingstatus.UNKNOWN_VALUE
    }

fun Utbetalingtype.tilSnapshotUtbetalingtype() =
    when (this) {
        Utbetalingtype.ANNULLERING -> SnapshotUtbetalingtype.ANNULLERING
        Utbetalingtype.ETTERUTBETALING -> SnapshotUtbetalingtype.ETTERUTBETALING
        Utbetalingtype.FERIEPENGER -> SnapshotUtbetalingtype.FERIEPENGER
        Utbetalingtype.REVURDERING -> SnapshotUtbetalingtype.REVURDERING
        Utbetalingtype.UTBETALING -> SnapshotUtbetalingtype.UTBETALING
        Utbetalingtype.__UNKNOWN_VALUE -> SnapshotUtbetalingtype.UNKNOWN_VALUE
    }
