package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spesialist.api.graphql.enums.GraphQLVilkarsgrunnlagtype
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLSykepengegrunnlagsgrense
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLVilkarsgrunnlag

enum class Vilkarsgrunnlagtype { INFOTRYGD, SPLEIS, UKJENT }

interface Vilkarsgrunnlag {
    val id: UUIDString
    val vilkarsgrunnlagtype: Vilkarsgrunnlagtype
    val inntekter: List<Arbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>
    val omregnetArsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val skjaeringstidspunkt: DateString
    val sykepengegrunnlag: Double
}

data class VilkarsgrunnlagInfotrygd(
    override val id: UUIDString,
    override val vilkarsgrunnlagtype: Vilkarsgrunnlagtype,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val skjaeringstidspunkt: DateString,
    override val sykepengegrunnlag: Double,
) : Vilkarsgrunnlag

data class VilkarsgrunnlagSpleis(
    override val id: UUIDString,
    override val vilkarsgrunnlagtype: Vilkarsgrunnlagtype,
    override val inntekter: List<Arbeidsgiverinntekt>,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val skjaeringstidspunkt: DateString,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<Arbeidsgiverrefusjon>,
    val skjonnsmessigFastsattAarlig: Double?,
    val avviksprosent: Double?,
    val antallOpptjeningsdagerErMinst: Int,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: Sykepengegrunnlagsgrense,
    val oppfyllerKravOmMedlemskap: Boolean?,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: DateString,
) : Vilkarsgrunnlag

private fun GraphQLVilkarsgrunnlagtype.tilVilkarsgrunnlagtype(): Vilkarsgrunnlagtype =
    when (this) {
        GraphQLVilkarsgrunnlagtype.INFOTRYGD -> Vilkarsgrunnlagtype.INFOTRYGD
        GraphQLVilkarsgrunnlagtype.SPLEIS -> Vilkarsgrunnlagtype.SPLEIS
        GraphQLVilkarsgrunnlagtype.UKJENT -> Vilkarsgrunnlagtype.UKJENT
        else -> throw Exception("Ukjent vilkårsgrunnlagtype ${this.name}")
    }

internal fun GraphQLVilkarsgrunnlag.tilVilkarsgrunnlag(): Vilkarsgrunnlag =
    when (this) {
        is GraphQLSpleisVilkarsgrunnlag -> VilkarsgrunnlagSpleis(
            id = id,
            vilkarsgrunnlagtype = vilkarsgrunnlagtype.tilVilkarsgrunnlagtype(),
            inntekter = inntekter.map { it.tilArbeidsgiverinntekt() },
            arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilArbeidsgiverrefusjon() },
            omregnetArsinntekt = omregnetArsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
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
            avviksprosent = avviksprosent
        )

        is GraphQLInfotrygdVilkarsgrunnlag -> VilkarsgrunnlagInfotrygd(
            id = id,
            vilkarsgrunnlagtype = vilkarsgrunnlagtype.tilVilkarsgrunnlagtype(),
            inntekter = inntekter.map { it.tilArbeidsgiverinntekt() },
            arbeidsgiverrefusjoner = arbeidsgiverrefusjoner.map { it.tilArbeidsgiverrefusjon() },
            omregnetArsinntekt = omregnetArsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            skjaeringstidspunkt = skjaeringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag
        )

        else -> throw Exception("Ukjent vilkårsgrunnlag ${this.javaClass.name}")
    }

internal fun GraphQLSykepengegrunnlagsgrense.tilSykepengegrunnlaggrense() =
    Sykepengegrunnlagsgrense(grunnbelop, grense, virkningstidspunkt)

data class Sykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: DateString,
)