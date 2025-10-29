package no.nav.helse

import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.ApiLovhjemmel
import no.nav.helse.spesialist.api.graphql.schema.ApiSkjonnsfastsettelse
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.hentsnapshot.Alder
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.hentsnapshot.Sykepengedager
import no.nav.helse.util.desember
import no.nav.helse.util.januar
import java.time.LocalDate
import java.util.UUID

object Testdata {
    private val testperson = TestPerson()

    fun snapshot(
        versjon: Int = 1,
        aktørId: String = testperson.aktørId,
        organisasjonsnummer: String = testperson.orgnummer,
        fødselsnummer: String,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        utbetalingId: UUID = testperson.utbetalingId1,
        arbeidsgiverbeløp: Int = 30000,
        personbeløp: Int = 0,
        utbetaling: GraphQLUtbetaling =
            GraphQLUtbetaling(
                id = utbetalingId,
                arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
                arbeidsgiverNettoBelop = arbeidsgiverbeløp,
                personFagsystemId = "EN_FAGSYSTEMID",
                personNettoBelop = personbeløp,
                statusEnum = GraphQLUtbetalingstatus.UBETALT,
                typeEnum = Utbetalingtype.UTBETALING,
                vurdering = null,
                personoppdrag = null,
                arbeidsgiveroppdrag = null,
            ),
    ): GraphQLPerson =
        GraphQLPerson(
            aktorId = aktørId,
            fodselsnummer = fødselsnummer,
            versjon = versjon,
            arbeidsgivere =
                listOf(
                    GraphQLArbeidsgiver(
                        organisasjonsnummer = organisasjonsnummer,
                        ghostPerioder = emptyList(),
                        generasjoner =
                            listOf(
                                GraphQLGenerasjon(
                                    id = UUID.randomUUID(),
                                    perioder =
                                        listOf(
                                            GraphQLBeregnetPeriode(
                                                behandlingId = UUID.randomUUID(),
                                                vedtaksperiodeId = vedtaksperiodeId,
                                                utbetaling = utbetaling,
                                                erForkastet = false,
                                                fom = 1.januar(2020),
                                                tom = 31.januar(2020),
                                                inntektstype = GraphQLInntektstype.ENARBEIDSGIVER,
                                                opprettet = 31.januar(2020).atStartOfDay(),
                                                periodetype = GraphQLPeriodetype.FORSTEGANGSBEHANDLING,
                                                tidslinje = emptyList(),
                                                beregningId = UUID.randomUUID(),
                                                forbrukteSykedager = null,
                                                gjenstaendeSykedager = null,
                                                hendelser = emptyList(),
                                                maksdato = 1.januar(2021),
                                                periodevilkar =
                                                    GraphQLPeriodevilkar(
                                                        alder =
                                                            Alder(
                                                                alderSisteSykedag = 30,
                                                                oppfylt = true,
                                                            ),
                                                        sykepengedager =
                                                            Sykepengedager(
                                                                forbrukteSykedager = null,
                                                                gjenstaendeSykedager = null,
                                                                maksdato = 1.januar(2021),
                                                                skjaeringstidspunkt = 1.januar(2020),
                                                                oppfylt = true,
                                                            ),
                                                    ),
                                                skjaeringstidspunkt = 1.januar(2020),
                                                vilkarsgrunnlagId = null,
                                                periodetilstand = GraphQLPeriodetilstand.TILGODKJENNING,
                                                pensjonsgivendeInntekter = emptyList(),
                                                annulleringskandidater = emptyList(),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
            dodsdato = null,
            vilkarsgrunnlag = emptyList(),
        )

    fun skjønnsvurdering() = ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver(
        organisasjonsnummer = testperson.orgnummer,
        arlig = 1.0,
        fraArlig = 1.0,
        arsak = "årsak",
        type = ApiSkjonnsfastsettelse.ApiSkjonnsfastsettelseArbeidsgiver.ApiSkjonnsfastsettelseType.OMREGNET_ARSINNTEKT,
        begrunnelseMal = "begrunnelseMal",
        begrunnelseKonklusjon = "begrunnelseKonklusjon",
        begrunnelseFritekst = "begrunnelseFritekst",
        lovhjemmel = ApiLovhjemmel(
            paragraf = "paragraf",
            ledd = "ledd",
            bokstav = "bokstav",
            lovverk = "folketrygdloven",
            lovverksversjon = "",
        ),
        initierendeVedtaksperiodeId = testperson.vedtaksperiodeId1.toString(),
    )

}

data class GodkjenningsbehovTestdata(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val periodeFom: LocalDate = 1.januar,
    val periodeTom: LocalDate = 31.januar,
    val skjæringstidspunkt: LocalDate = periodeFom,
    val periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
    val kanAvvises: Boolean = true,
    val førstegangsbehandling: Boolean = true,
    val inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
    val orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
    val utbetalingtype: no.nav.helse.modell.utbetaling.Utbetalingtype = UTBETALING,
    val vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    val spleisBehandlingId: UUID = UUID.randomUUID(),
    val tags: List<String> = emptyList(),
    val foreløpigBeregnetSluttPåSykepenger: LocalDate = 1.desember()
)

data class AvviksvurderingTestdata(
    val avviksprosent: Double = 10.0,
    val sammenligningsgrunnlag: Double = 650_000.0,
    val skjæringstidspunkt: LocalDate = 1.januar,
    val avviksvurderingId: UUID = UUID.randomUUID(),
    val vedtaksperiodeId: UUID = UUID.randomUUID(),
)
