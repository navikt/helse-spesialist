package no.nav.helse.spesialist.client.spleis

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
import no.nav.helse.spleis.graphql.hentsnapshot.Alder as GraphQLAlder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLAnnulleringskandidat
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverrefusjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLDag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
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
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingsinfo
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingslinje
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager as GraphQLSykepengedager
import no.nav.helse.spleis.rest.hentperson.Annulleringskandidat
import no.nav.helse.spleis.rest.hentperson.Arbeidsgiver
import no.nav.helse.spleis.rest.hentperson.Arbeidsgiverinntekt
import no.nav.helse.spleis.rest.hentperson.Arbeidsgiverrefusjon
import no.nav.helse.spleis.rest.hentperson.Begrunnelse
import no.nav.helse.spleis.rest.hentperson.BeregnetPeriode
import no.nav.helse.spleis.rest.hentperson.Dag
import no.nav.helse.spleis.rest.hentperson.Generasjon
import no.nav.helse.spleis.rest.hentperson.GhostPeriode
import no.nav.helse.spleis.rest.hentperson.Hendelsetype
import no.nav.helse.spleis.rest.hentperson.InfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.rest.hentperson.InntektFraAOrdningen
import no.nav.helse.spleis.rest.hentperson.Inntektskilde
import no.nav.helse.spleis.rest.hentperson.InntekterFraAOrdningen
import no.nav.helse.spleis.rest.hentperson.Inntektsmelding
import no.nav.helse.spleis.rest.hentperson.Inntektstype
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
import no.nav.helse.spleis.rest.hentperson.UberegnetPeriode
import no.nav.helse.spleis.rest.hentperson.Utbetaling
import no.nav.helse.spleis.rest.hentperson.Utbetalingsinfo
import no.nav.helse.spleis.rest.hentperson.Utbetalingslinje
import no.nav.helse.spleis.rest.hentperson.Utbetalingstatus
import no.nav.helse.spleis.rest.hentperson.Utbetalingtype as RestUtbetalingtype
import no.nav.helse.spleis.rest.hentperson.Vurdering
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Kontrakttest for [RestTilSnapshotMapping] og [SpleisTilSnapshotMapping]: bygger ett REST-DTO-tre
 * og ett GraphQL-DTO-tre med (så langt de to modellene overlapper) identiske underliggende verdier,
 * mapper begge til [no.nav.helse.spesialist.application.snapshot.SnapshotPerson], og forventer
 * strukturell likhet. Dette er sikkerhetsnettet mot at REST- og GraphQL-mappingen driver fra
 * hverandre over tid (se `SnapshotSammenligningHenter`), og dekker alle undertyper av
 * `Tidslinjeperiode`, `Vilkarsgrunnlag` og `Hendelse`.
 */
class MappingKontraktTest {
    private val behandlingId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val vedtaksperiodeId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val vilkarsgrunnlagId = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val infotrygdVilkarsgrunnlagId = UUID.fromString("00000000-0000-0000-0000-000000000004")
    private val opptjeningsvurderingId = UUID.fromString("00000000-0000-0000-0000-000000000005")
    private val forsikringsvurderingId = UUID.fromString("00000000-0000-0000-0000-000000000006")
    private val utbetalingId = UUID.fromString("00000000-0000-0000-0000-000000000007")
    private val meldingsreferanseId = UUID.fromString("00000000-0000-0000-0000-000000000008")
    private val sykdomsdagkildeId = UUID.fromString("00000000-0000-0000-0000-000000000009")
    private val generasjonId = UUID.fromString("00000000-0000-0000-0000-00000000000a")
    private val fom = LocalDate.of(2024, 1, 1)
    private val tom = LocalDate.of(2024, 1, 31)
    private val opprettet = LocalDateTime.of(2024, 1, 1, 8, 0)

    @Test
    fun `REST- og GraphQL-mapping gir identisk SnapshotPerson`() {
        val restPerson = restPerson()
        val graphQLPerson = graphQLPerson()

        assertEquals(graphQLPerson.tilSnapshotPerson(), restPerson.tilSnapshotPerson())
    }

    private fun restPerson() =
        Person(
            aktorId = "1234567890123",
            fodselsnummer = "11111111111",
            arbeidsgivere =
                listOf(
                    Arbeidsgiver(
                        organisasjonsnummer = "987654321",
                        generasjoner =
                            listOf(
                                Generasjon(
                                    id = generasjonId,
                                    kildeTilGenerasjon = generasjonId,
                                    perioder =
                                        listOf(
                                            restUberegnetPeriode(),
                                            restBeregnetPeriode(),
                                        ),
                                ),
                            ),
                        ghostPerioder =
                            listOf(
                                GhostPeriode(
                                    id = vilkarsgrunnlagId,
                                    fom = fom,
                                    tom = tom,
                                    skjaeringstidspunkt = fom,
                                    vilkarsgrunnlagId = vilkarsgrunnlagId,
                                    deaktivert = false,
                                ),
                            ),
                    ),
                ),
            dodsdato = null,
            versjon = 1,
            vilkarsgrunnlag =
                listOf(
                    restSpleisVilkarsgrunnlag(),
                    restInfotrygdVilkarsgrunnlag(),
                ),
        )

    private fun restUberegnetPeriode() =
        UberegnetPeriode(
            behandlingId = behandlingId,
            kilde = behandlingId,
            fom = fom,
            tom = tom,
            tidslinje = listOf(restDag()),
            periodetype = Periodetype.Forstegangsbehandling,
            erForkastet = false,
            opprettet = opprettet,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetilstand = Periodetilstand.TilGodkjenning,
            skjaeringstidspunkt = fom,
            hendelser = restHendelser(),
            pensjonsgivendeInntekter = emptyList(),
            inntektstype = Inntektstype.EnArbeidsgiver,
        )

    private fun restBeregnetPeriode() =
        BeregnetPeriode(
            behandlingId = behandlingId,
            kilde = behandlingId,
            fom = fom,
            tom = tom,
            tidslinje = listOf(restDag()),
            periodetype = Periodetype.Forlengelse,
            erForkastet = false,
            opprettet = opprettet,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetilstand = Periodetilstand.Utbetalt,
            skjaeringstidspunkt = fom,
            hendelser = restHendelser(),
            pensjonsgivendeInntekter = listOf(PensjonsgivendeInntekt(inntektsar = 2023, arligBelop = 500000.0)),
            beregningId = utbetalingId,
            gjenstaendeSykedager = 200,
            forbrukteSykedager = 48,
            maksdato = LocalDate.of(2025, 1, 1),
            utbetaling = restUtbetaling(),
            periodevilkar = restPeriodevilkar(),
            vilkarsgrunnlagId = vilkarsgrunnlagId,
            annulleringskandidater =
                listOf(
                    Annulleringskandidat(
                        vedtaksperiodeId = vedtaksperiodeId,
                        organisasjonsnummer = "987654321",
                        fom = fom,
                        tom = tom,
                    ),
                ),
            inntektstype = Inntektstype.EnArbeidsgiver,
        )

    private fun restDag() =
        Dag(
            dato = fom,
            sykdomsdagtype = Sykdomsdagtype.Sykedag,
            utbetalingsdagtype = no.nav.helse.spleis.rest.hentperson.UtbetalingsdagType.NavDag,
            kilde = Sykdomsdagkilde(id = sykdomsdagkildeId, type = Sykdomsdagkildetype.Sykmelding),
            grad = 100.0,
            utbetalingsinfo =
                Utbetalingsinfo(
                    inntekt = 500000,
                    utbetaling = 1000,
                    personbelop = 0,
                    arbeidsgiverbelop = 1000,
                    refusjonsbelop = 1000,
                    totalGrad = 100.0,
                ),
            begrunnelser = listOf(Begrunnelse.MinimumInntekt),
        )

    private fun restUtbetaling() =
        Utbetaling(
            id = utbetalingId,
            typeEnum = RestUtbetalingtype.UTBETALING,
            statusEnum = Utbetalingstatus.Utbetalt,
            arbeidsgiverNettoBelop = 10000,
            personNettoBelop = 0,
            arbeidsgiverFagsystemId = "AG123",
            personFagsystemId = "P123",
            arbeidsgiveroppdrag = restOppdrag(),
            personoppdrag = null,
            vurdering =
                Vurdering(
                    godkjent = true,
                    tidsstempel = opprettet,
                    automatisk = false,
                    ident = "Z999999",
                ),
        )

    private fun restOppdrag() =
        Oppdrag(
            fagsystemId = "AG123",
            tidsstempel = opprettet,
            simulering = restSimulering(),
            utbetalingslinjer = listOf(Utbetalingslinje(fom = fom, tom = tom, dagsats = 1000, grad = 100)),
        )

    private fun restSimulering() =
        Simulering(
            totalbelop = 10000,
            perioder =
                listOf(
                    Simuleringsperiode(
                        fom = fom,
                        tom = tom,
                        utbetalinger =
                            listOf(
                                Simuleringsutbetaling(
                                    detaljer =
                                        listOf(
                                            Simuleringsdetaljer(
                                                belop = 10000,
                                                antallSats = 31,
                                                faktiskFom = fom,
                                                faktiskTom = tom,
                                                klassekode = "SPREFAG-IOP",
                                                klassekodeBeskrivelse = "Sykepenger",
                                                konto = "0",
                                                refunderesOrgNr = "987654321",
                                                sats = 1000.0,
                                                tilbakeforing = false,
                                                typeSats = "DAG",
                                                uforegrad = 100,
                                                utbetalingstype = "YTEL",
                                            ),
                                        ),
                                    feilkonto = false,
                                    forfall = tom,
                                    utbetalesTilId = "987654321",
                                    utbetalesTilNavn = "Arbeidsgiver AS",
                                ),
                            ),
                    ),
                ),
        )

    private fun restPeriodevilkar() =
        Periodevilkar(
            alder = Periodevilkar.Alder(alderSisteSykedag = 40, oppfylt = true),
            sykepengedager =
                Periodevilkar.Sykepengedager(
                    skjaeringstidspunkt = fom,
                    maksdato = LocalDate.of(2025, 1, 1),
                    forbrukteSykedager = 48,
                    gjenstaendeSykedager = 200,
                    oppfylt = true,
                ),
        )

    private fun restHendelser() =
        listOf(
            Inntektsmelding(
                beregnetInntekt = 50000.0,
                id = "im-1",
                mottattDato = opprettet,
                type = Hendelsetype.Inntektsmelding,
                eksternDokumentId = "ekstern-1",
            ),
            SoknadArbeidsgiver(
                fom = fom,
                id = "sa-1",
                rapportertDato = opprettet,
                sendtArbeidsgiver = opprettet,
                tom = tom,
                type = Hendelsetype.SendtSoknadArbeidsgiver,
                eksternDokumentId = "ekstern-2",
            ),
            SoknadNav(
                fom = fom,
                id = "sn-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = Hendelsetype.SendtSoknadNav,
                eksternDokumentId = "ekstern-3",
            ),
            SoknadArbeidsledig(
                fom = fom,
                id = "sal-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = Hendelsetype.SendtSoknadArbeidsledig,
                eksternDokumentId = "ekstern-4",
            ),
            SoknadFrilans(
                fom = fom,
                id = "sf-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = Hendelsetype.SendtSoknadFrilans,
                eksternDokumentId = "ekstern-5",
            ),
            SoknadSelvstendig(
                fom = fom,
                id = "ss-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = Hendelsetype.SendtSoknadSelvstendig,
                eksternDokumentId = "ekstern-6",
            ),
            Sykmelding(
                fom = fom,
                id = "sm-1",
                eksternDokumentId = "ekstern-8",
                rapportertDato = opprettet,
                tom = tom,
                type = Hendelsetype.Ukjent,
            ),
            InntektFraAOrdningen(
                id = "ifa-1",
                mottattDato = opprettet,
                type = Hendelsetype.InntektFraAOrdningen,
                eksternDokumentId = "ekstern-7",
            ),
        )

    private fun restSpleisVilkarsgrunnlag() =
        SpleisVilkarsgrunnlag(
            id = vilkarsgrunnlagId,
            skjaeringstidspunkt = fom,
            omregnetArsinntekt = 500000.0,
            sykepengegrunnlag = 500000.0,
            inntekter = listOf(restArbeidsgiverinntekt()),
            arbeidsgiverrefusjoner = listOf(restArbeidsgiverrefusjon()),
            beregningsgrunnlag = 500000.0,
            grunnbelop = 118620,
            sykepengegrunnlagsgrense =
                Sykepengegrunnlagsgrense(grunnbelop = 118620, grense = 711720, virkningstidspunkt = fom),
            antallOpptjeningsdagerErMinst = 28,
            opptjeningFra = fom.minusMonths(1),
            oppfyllerKravOmMinstelonn = true,
            oppfyllerKravOmOpptjening = true,
            oppfyllerKravOmMedlemskap = true,
            forsikringsvurderingId = forsikringsvurderingId,
            opptjeningsvurderingId = opptjeningsvurderingId,
            skjonnsmessigFastsattAarlig = null,
        )

    private fun restInfotrygdVilkarsgrunnlag() =
        InfotrygdVilkarsgrunnlag(
            id = infotrygdVilkarsgrunnlagId,
            skjaeringstidspunkt = fom,
            omregnetArsinntekt = 400000.0,
            sykepengegrunnlag = 400000.0,
            arbeidsgiverrefusjoner = listOf(restArbeidsgiverrefusjon()),
            inntekter = listOf(restArbeidsgiverinntekt()),
            opptjeningsvurderingId = opptjeningsvurderingId,
        )

    private fun restArbeidsgiverinntekt() =
        Arbeidsgiverinntekt(
            arbeidsgiver = "987654321",
            omregnetArsinntekt =
                OmregnetArsinntekt(
                    kilde = Inntektskilde.Inntektsmelding,
                    belop = 500000.0,
                    manedsbelop = 41666.67,
                    inntekterFraAOrdningen = listOf(InntekterFraAOrdningen(maned = YearMonth.of(2023, 12), sum = 41666.67)),
                ),
            skjonnsmessigFastsatt = SkjonnsmessigFastsatt(belop = 510000.0, manedsbelop = 42500.0),
            fom = fom,
            tom = tom,
            deaktivert = false,
        )

    private fun restArbeidsgiverrefusjon() =
        Arbeidsgiverrefusjon(
            arbeidsgiver = "987654321",
            refusjonsopplysninger =
                listOf(
                    Refusjonselement(fom = fom, tom = tom, belop = 500000.0, meldingsreferanseId = meldingsreferanseId),
                ),
        )

    private fun graphQLPerson() =
        GraphQLPerson(
            aktorId = "1234567890123",
            fodselsnummer = "11111111111",
            arbeidsgivere =
                listOf(
                    GraphQLArbeidsgiver(
                        organisasjonsnummer = "987654321",
                        generasjoner =
                            listOf(
                                GraphQLGenerasjon(
                                    id = generasjonId,
                                    perioder =
                                        listOf(
                                            graphQLUberegnetPeriode(),
                                            graphQLBeregnetPeriode(),
                                        ),
                                ),
                            ),
                        ghostPerioder =
                            listOf(
                                GraphQLGhostPeriode(
                                    fom = fom,
                                    tom = tom,
                                    skjaeringstidspunkt = fom,
                                    vilkarsgrunnlagId = vilkarsgrunnlagId,
                                    deaktivert = false,
                                ),
                            ),
                    ),
                ),
            dodsdato = null,
            versjon = 1,
            vilkarsgrunnlag =
                listOf(
                    graphQLSpleisVilkarsgrunnlag(),
                    graphQLInfotrygdVilkarsgrunnlag(),
                ),
        )

    private fun graphQLUberegnetPeriode() =
        no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUberegnetPeriode(
            behandlingId = behandlingId,
            fom = fom,
            tom = tom,
            tidslinje = listOf(graphQLDag()),
            periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
            erForkastet = false,
            opprettet = opprettet,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
            skjaeringstidspunkt = fom,
            hendelser = graphQLHendelser(),
            inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
        )

    private fun graphQLBeregnetPeriode() =
        GraphQLBeregnetPeriode(
            behandlingId = behandlingId,
            fom = fom,
            tom = tom,
            tidslinje = listOf(graphQLDag()),
            periodetype = GraphQLPeriodetype.FORLENGELSE,
            erForkastet = false,
            opprettet = opprettet,
            vedtaksperiodeId = vedtaksperiodeId,
            periodetilstand = GraphQLPeriodetilstand.UTBETALT,
            skjaeringstidspunkt = fom,
            hendelser = graphQLHendelser(),
            inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
            pensjonsgivendeInntekter = listOf(GraphQLPensjonsgivendeInntekt(inntektsar = 2023, arligBelop = 500000.0)),
            gjenstaendeSykedager = 200,
            forbrukteSykedager = 48,
            maksdato = LocalDate.of(2025, 1, 1),
            utbetaling = graphQLUtbetaling(),
            periodevilkar = graphQLPeriodevilkar(),
            vilkarsgrunnlagId = vilkarsgrunnlagId,
            annulleringskandidater =
                listOf(
                    GraphQLAnnulleringskandidat(
                        vedtaksperiodeId = vedtaksperiodeId,
                        organisasjonsnummer = "987654321",
                        fom = fom,
                        tom = tom,
                    ),
                ),
        )

    private fun graphQLDag() =
        GraphQLDag(
            dato = fom,
            sykdomsdagtype = GraphQLSykdomsdagtype.SYKEDAG,
            utbetalingsdagtype = GraphQLUtbetalingsdagType.NAVDAG,
            kilde = GraphQLSykdomsdagkilde(id = sykdomsdagkildeId, type = GraphQLSykdomsdagkildetype.SYKMELDING),
            grad = 100.0,
            utbetalingsinfo =
                GraphQLUtbetalingsinfo(
                    inntekt = 500000,
                    utbetaling = 1000,
                    personbelop = 0,
                    arbeidsgiverbelop = 1000,
                    refusjonsbelop = 1000,
                    totalGrad = 100.0,
                ),
            begrunnelser = listOf(GraphQLBegrunnelse.MINIMUMINNTEKT),
        )

    private fun graphQLUtbetaling() =
        GraphQLUtbetaling(
            id = utbetalingId,
            typeEnum = Utbetalingtype.UTBETALING,
            statusEnum = GraphQLUtbetalingstatus.UTBETALT,
            arbeidsgiverNettoBelop = 10000,
            personNettoBelop = 0,
            arbeidsgiverFagsystemId = "AG123",
            personFagsystemId = "P123",
            arbeidsgiveroppdrag = graphQLOppdrag(),
            personoppdrag = null,
            vurdering =
                GraphQLVurdering(
                    godkjent = true,
                    tidsstempel = opprettet,
                    automatisk = false,
                    ident = "Z999999",
                ),
        )

    private fun graphQLOppdrag() =
        GraphQLOppdrag(
            fagsystemId = "AG123",
            tidsstempel = opprettet,
            simulering = graphQLSimulering(),
            utbetalingslinjer = listOf(GraphQLUtbetalingslinje(fom = fom, tom = tom, dagsats = 1000, grad = 100)),
        )

    private fun graphQLSimulering() =
        GraphQLSimulering(
            totalbelop = 10000,
            perioder =
                listOf(
                    GraphQLSimuleringsperiode(
                        fom = fom,
                        tom = tom,
                        utbetalinger =
                            listOf(
                                GraphQLSimuleringsutbetaling(
                                    detaljer =
                                        listOf(
                                            GraphQLSimuleringsdetaljer(
                                                belop = 10000,
                                                antallSats = 31,
                                                faktiskFom = fom,
                                                faktiskTom = tom,
                                                klassekode = "SPREFAG-IOP",
                                                klassekodeBeskrivelse = "Sykepenger",
                                                konto = "0",
                                                refunderesOrgNr = "987654321",
                                                sats = 1000.0,
                                                tilbakeforing = false,
                                                typeSats = "DAG",
                                                uforegrad = 100,
                                                utbetalingstype = "YTEL",
                                            ),
                                        ),
                                    feilkonto = false,
                                    forfall = tom,
                                    utbetalesTilId = "987654321",
                                    utbetalesTilNavn = "Arbeidsgiver AS",
                                ),
                            ),
                    ),
                ),
        )

    private fun graphQLPeriodevilkar() =
        GraphQLPeriodevilkar(
            alder = GraphQLAlder(alderSisteSykedag = 40, oppfylt = true),
            sykepengedager =
                GraphQLSykepengedager(
                    skjaeringstidspunkt = fom,
                    maksdato = LocalDate.of(2025, 1, 1),
                    forbrukteSykedager = 48,
                    gjenstaendeSykedager = 200,
                    oppfylt = true,
                ),
        )

    private fun graphQLHendelser(): List<GraphQLHendelse> =
        listOf(
            GraphQLInntektsmelding(
                beregnetInntekt = 50000.0,
                id = "im-1",
                mottattDato = opprettet,
                type = GraphQLHendelsetype.INNTEKTSMELDING,
                eksternDokumentId = "ekstern-1",
            ),
            GraphQLSoknadArbeidsgiver(
                fom = fom,
                id = "sa-1",
                rapportertDato = opprettet,
                sendtArbeidsgiver = opprettet,
                tom = tom,
                type = GraphQLHendelsetype.SENDTSOKNADARBEIDSGIVER,
                eksternDokumentId = "ekstern-2",
            ),
            GraphQLSoknadNav(
                fom = fom,
                id = "sn-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = GraphQLHendelsetype.SENDTSOKNADNAV,
                eksternDokumentId = "ekstern-3",
            ),
            GraphQLSoknadArbeidsledig(
                fom = fom,
                id = "sal-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = GraphQLHendelsetype.SENDTSOKNADARBEIDSLEDIG,
                eksternDokumentId = "ekstern-4",
            ),
            GraphQLSoknadFrilans(
                fom = fom,
                id = "sf-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = GraphQLHendelsetype.SENDTSOKNADFRILANS,
                eksternDokumentId = "ekstern-5",
            ),
            GraphQLSoknadSelvstendig(
                fom = fom,
                id = "ss-1",
                rapportertDato = opprettet,
                sendtNav = opprettet,
                tom = tom,
                type = GraphQLHendelsetype.SENDTSOKNADSELVSTENDIG,
                eksternDokumentId = "ekstern-6",
            ),
            GraphQLSykmelding(
                fom = fom,
                id = "sm-1",
                rapportertDato = opprettet,
                tom = tom,
                type = GraphQLHendelsetype.UKJENT,
            ),
            GraphQLInntektFraAOrdningen(
                id = "ifa-1",
                mottattDato = opprettet,
                type = GraphQLHendelsetype.INNTEKTFRAAORDNINGEN,
                eksternDokumentId = "ekstern-7",
            ),
        )

    private fun graphQLSpleisVilkarsgrunnlag() =
        GraphQLSpleisVilkarsgrunnlag(
            id = vilkarsgrunnlagId,
            skjaeringstidspunkt = fom,
            omregnetArsinntekt = 500000.0,
            sykepengegrunnlag = 500000.0,
            inntekter = listOf(graphQLArbeidsgiverinntekt()),
            arbeidsgiverrefusjoner = listOf(graphQLArbeidsgiverrefusjon()),
            beregningsgrunnlag = 500000.0,
            grunnbelop = 118620,
            sykepengegrunnlagsgrense =
                GraphQLSykepengegrunnlagsgrense(grunnbelop = 118620, grense = 711720, virkningstidspunkt = fom),
            antallOpptjeningsdagerErMinst = 28,
            opptjeningFra = fom.minusMonths(1),
            oppfyllerKravOmMinstelonn = true,
            oppfyllerKravOmOpptjening = true,
            oppfyllerKravOmMedlemskap = true,
            forsikringsvurderingId = forsikringsvurderingId,
            opptjeningsvurderingId = opptjeningsvurderingId,
            skjonnsmessigFastsattAarlig = null,
        )

    private fun graphQLInfotrygdVilkarsgrunnlag() =
        no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag(
            id = infotrygdVilkarsgrunnlagId,
            skjaeringstidspunkt = fom,
            omregnetArsinntekt = 400000.0,
            sykepengegrunnlag = 400000.0,
            arbeidsgiverrefusjoner = listOf(graphQLArbeidsgiverrefusjon()),
            inntekter = listOf(graphQLArbeidsgiverinntekt()),
            opptjeningsvurderingId = opptjeningsvurderingId,
        )

    private fun graphQLArbeidsgiverinntekt() =
        GraphQLArbeidsgiverinntekt(
            arbeidsgiver = "987654321",
            omregnetArsinntekt =
                GraphQLOmregnetArsinntekt(
                    kilde = GraphQLInntektskilde.INNTEKTSMELDING,
                    belop = 500000.0,
                    manedsbelop = 41666.67,
                    inntekterFraAOrdningen =
                        listOf(GraphQLInntekterFraAOrdningen(maned = YearMonth.of(2023, 12), sum = 41666.67)),
                ),
            skjonnsmessigFastsatt = GraphQLSkjonnsmessigFastsatt(belop = 510000.0, manedsbelop = 42500.0),
            fom = fom,
            tom = tom,
            deaktivert = false,
        )

    private fun graphQLArbeidsgiverrefusjon() =
        GraphQLArbeidsgiverrefusjon(
            arbeidsgiver = "987654321",
            refusjonsopplysninger =
                listOf(
                    GraphQLRefusjonselement(
                        fom = fom,
                        tom = tom,
                        belop = 500000.0,
                        meldingsreferanseId = meldingsreferanseId,
                    ),
                ),
        )
}
