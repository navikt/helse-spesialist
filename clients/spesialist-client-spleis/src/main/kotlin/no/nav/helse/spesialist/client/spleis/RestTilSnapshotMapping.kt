package no.nav.helse.spesialist.client.spleis

import no.nav.helse.spesialist.application.snapshot.SnapshotAlder
import no.nav.helse.spesialist.application.snapshot.SnapshotAnnulleringskandidat
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiver
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverrefusjon
import no.nav.helse.spesialist.application.snapshot.SnapshotBegrunnelse
import no.nav.helse.spesialist.application.snapshot.SnapshotBehandling
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotDag
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
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetaling
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsdagType
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingsinfo
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingslinje
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingstatus
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingtype
import no.nav.helse.spesialist.application.snapshot.SnapshotVurdering
import no.nav.helse.spleis.rest.hentperson.Arbeidsgiver
import no.nav.helse.spleis.rest.hentperson.Arbeidsgiverinntekt
import no.nav.helse.spleis.rest.hentperson.Arbeidsgiverrefusjon
import no.nav.helse.spleis.rest.hentperson.Begrunnelse
import no.nav.helse.spleis.rest.hentperson.BeregnetPeriode
import no.nav.helse.spleis.rest.hentperson.Dag
import no.nav.helse.spleis.rest.hentperson.Generasjon
import no.nav.helse.spleis.rest.hentperson.GhostPeriode
import no.nav.helse.spleis.rest.hentperson.Hendelse
import no.nav.helse.spleis.rest.hentperson.Hendelsetype
import no.nav.helse.spleis.rest.hentperson.InfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.rest.hentperson.InntektFraAOrdningen
import no.nav.helse.spleis.rest.hentperson.Inntektskilde
import no.nav.helse.spleis.rest.hentperson.InntekterFraAOrdningen
import no.nav.helse.spleis.rest.hentperson.Inntektsmelding
import no.nav.helse.spleis.rest.hentperson.OmregnetArsinntekt
import no.nav.helse.spleis.rest.hentperson.Oppdrag
import no.nav.helse.spleis.rest.hentperson.PensjonsgivendeInntekt
import no.nav.helse.spleis.rest.hentperson.Periodetilstand
import no.nav.helse.spleis.rest.hentperson.Periodetype
import no.nav.helse.spleis.rest.hentperson.Periodevilkar
import no.nav.helse.spleis.rest.hentperson.Person
import no.nav.helse.spleis.rest.hentperson.Refusjonselement
import no.nav.helse.spleis.rest.hentperson.Simulering
import no.nav.helse.spleis.rest.hentperson.Simuleringsdetaljer
import no.nav.helse.spleis.rest.hentperson.Simuleringsperiode
import no.nav.helse.spleis.rest.hentperson.Simuleringsutbetaling
import no.nav.helse.spleis.rest.hentperson.SkjonnsmessigFastsatt
import no.nav.helse.spleis.rest.hentperson.SoknadArbeidsgiver
import no.nav.helse.spleis.rest.hentperson.SoknadArbeidsledig
import no.nav.helse.spleis.rest.hentperson.SoknadFrilans
import no.nav.helse.spleis.rest.hentperson.SoknadNav
import no.nav.helse.spleis.rest.hentperson.SoknadSelvstendig
import no.nav.helse.spleis.rest.hentperson.SpleisVilkarsgrunnlag
import no.nav.helse.spleis.rest.hentperson.Sykdomsdagkilde
import no.nav.helse.spleis.rest.hentperson.Sykdomsdagkildetype
import no.nav.helse.spleis.rest.hentperson.Sykdomsdagtype
import no.nav.helse.spleis.rest.hentperson.Sykepengegrunnlagsgrense
import no.nav.helse.spleis.rest.hentperson.Sykmelding
import no.nav.helse.spleis.rest.hentperson.Tidslinjeperiode
import no.nav.helse.spleis.rest.hentperson.UberegnetPeriode
import no.nav.helse.spleis.rest.hentperson.Utbetaling
import no.nav.helse.spleis.rest.hentperson.Utbetalingsinfo
import no.nav.helse.spleis.rest.hentperson.Utbetalingslinje
import no.nav.helse.spleis.rest.hentperson.Utbetalingstatus
import no.nav.helse.spleis.rest.hentperson.Utbetalingtype
import no.nav.helse.spleis.rest.hentperson.Vilkarsgrunnlag
import no.nav.helse.spleis.rest.hentperson.Vurdering
import java.math.BigDecimal

/**
 * Mapper REST-DTO-ene i `no.nav.helse.spleis.rest.hentperson` til [SnapshotPerson].
 */
internal fun Person.tilSnapshotPerson() =
    SnapshotPerson(
        aktorId = aktorId,
        arbeidsgivere = arbeidsgivere.map { it.tilSnapshotArbeidsgiver() },
        dodsdato = dodsdato,
        fodselsnummer = fodselsnummer,
        versjon = versjon,
        vilkarsgrunnlag = vilkarsgrunnlag.map { it.tilSnapshotVilkarsgrunnlag() },
    )

internal fun Arbeidsgiver.tilSnapshotArbeidsgiver() =
    SnapshotArbeidsgiver(
        organisasjonsnummer = organisasjonsnummer,
        ghostPerioder = ghostPerioder.map { it.tilSnapshotGhostPeriode() },
        behandlinger = generasjoner.map { it.tilSnapshotBehandling() },
    )

internal fun GhostPeriode.tilSnapshotGhostPeriode() =
    SnapshotGhostPeriode(
        fom = fom,
        tom = tom,
        skjaeringstidspunkt = skjaeringstidspunkt,
        vilkarsgrunnlagId = vilkarsgrunnlagId,
        deaktivert = deaktivert,
    )

internal fun Generasjon.tilSnapshotBehandling() =
    SnapshotBehandling(
        id = id,
        perioder = perioder.map { it.tilSnapshotTidslinjeperiode() },
    )

internal fun Tidslinjeperiode.tilSnapshotTidslinjeperiode() =
    when (this) {
        is UberegnetPeriode -> tilSnapshotUberegnetPeriode()
        is BeregnetPeriode -> tilSnapshotBeregnetPeriode()
    }

internal fun UberegnetPeriode.tilSnapshotUberegnetPeriode() =
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

internal fun BeregnetPeriode.tilSnapshotBeregnetPeriode() =
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
        forbrukteSykedager = forbrukteSykedager,
        gjenstaendeSykedager = gjenstaendeSykedager,
        maksdato = maksdato,
        periodevilkar = periodevilkar.tilSnapshotPeriodevilkar(),
        utbetaling = utbetaling.tilSnapshotUtbetaling(),
        vilkarsgrunnlagId = vilkarsgrunnlagId,
        pensjonsgivendeInntekter = pensjonsgivendeInntekter.map { it.tilSnapshotPensjonsgivendeInntekt() },
        annulleringskandidater = annulleringskandidater.map { it.tilSnapshotAnnulleringskandidat() },
    )

internal fun Dag.tilSnapshotDag() =
    SnapshotDag(
        begrunnelser = begrunnelser?.map { it.tilSnapshotBegrunnelse() },
        dato = dato,
        grad = grad,
        kilde = kilde.tilSnapshotSykdomsdagkilde(),
        sykdomsdagtype = sykdomsdagtype.tilSnapshotSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilSnapshotUtbetalingsdagType(),
        utbetalingsinfo = utbetalingsinfo?.tilSnapshotUtbetalingsinfo(),
    )

internal fun Sykdomsdagkilde.tilSnapshotSykdomsdagkilde() =
    SnapshotSykdomsdagkilde(
        id = id,
        type = type.tilSnapshotSykdomsdagkildetype(),
    )

internal fun Utbetalingsinfo.tilSnapshotUtbetalingsinfo() =
    SnapshotUtbetalingsinfo(
        arbeidsgiverbelop = arbeidsgiverbelop,
        inntekt = inntekt,
        personbelop = personbelop,
        refusjonsbelop = refusjonsbelop,
        totalGrad = totalGrad,
        utbetaling = utbetaling,
    )

internal fun Utbetalingslinje.tilSnapshotUtbetalingslinje() =
    SnapshotUtbetalingslinje(
        tom = tom,
        fom = fom,
        grad = grad,
        dagsats = dagsats,
    )

internal fun Oppdrag.tilSnapshotOppdrag() =
    SnapshotOppdrag(
        fagsystemId = fagsystemId,
        tidsstempel = tidsstempel,
        utbetalingslinjer = utbetalingslinjer.map { it.tilSnapshotUtbetalingslinje() },
        simulering = simulering?.tilSnapshotSimulering(),
    )

internal fun Utbetaling.tilSnapshotUtbetaling() =
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

internal fun Vurdering.tilSnapshotVurdering() =
    SnapshotVurdering(
        automatisk = automatisk,
        godkjent = godkjent,
        ident = ident,
        tidsstempel = tidsstempel,
    )

internal fun Periodevilkar.tilSnapshotPeriodevilkar() =
    SnapshotPeriodevilkar(
        alder = alder.tilSnapshotAlder(),
        sykepengedager = sykepengedager.tilSnapshotSykepengedager(),
    )

internal fun Periodevilkar.Alder.tilSnapshotAlder() =
    SnapshotAlder(
        alderSisteSykedag = alderSisteSykedag,
        oppfylt = oppfylt,
    )

internal fun Periodevilkar.Sykepengedager.tilSnapshotSykepengedager() =
    SnapshotSykepengedager(
        forbrukteSykedager = forbrukteSykedager,
        gjenstaendeSykedager = gjenstaendeSykedager,
        maksdato = maksdato,
        oppfylt = oppfylt,
        skjaeringstidspunkt = skjaeringstidspunkt,
    )

internal fun PensjonsgivendeInntekt.tilSnapshotPensjonsgivendeInntekt(): SnapshotPensjonsgivendeInntekt =
    SnapshotPensjonsgivendeInntekt(
        arligBelop = BigDecimal.valueOf(arligBelop),
        inntektsar = inntektsar,
    )

internal fun no.nav.helse.spleis.rest.hentperson.Annulleringskandidat.tilSnapshotAnnulleringskandidat(): SnapshotAnnulleringskandidat =
    SnapshotAnnulleringskandidat(
        fom = fom,
        organisasjonsnummer = organisasjonsnummer,
        tom = tom,
        vedtaksperiodeId = vedtaksperiodeId,
    )

internal fun Hendelse.tilSnapshotHendelse() =
    when (this) {
        is Inntektsmelding -> tilSnapshotInntektsmelding()
        is SoknadArbeidsgiver -> tilSnapshotSoknadArbeidsgiver()
        is SoknadNav -> tilSnapshotSoknadNav()
        is SoknadArbeidsledig -> tilSnapshotSoknadArbeidsledig()
        is SoknadFrilans -> tilSnapshotSoknadFrilans()
        is SoknadSelvstendig -> tilSnapshotSoknadSelvstendig()
        is Sykmelding -> tilSnapshotSykmelding()
        is InntektFraAOrdningen -> tilSnapshotInntektFraAOrdningen()
    }

internal fun Inntektsmelding.tilSnapshotInntektsmelding() =
    SnapshotInntektsmelding(
        beregnetInntekt = beregnetInntekt,
        id = id,
        mottattDato = mottattDato,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun SoknadArbeidsgiver.tilSnapshotSoknadArbeidsgiver() =
    SnapshotSoknadArbeidsgiver(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtArbeidsgiver = sendtArbeidsgiver,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun SoknadNav.tilSnapshotSoknadNav() =
    SnapshotSoknadNav(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun SoknadArbeidsledig.tilSnapshotSoknadArbeidsledig() =
    SnapshotSoknadArbeidsledig(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun SoknadFrilans.tilSnapshotSoknadFrilans() =
    SnapshotSoknadFrilans(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun SoknadSelvstendig.tilSnapshotSoknadSelvstendig() =
    SnapshotSoknadSelvstendig(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun Sykmelding.tilSnapshotSykmelding() =
    SnapshotSykmelding(
        fom = fom,
        id = id,
        rapportertDato = rapportertDato,
        tom = tom,
        type = type.tilSnapshotHendelsetype(),
    )

internal fun InntektFraAOrdningen.tilSnapshotInntektFraAOrdningen() =
    SnapshotInntektFraAOrdningen(
        id = id,
        mottattDato = mottattDato,
        type = type.tilSnapshotHendelsetype(),
        eksternDokumentId = eksternDokumentId,
    )

internal fun Vilkarsgrunnlag.tilSnapshotVilkarsgrunnlag() =
    when (this) {
        is InfotrygdVilkarsgrunnlag -> tilSnapshotInfotrygdVilkarsgrunnlag()
        is SpleisVilkarsgrunnlag -> tilSnapshotSpleisVilkarsgrunnlag()
    }

internal fun InfotrygdVilkarsgrunnlag.tilSnapshotInfotrygdVilkarsgrunnlag() =
    SnapshotInfotrygdVilkarsgrunnlag(
        id = id,
        inntekter = inntekter.map { it.tilSnapshotArbeidsgiverinntekt() },
        arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilSnapshotArbeidsgiverrefusjon() },
        omregnetArsinntekt = omregnetArsinntekt,
        skjaeringstidspunkt = skjaeringstidspunkt,
        sykepengegrunnlag = sykepengegrunnlag,
        opptjeningsvurderingId = opptjeningsvurderingId,
    )

internal fun SpleisVilkarsgrunnlag.tilSnapshotSpleisVilkarsgrunnlag() =
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
        opptjeningsvurderingId = opptjeningsvurderingId,
        beregingsgrunnlag = BigDecimal.valueOf(beregningsgrunnlag),
        forsikringsvurderingId = forsikringsvurderingId,
    )

internal fun Sykepengegrunnlagsgrense.tilSnapshotSykepengegrunnlagsgrense() =
    SnapshotSykepengegrunnlagsgrense(
        grunnbelop = grunnbelop,
        grense = grense,
        virkningstidspunkt = virkningstidspunkt,
    )

internal fun Arbeidsgiverinntekt.tilSnapshotArbeidsgiverinntekt() =
    SnapshotArbeidsgiverinntekt(
        arbeidsgiver = arbeidsgiver,
        omregnetArsinntekt = omregnetArsinntekt.tilSnapshotOmregnetArsinntekt(),
        skjonnsmessigFastsatt = skjonnsmessigFastsatt?.tilSnapshotSkjonnsmessigFastsatt(),
        deaktivert = deaktivert,
        fom = fom,
        tom = tom,
    )

internal fun Arbeidsgiverrefusjon.tilSnapshotArbeidsgiverrefusjon() =
    SnapshotArbeidsgiverrefusjon(
        arbeidsgiver = arbeidsgiver,
        refusjonsopplysninger = refusjonsopplysninger.map { it.tilSnapshotRefusjonselement() },
    )

internal fun Refusjonselement.tilSnapshotRefusjonselement() =
    SnapshotRefusjonselement(
        fom = fom,
        tom = tom,
        belop = belop,
        meldingsreferanseId = meldingsreferanseId,
    )

internal fun OmregnetArsinntekt.tilSnapshotOmregnetArsinntekt() =
    SnapshotOmregnetArsinntekt(
        belop = belop,
        inntekterFraAOrdningen = inntekterFraAOrdningen?.map { it.tilSnapshotInntekterFraAOrdningen() },
        kilde = kilde.tilSnapshotInntektskilde(),
        manedsbelop = manedsbelop,
    )

internal fun InntekterFraAOrdningen.tilSnapshotInntekterFraAOrdningen() =
    SnapshotInntekterFraAOrdningen(
        maned = maned,
        sum = sum,
    )

internal fun SkjonnsmessigFastsatt.tilSnapshotSkjonnsmessigFastsatt() =
    SnapshotSkjonnsmessigFastsatt(
        belop = belop,
        manedsbelop = manedsbelop,
    )

internal fun Simulering.tilSnapshotSimulering() =
    SnapshotSimulering(
        totalbelop = totalbelop,
        perioder = perioder.map { it.tilSnapshotSimuleringsperiode() },
    )

internal fun Simuleringsperiode.tilSnapshotSimuleringsperiode() =
    SnapshotSimuleringsperiode(
        fom = fom,
        tom = tom,
        utbetalinger = utbetalinger.map { it.tilSnapshotSimuleringsutbetaling() },
    )

internal fun Simuleringsutbetaling.tilSnapshotSimuleringsutbetaling() =
    SnapshotSimuleringsutbetaling(
        detaljer = detaljer.map { it.tilSnapshotSimuleringsdetaljer() },
        feilkonto = feilkonto,
        forfall = forfall,
        utbetalesTilId = utbetalesTilId,
        utbetalesTilNavn = utbetalesTilNavn,
    )

internal fun Simuleringsdetaljer.tilSnapshotSimuleringsdetaljer() =
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

internal fun Begrunnelse.tilSnapshotBegrunnelse() =
    when (this) {
        Begrunnelse.AndreYtelser -> SnapshotBegrunnelse.ANDREYTELSER
        Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> SnapshotBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE
        Begrunnelse.EtterDodsdato -> SnapshotBegrunnelse.ETTERDODSDATO
        Begrunnelse.ManglerMedlemskap -> SnapshotBegrunnelse.MANGLERMEDLEMSKAP
        Begrunnelse.ManglerOpptjening -> SnapshotBegrunnelse.MANGLEROPPTJENING
        Begrunnelse.MinimumInntekt -> SnapshotBegrunnelse.MINIMUMINNTEKT
        Begrunnelse.MinimumInntektOver67 -> SnapshotBegrunnelse.MINIMUMINNTEKTOVER67
        Begrunnelse.MinimumSykdomsgrad -> SnapshotBegrunnelse.MINIMUMSYKDOMSGRAD
        Begrunnelse.Over70 -> SnapshotBegrunnelse.OVER70
        Begrunnelse.SykepengedagerOppbrukt -> SnapshotBegrunnelse.SYKEPENGEDAGEROPPBRUKT
        Begrunnelse.SykepengedagerOppbruktOver67 -> SnapshotBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67
        Begrunnelse.AvslattMeldingTilNavDag -> SnapshotBegrunnelse.AVSLATTMELDINGTILNAVDAG
        Begrunnelse.MeldingTilNavDagUtenforVentetid -> SnapshotBegrunnelse.MELDINGTILNAVDAGUTENFORVENTETID
    }

internal fun Hendelsetype.tilSnapshotHendelsetype() =
    when (this) {
        Hendelsetype.InntektFraAOrdningen -> SnapshotHendelsetype.INNTEKTFRAAORDNINGEN
        Hendelsetype.Inntektsmelding -> SnapshotHendelsetype.INNTEKTSMELDING
        Hendelsetype.NySoknad -> SnapshotHendelsetype.NYSOKNAD
        Hendelsetype.SendtSoknadArbeidsgiver -> SnapshotHendelsetype.SENDTSOKNADARBEIDSGIVER
        Hendelsetype.SendtSoknadArbeidsledig -> SnapshotHendelsetype.SENDTSOKNADARBEIDSLEDIG
        Hendelsetype.SendtSoknadFrilans -> SnapshotHendelsetype.SENDTSOKNADFRILANS
        Hendelsetype.SendtSoknadNav -> SnapshotHendelsetype.SENDTSOKNADNAV
        Hendelsetype.SendtSoknadSelvstendig -> SnapshotHendelsetype.SENDTSOKNADSELVSTENDIG
        Hendelsetype.Ukjent -> SnapshotHendelsetype.UKJENT
    }

internal fun Inntektskilde.tilSnapshotInntektskilde() =
    when (this) {
        Inntektskilde.AOrdningen -> SnapshotInntektskilde.AORDNINGEN
        Inntektskilde.IkkeRapportert -> SnapshotInntektskilde.IKKERAPPORTERT
        Inntektskilde.Infotrygd -> SnapshotInntektskilde.INFOTRYGD
        Inntektskilde.Inntektsmelding -> SnapshotInntektskilde.INNTEKTSMELDING
        Inntektskilde.Saksbehandler -> SnapshotInntektskilde.SAKSBEHANDLER
    }

internal fun no.nav.helse.spleis.rest.hentperson.Inntektstype.tilSnapshotInntektstype() =
    when (this) {
        no.nav.helse.spleis.rest.hentperson.Inntektstype.EnArbeidsgiver -> SnapshotInntektstype.ENARBEIDSGIVER
    }

internal fun Periodetilstand.tilSnapshotPeriodetilstand() =
    when (this) {
        Periodetilstand.AnnulleringFeilet -> SnapshotPeriodetilstand.ANNULLERINGFEILET
        Periodetilstand.Annullert -> SnapshotPeriodetilstand.ANNULLERT
        Periodetilstand.AvventerAnnullering -> SnapshotPeriodetilstand.AVVENTERANNULLERING
        Periodetilstand.AvventerInntektsopplysninger -> SnapshotPeriodetilstand.AVVENTERINNTEKTSOPPLYSNINGER
        Periodetilstand.ForberederGodkjenning -> SnapshotPeriodetilstand.FORBEREDERGODKJENNING
        Periodetilstand.IngenUtbetaling -> SnapshotPeriodetilstand.INGENUTBETALING
        Periodetilstand.ManglerInformasjon -> SnapshotPeriodetilstand.MANGLERINFORMASJON
        Periodetilstand.RevurderingFeilet -> SnapshotPeriodetilstand.REVURDERINGFEILET
        Periodetilstand.TilAnnullering -> SnapshotPeriodetilstand.TILANNULLERING
        Periodetilstand.TilGodkjenning -> SnapshotPeriodetilstand.TILGODKJENNING
        Periodetilstand.TilInfotrygd -> SnapshotPeriodetilstand.TILINFOTRYGD
        Periodetilstand.TilUtbetaling -> SnapshotPeriodetilstand.TILUTBETALING
        Periodetilstand.Utbetalt -> SnapshotPeriodetilstand.UTBETALT
        Periodetilstand.UtbetaltVenterPaAnnenPeriode -> SnapshotPeriodetilstand.UTBETALTVENTERPAANNENPERIODE
        Periodetilstand.VenterPaAnnenPeriode -> SnapshotPeriodetilstand.VENTERPAANNENPERIODE
    }

internal fun Periodetype.tilSnapshotPeriodetype() =
    when (this) {
        Periodetype.Forlengelse -> SnapshotPeriodetype.FORLENGELSE
        Periodetype.Forstegangsbehandling -> SnapshotPeriodetype.FORSTEGANGSBEHANDLING
        Periodetype.Infotrygdforlengelse -> SnapshotPeriodetype.INFOTRYGDFORLENGELSE
        Periodetype.OvergangFraIt -> SnapshotPeriodetype.OVERGANGFRAIT
    }

internal fun Sykdomsdagkildetype.tilSnapshotSykdomsdagkildetype() =
    when (this) {
        Sykdomsdagkildetype.Inntektsmelding -> SnapshotSykdomsdagkildetype.INNTEKTSMELDING
        Sykdomsdagkildetype.Saksbehandler -> SnapshotSykdomsdagkildetype.SAKSBEHANDLER
        Sykdomsdagkildetype.Soknad -> SnapshotSykdomsdagkildetype.SOKNAD
        Sykdomsdagkildetype.Sykmelding -> SnapshotSykdomsdagkildetype.SYKMELDING
        Sykdomsdagkildetype.Ukjent -> SnapshotSykdomsdagkildetype.UKJENT
    }

internal fun Sykdomsdagtype.tilSnapshotSykdomsdagtype() =
    when (this) {
        Sykdomsdagtype.AndreYtelserAap -> SnapshotSykdomsdagtype.ANDREYTELSERAAP
        Sykdomsdagtype.AndreYtelserDagpenger -> SnapshotSykdomsdagtype.ANDREYTELSERDAGPENGER
        Sykdomsdagtype.AndreYtelserForeldrepenger -> SnapshotSykdomsdagtype.ANDREYTELSERFORELDREPENGER
        Sykdomsdagtype.AndreYtelserOmsorgspenger -> SnapshotSykdomsdagtype.ANDREYTELSEROMSORGSPENGER
        Sykdomsdagtype.AndreYtelserOpplaringspenger -> SnapshotSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER
        Sykdomsdagtype.AndreYtelserPleiepenger -> SnapshotSykdomsdagtype.ANDREYTELSERPLEIEPENGER
        Sykdomsdagtype.AndreYtelserSvangerskapspenger -> SnapshotSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER
        Sykdomsdagtype.ArbeidIkkeGjenopptattDag -> SnapshotSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG
        Sykdomsdagtype.Arbeidsdag -> SnapshotSykdomsdagtype.ARBEIDSDAG
        Sykdomsdagtype.Arbeidsgiverdag -> SnapshotSykdomsdagtype.ARBEIDSGIVERDAG
        Sykdomsdagtype.Feriedag -> SnapshotSykdomsdagtype.FERIEDAG
        Sykdomsdagtype.ForeldetSykedag -> SnapshotSykdomsdagtype.FORELDETSYKEDAG
        Sykdomsdagtype.FriskHelgedag -> SnapshotSykdomsdagtype.FRISKHELGEDAG
        Sykdomsdagtype.Permisjonsdag -> SnapshotSykdomsdagtype.PERMISJONSDAG
        Sykdomsdagtype.SykHelgedag -> SnapshotSykdomsdagtype.SYKHELGEDAG
        Sykdomsdagtype.Sykedag -> SnapshotSykdomsdagtype.SYKEDAG
        Sykdomsdagtype.SykedagNav -> SnapshotSykdomsdagtype.SYKEDAGNAV
        Sykdomsdagtype.Ubestemtdag -> SnapshotSykdomsdagtype.UBESTEMTDAG
        Sykdomsdagtype.MeldingTilNavDag -> SnapshotSykdomsdagtype.MELDINGTILNAVDAG
        Sykdomsdagtype.AvslattMeldingTilNavDag -> SnapshotSykdomsdagtype.AVSLATTMELDINGTILNAVDAG
    }

internal fun no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.tilSnapshotUtbetalingsdagType() =
    when (this) {
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.Arbeidsdag -> SnapshotUtbetalingsdagType.ARBEIDSDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.ArbeidsgiverperiodeDag -> SnapshotUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.AvvistDag -> SnapshotUtbetalingsdagType.AVVISTDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.Feriedag -> SnapshotUtbetalingsdagType.FERIEDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.ForeldetDag -> SnapshotUtbetalingsdagType.FORELDETDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.Helgedag -> SnapshotUtbetalingsdagType.HELGEDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.NavDag -> SnapshotUtbetalingsdagType.NAVDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.NavHelgDag -> SnapshotUtbetalingsdagType.NAVHELGDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.UkjentDag -> SnapshotUtbetalingsdagType.UKJENTDAG
        no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.Ventetidsdag -> SnapshotUtbetalingsdagType.VENTETIDSDAG
    }

internal fun Utbetalingstatus.tilSnapshotUtbetalingstatus() =
    when (this) {
        Utbetalingstatus.Annullert -> SnapshotUtbetalingstatus.ANNULLERT
        Utbetalingstatus.GodkjentUtenUtbetaling -> SnapshotUtbetalingstatus.GODKJENTUTENUTBETALING
        Utbetalingstatus.IkkeGodkjent -> SnapshotUtbetalingstatus.IKKEGODKJENT
        Utbetalingstatus.Overfort -> SnapshotUtbetalingstatus.OVERFORT
        Utbetalingstatus.Ubetalt -> SnapshotUtbetalingstatus.UBETALT
        Utbetalingstatus.Utbetalt -> SnapshotUtbetalingstatus.UTBETALT
    }

internal fun Utbetalingtype.tilSnapshotUtbetalingtype() =
    when (this) {
        Utbetalingtype.ANNULLERING -> SnapshotUtbetalingtype.ANNULLERING
        Utbetalingtype.ETTERUTBETALING -> SnapshotUtbetalingtype.ETTERUTBETALING
        Utbetalingtype.FERIEPENGER -> SnapshotUtbetalingtype.FERIEPENGER
        Utbetalingtype.REVURDERING -> SnapshotUtbetalingtype.REVURDERING
        Utbetalingtype.UTBETALING -> SnapshotUtbetalingtype.UTBETALING
    }
