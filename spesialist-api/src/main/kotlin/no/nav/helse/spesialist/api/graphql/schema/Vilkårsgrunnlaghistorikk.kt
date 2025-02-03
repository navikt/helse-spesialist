package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.avviksvurdering.Avviksvurdering
import no.nav.helse.spesialist.api.graphql.mapping.tilApiArbeidsgiverrefusjon
import no.nav.helse.spesialist.api.graphql.mapping.tilApiOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLVilkarsgrunnlag
import java.time.LocalDate
import java.util.UUID

@GraphQLName("Vilkarsgrunnlagtype")
enum class ApiVilkårsgrunnlagtype { INFOTRYGD, SPLEIS, UKJENT }

@GraphQLName("Vilkarsgrunnlag")
interface ApiVilkårsgrunnlag {
    val id: UUID
    val vilkarsgrunnlagtype: ApiVilkårsgrunnlagtype
    val inntekter: List<ApiArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>
    val omregnetArsinntekt: Double
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: Double
}

@GraphQLName("VilkarsgrunnlagInfotrygd")
data class ApiVilkårsgrunnlagInfotrygd(
    override val id: UUID,
    override val vilkarsgrunnlagtype: ApiVilkårsgrunnlagtype,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
) : ApiVilkårsgrunnlag

@GraphQLName("VilkarsgrunnlagSpleis")
data class ApiVilkårsgrunnlagSpleis(
    override val id: UUID,
    override val vilkarsgrunnlagtype: ApiVilkårsgrunnlagtype,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val omregnetArsinntekt: Double,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    val sammenligningsgrunnlag: Double?,
    val skjonnsmessigFastsattAarlig: Double?,
    val avviksprosent: Double?,
    val antallOpptjeningsdagerErMinst: Int,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: ApiSykepengegrunnlagsgrense,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: LocalDate,
) : ApiVilkårsgrunnlag

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

@GraphQLName("Sykepengegrunnlagsgrense")
data class ApiSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
)
