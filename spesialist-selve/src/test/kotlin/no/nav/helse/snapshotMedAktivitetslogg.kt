package no.nav.helse

import com.expediagroup.graphql.client.types.GraphQLClientResponse
import java.util.UUID
import no.nav.helse.mediator.graphql.HentSnapshot
import no.nav.helse.mediator.graphql.enums.GraphQLBehandlingstype
import no.nav.helse.mediator.graphql.enums.GraphQLInntektstype
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.mediator.graphql.enums.GraphQLPeriodetype
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.mediator.graphql.enums.Utbetalingtype
import no.nav.helse.mediator.graphql.hentsnapshot.Alder
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLAktivitet
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.mediator.graphql.hentsnapshot.Soknadsfrist
import no.nav.helse.mediator.graphql.hentsnapshot.Sykepengedager

fun snapshotMedWarnings(
    vedtaksperiodeId: UUID,
    orgnr: String,
    fnr: String,
    aktørId: String
): GraphQLClientResponse<HentSnapshot.Result> =
    snapshot(
        vedtaksperiodeId, orgnr, fnr, aktørId, listOf(
            GraphQLAktivitet(
                alvorlighetsgrad = "W",
                melding = "Brukeren har flere inntekter de siste tre måneder",
                tidsstempel = "2020-06-12 13:21:24.072",
                vedtaksperiodeId = vedtaksperiodeId.toString()
            )
        )
    )

fun snapshot(
    vedtaksperiodeId: UUID,
    orgnr: String,
    fnr: String,
    aktørId: String,
    aktivitetslogg: List<GraphQLAktivitet> = emptyList()
): GraphQLClientResponse<HentSnapshot.Result> =
    object : GraphQLClientResponse<HentSnapshot.Result> {
        override val data = HentSnapshot.Result(
            GraphQLPerson(
                aktorId = aktørId,
                fodselsnummer = fnr,
                versjon = 1,
                arbeidsgivere = listOf(
                    GraphQLArbeidsgiver(
                        organisasjonsnummer = orgnr,
                        ghostPerioder = emptyList(),
                        generasjoner = listOf(
                            GraphQLGenerasjon(
                                id = UUID.randomUUID().toString(),
                                perioder = listOf(
                                    GraphQLBeregnetPeriode(
                                        id = UUID.randomUUID().toString(),
                                        vedtaksperiodeId = vedtaksperiodeId.toString(),
                                        utbetaling = GraphQLUtbetaling(
                                            id = UUID.randomUUID().toString(),
                                            arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
                                            arbeidsgiverNettoBelop = 30000,
                                            personFagsystemId = "EN_FAGSYSTEMID",
                                            personNettoBelop = 0,
                                            statusEnum = GraphQLUtbetalingstatus.UBETALT,
                                            typeEnum = Utbetalingtype.UTBETALING,
                                            vurdering = null,
                                            personoppdrag = null,
                                            arbeidsgiveroppdrag = null
                                        ),
                                        behandlingstype = GraphQLBehandlingstype.BEHANDLET,
                                        erForkastet = false,
                                        fom = "2020-01-01",
                                        tom = "2020-01-31",
                                        inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
                                        opprettet = "2020-01-31",
                                        periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
                                        tidslinje = emptyList(),
                                        aktivitetslogg = aktivitetslogg,
                                        beregningId = UUID.randomUUID().toString(),
                                        forbrukteSykedager = null,
                                        gjenstaendeSykedager = null,
                                        hendelser = emptyList(),
                                        maksdato = "2021-01-01",
                                        periodevilkar = GraphQLPeriodevilkar(
                                            alder = Alder(
                                                alderSisteSykedag = 30,
                                                oppfylt = true,
                                            ),
                                            soknadsfrist = Soknadsfrist(
                                                sendtNav = "2020-01-31",
                                                soknadFom = "2020-01-01",
                                                soknadTom = "2020-01-31",
                                                oppfylt = true,
                                            ),
                                            sykepengedager = Sykepengedager(
                                                forbrukteSykedager = null,
                                                gjenstaendeSykedager = null,
                                                maksdato = "2021-01-01",
                                                skjaeringstidspunkt = "2020-01-01",
                                                oppfylt = true,
                                            )
                                        ),
                                        skjaeringstidspunkt = "2020-01-01",
                                        refusjon = null,
                                        vilkarsgrunnlaghistorikkId = UUID.randomUUID().toString(),
                                        tilstand = GraphQLPeriodetilstand.OPPGAVER,
                                    )
                                )
                            )
                        ),
                    )
                ),
                dodsdato = null,
                inntektsgrunnlag = emptyList(),
                vilkarsgrunnlaghistorikk = emptyList(),
            )
        )
    }
