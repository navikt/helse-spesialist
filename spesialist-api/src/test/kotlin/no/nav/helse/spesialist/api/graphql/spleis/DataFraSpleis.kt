package no.nav.helse.spesialist.api.graphql.spleis

import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOppdrag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimulering
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsdetaljer
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsperiode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSimuleringsutbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVurdering
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

internal fun enSpleisPerson() = GraphQLPerson(
    aktorId = "jedi-master",
    arbeidsgivere = listOf(enArbeidsgiver()),
    dodsdato = null,
    fodselsnummer = "01017012345",
    versjon = 1,
    vilkarsgrunnlag = emptyList(),
)

private fun enArbeidsgiver(organisasjonsnummer: String = "987654321") = GraphQLArbeidsgiver(
    organisasjonsnummer = organisasjonsnummer,
    ghostPerioder = emptyList(),
    generasjoner = listOf(enGenerasjonFraSpleis()),
)

private fun enGenerasjonFraSpleis() = GraphQLGenerasjon(
    id = UUID.randomUUID(),
    perioder = listOf(enPeriodeFraSpleis()),
)

private fun enPeriodeFraSpleis() = GraphQLBeregnetPeriode(
    erForkastet = false,
    fom = LocalDate.parse("2020-01-01"),
    tom = LocalDate.parse("2020-01-31"),
    inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
    opprettet = LocalDateTime.now(),
    periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
    tidslinje = emptyList(),
    vedtaksperiodeId = UUID.randomUUID(),
    beregningId = UUID.randomUUID(),
    forbrukteSykedager = 10,
    gjenstaendeSykedager = 270,
    hendelser = emptyList(),
    maksdato = LocalDate.parse("2021-01-01"),
    periodevilkar = GraphQLPeriodevilkar(
        Alder(
            alderSisteSykedag = 40,
            oppfylt = true,
        ),
        sykepengedager = Sykepengedager(
            forbrukteSykedager = 10,
            gjenstaendeSykedager = 270,
            maksdato = LocalDate.parse("2021-01-01"),
            oppfylt = true,
            skjaeringstidspunkt = LocalDate.parse("2020-01-01"),
        ),
    ),
    skjaeringstidspunkt = LocalDate.parse("2020-01-01"),
    utbetaling = GraphQLUtbetaling(
        id = UUID.randomUUID(),
        arbeidsgiverFagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
        arbeidsgiverNettoBelop = 30000,
        personFagsystemId = "EN-PERSONFAGSYSTEMID",
        personNettoBelop = 0,
        statusEnum = GraphQLUtbetalingstatus.GODKJENT,
        typeEnum = Utbetalingtype.UTBETALING,
        vurdering = GraphQLVurdering(
            automatisk = false,
            godkjent = true,
            ident = "AB123456",
            tidsstempel = LocalDateTime.now(),
        ),
        personoppdrag = GraphQLOppdrag(
            fagsystemId = "EN-PERSONFAGSYSTEMID",
            tidsstempel = localDateTimeMedTilfeldigTidspunkt("2021-01-01"),
            utbetalingslinjer = emptyList(),
            simulering = null,
        ),
        arbeidsgiveroppdrag = GraphQLOppdrag(
            fagsystemId = "EN-ARBEIDSGIVERFAGSYSTEMID",
            tidsstempel = localDateTimeMedTilfeldigTidspunkt("2021-01-01"),
            utbetalingslinjer = emptyList(),
            simulering = GraphQLSimulering(
                totalbelop = 30000,
                perioder = listOf(
                    GraphQLSimuleringsperiode(
                        fom = LocalDate.parse("2020-01-01"),
                        tom = LocalDate.parse("2020-01-31"),
                        utbetalinger = listOf(
                            GraphQLSimuleringsutbetaling(
                                utbetalesTilNavn = "EN-PERSON",
                                utbetalesTilId = "EN-PERSONID",
                                feilkonto = false,
                                forfall = LocalDate.parse("2022-01-01"),
                                detaljer = listOf(
                                    GraphQLSimuleringsdetaljer(
                                        belop = 30000,
                                        antallSats = 1,
                                        faktiskFom = LocalDate.parse("2020-01-01"),
                                        faktiskTom = LocalDate.parse("2020-01-31"),
                                        klassekode = "EN-KLASSEKODE",
                                        klassekodeBeskrivelse = "EN-KLASSEKODEBESKRIVELSE",
                                        konto = "EN-KONTO",
                                        refunderesOrgNr = "ET-ORGNR",
                                        sats = 30000.0,
                                        tilbakeforing = false,
                                        typeSats = "EN-TYPESATS",
                                        uforegrad = 100,
                                        utbetalingstype = "EN-UTBETALINGSTYPE",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
    vilkarsgrunnlagId = null,
    periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
    behandlingId = UUID.randomUUID(),
)

fun localDateTimeMedTilfeldigTidspunkt(dato: String): LocalDateTime =
    LocalDate.parse(dato).atStartOfDay().plusNanos(Random.nextLong(until = 86_400_000_000_000))
        .truncatedTo(ChronoUnit.MILLIS)
