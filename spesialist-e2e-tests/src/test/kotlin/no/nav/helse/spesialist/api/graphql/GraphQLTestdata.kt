package no.nav.helse.spesialist.api.graphql

import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.mai
import no.nav.helse.spesialist.domain.testfixtures.okt
import no.nav.helse.spleis.graphql.enums.GraphQLHendelsetype
import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLRefusjonselement
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsledig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLTidslinjeperiode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object GraphQLTestdata {
    fun opprettSnapshotArbeidsgiver(
        organisasjonsnummer: String,
        generasjoner: List<GraphQLGenerasjon>,
    ) = GraphQLArbeidsgiver(
        organisasjonsnummer = organisasjonsnummer,
        ghostPerioder = emptyList(),
        generasjoner = generasjoner,
    )

    fun opprettSnapshotHendelse(eksternDokumentId: UUID) =
        GraphQLSoknadArbeidsledig(
            id = UUID.randomUUID().toString(),
            eksternDokumentId = eksternDokumentId.toString(),
            fom = 11 mai 2022,
            tom = 30.mai(2022),
            rapportertDato = (10 okt 2023).atStartOfDay(),
            sendtNav = (10 okt 2023).atStartOfDay(),
            type = GraphQLHendelsetype.SENDTSOKNADARBEIDSLEDIG,
        )

    fun opprettSnapshotGenerasjon(
        perioder: List<GraphQLTidslinjeperiode>,
        id: UUID = UUID.randomUUID(),
    ) = GraphQLGenerasjon(id = id, perioder = perioder)

    fun opprettBeregnetPeriode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
        hendelser: List<GraphQLHendelse> = emptyList(),
    ) = GraphQLBeregnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId,
        forbrukteSykedager = null,
        gjenstaendeSykedager = null,
        hendelser = hendelser,
        maksdato = LocalDate.now(),
        vilkarsgrunnlagId = null,
        periodevilkar =
            GraphQLPeriodevilkar(
                alder = Alder(55, true),
                sykepengedager =
                    Sykepengedager(
                        maksdato = LocalDate.now(),
                        oppfylt = true,
                        skjaeringstidspunkt = LocalDate.now(),
                    ),
            ),
        behandlingId = behandlingId,
        utbetaling =
            GraphQLUtbetaling(
                id = utbetalingId,
                arbeidsgiverFagsystemId = "EN_FAGSYSTEM_ID",
                arbeidsgiverNettoBelop = 1,
                personFagsystemId = "EN_FAGSYSTEM_ID",
                personNettoBelop = 0,
                statusEnum = GraphQLUtbetalingstatus.IKKEGODKJENT,
                typeEnum = Utbetalingtype.UTBETALING,
            ),
        pensjonsgivendeInntekter = emptyList(),
        annulleringskandidater = emptyList(),
    )

    fun opprettUberegnetPeriode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        behandlingId: UUID = UUID.randomUUID(),
    ) = GraphQLUberegnetPeriode(
        erForkastet = false,
        fom = fom,
        tom = tom,
        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
        periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
        skjaeringstidspunkt = LocalDate.now(),
        tidslinje = emptyList(),
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        hendelser = emptyList(),
    )

    fun graphQLSpleisVilkarsgrunnlag(
        organisasjonsnummer: String,
        id: UUID = UUID.randomUUID(),
    ) = GraphQLSpleisVilkarsgrunnlag(
        id = id,
        inntekter =
            listOf(
                GraphQLArbeidsgiverinntekt(
                    arbeidsgiver = organisasjonsnummer,
                    omregnetArsinntekt =
                        GraphQLOmregnetArsinntekt(
                            belop = 500_000.0,
                            manedsbelop = 55_000.0,
                            kilde = GraphQLInntektskilde.INNTEKTSMELDING,
                        ),
                    fom = 1 jan 2020,
                    tom = null,
                ),
                GraphQLArbeidsgiverinntekt(
                    arbeidsgiver = "987656789",
                    omregnetArsinntekt =
                        GraphQLOmregnetArsinntekt(
                            belop = 500_000.0,
                            manedsbelop = 55_000.0,
                            kilde = GraphQLInntektskilde.INNTEKTSMELDING,
                        ),
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
            GraphQLSykepengegrunnlagsgrense(
                grunnbelop = 100_000,
                grense = 600_000,
                virkningstidspunkt = 1 jan 2020,
            ),
        oppfyllerKravOmMedlemskap = true,
        oppfyllerKravOmMinstelonn = true,
        oppfyllerKravOmOpptjening = true,
        opptjeningFra = 1 jan 2000,
        arbeidsgiverrefusjoner =
            listOf(
                GraphQLArbeidsgiverrefusjon(
                    arbeidsgiver = organisasjonsnummer,
                    refusjonsopplysninger =
                        listOf(
                            GraphQLRefusjonselement(
                                fom = 1 jan 2020,
                                tom = null,
                                belop = 30000.0,
                                meldingsreferanseId = UUID.randomUUID(),
                            ),
                        ),
                ),
            ),
        beregningsgrunnlag = 25.0,
    )
}
