package no.nav.helse.spesialist.api.graphql

import no.nav.helse.spesialist.application.snapshot.SnapshotAlder
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiver
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiverrefusjon
import no.nav.helse.spesialist.application.snapshot.SnapshotBehandling
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotHendelse
import no.nav.helse.spesialist.application.snapshot.SnapshotHendelsetype
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektskilde
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektstype
import no.nav.helse.spesialist.application.snapshot.SnapshotOmregnetArsinntekt
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetilstand
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetype
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodevilkar
import no.nav.helse.spesialist.application.snapshot.SnapshotRefusjonselement
import no.nav.helse.spesialist.application.snapshot.SnapshotSoknadArbeidsledig
import no.nav.helse.spesialist.application.snapshot.SnapshotSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.application.snapshot.SnapshotSykepengedager
import no.nav.helse.spesialist.application.snapshot.SnapshotSykepengegrunnlagsgrense
import no.nav.helse.spesialist.application.snapshot.SnapshotTidslinjeperiode
import no.nav.helse.spesialist.application.snapshot.SnapshotUberegnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetaling
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingstatus
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingtype
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.mai
import no.nav.helse.spesialist.domain.testfixtures.okt
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object SnapshotTestdata {
    fun opprettSnapshotArbeidsgiver(
        organisasjonsnummer: String,
        behandlinger: List<SnapshotBehandling>,
    ) = SnapshotArbeidsgiver(
        organisasjonsnummer = organisasjonsnummer,
        ghostPerioder = emptyList(),
        behandlinger = behandlinger,
    )

    fun opprettSnapshotHendelse(eksternDokumentId: UUID) =
        SnapshotSoknadArbeidsledig(
            id = UUID.randomUUID().toString(),
            eksternDokumentId = eksternDokumentId.toString(),
            fom = 11 mai 2022,
            tom = 30.mai(2022),
            rapportertDato = (10 okt 2023).atStartOfDay(),
            sendtNav = (10 okt 2023).atStartOfDay(),
            type = SnapshotHendelsetype.SENDTSOKNADARBEIDSLEDIG,
        )

    fun opprettSnapshotGenerasjon(
        perioder: List<SnapshotTidslinjeperiode>,
        id: UUID = UUID.randomUUID(),
    ) = SnapshotBehandling(id = id, perioder = perioder)

    fun opprettBeregnetPeriode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
        hendelser: List<SnapshotHendelse> = emptyList(),
    ) = SnapshotBeregnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = SnapshotInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = SnapshotPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = SnapshotPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId,
        forbrukteSykedager = null,
        gjenstaendeSykedager = null,
        hendelser = hendelser,
        maksdato = LocalDate.now(),
        vilkarsgrunnlagId = null,
        periodevilkar =
            SnapshotPeriodevilkar(
                alder = SnapshotAlder(55, true),
                sykepengedager =
                    SnapshotSykepengedager(
                        forbrukteSykedager = null,
                        gjenstaendeSykedager = null,
                        maksdato = LocalDate.now(),
                        oppfylt = true,
                        skjaeringstidspunkt = LocalDate.now(),
                    ),
            ),
        behandlingId = behandlingId,
        utbetaling =
            SnapshotUtbetaling(
                id = utbetalingId,
                arbeidsgiverFagsystemId = "EN_FAGSYSTEM_ID",
                arbeidsgiverNettoBelop = 1,
                personFagsystemId = "EN_FAGSYSTEM_ID",
                personNettoBelop = 0,
                statusEnum = SnapshotUtbetalingstatus.IKKEGODKJENT,
                typeEnum = SnapshotUtbetalingtype.UTBETALING,
                vurdering = null,
                personoppdrag = null,
                arbeidsgiveroppdrag = null,
            ),
        pensjonsgivendeInntekter = emptyList(),
        annulleringskandidater = emptyList(),
    )

    fun opprettUberegnetPeriode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
    ) = SnapshotUberegnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = SnapshotInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = SnapshotPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = SnapshotPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        hendelser = emptyList(),
    )

    fun graphQLSpleisVilkarsgrunnlag(
        organisasjonsnummer: String,
        id: UUID = UUID.randomUUID(),
    ) = SnapshotSpleisVilkarsgrunnlag(
        id = id,
        inntekter =
            listOf(
                SnapshotArbeidsgiverinntekt(
                    arbeidsgiver = organisasjonsnummer,
                    omregnetArsinntekt =
                        SnapshotOmregnetArsinntekt(
                            belop = 500_000.0,
                            inntekterFraAOrdningen = null,
                            manedsbelop = 55_000.0,
                            kilde = SnapshotInntektskilde.INNTEKTSMELDING,
                        ),
                    skjonnsmessigFastsatt = null,
                    deaktivert = null,
                    fom = 1 jan 2020,
                    tom = null,
                ),
                SnapshotArbeidsgiverinntekt(
                    arbeidsgiver = "987656789",
                    omregnetArsinntekt =
                        SnapshotOmregnetArsinntekt(
                            belop = 500_000.0,
                            inntekterFraAOrdningen = null,
                            manedsbelop = 55_000.0,
                            kilde = SnapshotInntektskilde.INNTEKTSMELDING,
                        ),
                    skjonnsmessigFastsatt = null,
                    deaktivert = null,
                    fom = 1 jan 2020,
                    tom = null,
                ),
            ),
        omregnetArsinntekt = 1_000_000.0,
        skjonnsmessigFastsattAarlig = 0.0,
        skjaeringstidspunkt = 1 jan 2020,
        sykepengegrunnlag = 1_000_000.0,
        antallOpptjeningsdagerErMinst = 123,
        grunnbelop = 100_000,
        sykepengegrunnlagsgrense =
            SnapshotSykepengegrunnlagsgrense(
                grunnbelop = 100_000,
                grense = 600_000,
                virkningstidspunkt = 1 jan 2020,
            ),
        oppfyllerKravOmMedlemskap = true,
        oppfyllerKravOmMinstelonn = true,
        oppfyllerKravOmOpptjening = true,
        opptjeningFra = 1 jan 2000,
        opptjeningsvurderingId = UUID.randomUUID(),
        arbeidsgiverrefusjoner =
            listOf(
                SnapshotArbeidsgiverrefusjon(
                    arbeidsgiver = organisasjonsnummer,
                    refusjonsopplysninger =
                        listOf(
                            SnapshotRefusjonselement(
                                fom = 1 jan 2020,
                                tom = null,
                                belop = 30000.0,
                                meldingsreferanseId = UUID.randomUUID(),
                            ),
                        ),
                ),
            ),
        beregingsgrunnlag = BigDecimal.valueOf(25.0),
        forsikringsvurderingId = null,
    )
}
